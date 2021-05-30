package ru.avca.backtest

import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.general.SymbolStatus
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import ru.avca.backtest.history.HistoryLoader
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.streams.toList

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
@Singleton
open class BacktestTopCoinsStrategy(
    @Inject val restClient: BinanceApiRestClient,
    @Inject val historyLoader: HistoryLoader
) {

    @EventListener
    fun onStartupEvent(startupEvent: StartupEvent) {
        val symbols = restClient.exchangeInfo.symbols.stream()
            .filter { it.quoteAsset == "USDT" }
            .filter { it.status == SymbolStatus.TRADING }
            .map { it.symbol }
//            .filter {(it.endsWith("UPUSDT", ignoreCase = true) || it.endsWith("DOWNUSDT", ignoreCase = true))}
            .toList()
        val executor = Executors.newFixedThreadPool(3)
        val timestampFrom = Utils.timestampOfTheDate("2019-01-01")
        val results: MutableMap<ResultKey, BigDecimal> = ConcurrentHashMap()

        val btcUsdtDailyMap =
            historyLoader.loadHistory("BTCUSDT", CandlestickInterval.DAILY, Utils.timestampOfTheDate("2017-01-01"))
                .map {
                   it.openTime to it
                }
                .toList()
                .toMap()
                .toSortedMap() as TreeMap<Long, Candlestick>
        for (interval in arrayOf(
//            CandlestickInterval.MONTHLY,
//            CandlestickInterval.WEEKLY,
//            CandlestickInterval.THREE_DAILY,
//            CandlestickInterval.DAILY,
            CandlestickInterval.TWELVE_HOURLY,
//            CandlestickInterval.EIGHT_HOURLY,
//            CandlestickInterval.SIX_HOURLY,
//            CandlestickInterval.FOUR_HOURLY,
//            CandlestickInterval.TWO_HOURLY,
//            CandlestickInterval.HOURLY
        )) {
            val candlesBySymbol =
                symbols.associateWith { historyLoader.loadHistory(it, interval, timestampFrom).toList() }
            for (coinsCount in arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)) {
                for (stopLoss in 0..9)
                {
                    val tpValues = 105..200 step 10
                    val toMutableList = tpValues.toMutableList()
                    toMutableList.add(200000)

                    for (tp in toMutableList) {
                        val stopLossBigDecimal = BigDecimal(stopLoss.toDouble() * 0.1)
                        val takeProfit = BigDecimal(tp * 0.01)
                        results[ResultKey(interval, coinsCount, stopLossBigDecimal, takeProfit)] = test(
                            candlesBySymbol,
                            stopLoss = stopLossBigDecimal,
                            takeProfit = takeProfit,
                            coinsCount,
                            btcUsdtDaylyMap = btcUsdtDailyMap
                        )
                        println("$interval coins=$coinsCount tested, stopLoss $stopLossBigDecimal, takeProfit $takeProfit")
                    }
                }
            }
        }

        println("resultsSize ${results.size}")
        results.entries.sortedByDescending { it.value.toDouble() }
//            .filter { it.value.toDouble() > 500 }
            .take(100)
            .forEach { println("Result ${it.key} = ${it.value}") }

        System.exit(0)
    }

    fun test(
        candles: Map<String, List<Candlestick>>,
        stopLoss: BigDecimal?,
        takeProfit: BigDecimal?,
        coinsCount: Int,
        timestampFrom: Long = 0,
        timestampTo: Long = Long.MAX_VALUE,
        btcUsdtDaylyMap: TreeMap<Long, Candlestick>
    ): BigDecimal {
        var usdtBalance = BigDecimal(100)
        val currentBest: MutableList<Pair<String, Candlestick>> = ArrayList();
        val balances: MutableList<Pair<String, BigDecimal>> = ArrayList();
        val candlesByOpenTimeBySymbol = candles.entries
            .map {
                it.key to it.value.map({ it.openTime to it }).toMap().toSortedMap()
            }
            .toMap()

        val keyOfBiggestMap = candlesByOpenTimeBySymbol.entries.maxByOrNull { it.value.size }!!.key
        val biggestMap = candlesByOpenTimeBySymbol[keyOfBiggestMap]!!

        val openTimes = biggestMap.keys
            .filter { it in (timestampFrom + 1) until timestampTo }

        for (openTime in openTimes) {
            val candlesWithSymbolsForCurrentTime = candlesByOpenTimeBySymbol.entries
                .filter { it.value[openTime] != null }
                .map { it.key to it.value[openTime]!! }
                .filter {
                    balances.any { balance -> it.first == balance.first } || BigDecimal(it.second.quoteAssetVolume).multiply(
                        BigDecimal("0.01")
                    )
                        .toDouble() > usdtBalance.toDouble()
                }
                .toMap()

            if (balances.isNotEmpty()) {
                usdtBalance = BigDecimal(balances
                    .map { symbolToBalance ->
                        var candlestickForBalance = candlesWithSymbolsForCurrentTime[symbolToBalance.first]
                        if (candlestickForBalance == null) {
                            println("Couldn't find candle for $symbolToBalance $balances")
                            candlestickForBalance = currentBest.find { it.first == symbolToBalance.first }!!.second
                        }
                        val prevClosePrice =
                            BigDecimal(currentBest.find { it.first == symbolToBalance.first }!!.second.close)
                        if (stopLoss != null) {
                            val stopLossPrice = prevClosePrice.multiply(stopLoss)
                            if (BigDecimal(candlestickForBalance.low).toDouble() < stopLoss.toDouble()) {
                                return@map stopLossPrice.multiply(symbolToBalance.second).multiply(BigDecimal(0.999))
                            }
                        }
                        if (takeProfit != null) {
                            val profitPrice = prevClosePrice.multiply(takeProfit)
                            if (BigDecimal(candlestickForBalance.high).toDouble() > profitPrice.toDouble()) {
                                return@map profitPrice.multiply(symbolToBalance.second).multiply(BigDecimal(0.999))
                            }
                        }
                        return@map BigDecimal(candlestickForBalance.close).multiply(symbolToBalance.second)
                            .multiply(BigDecimal(0.999))
                    }
                    .sumOf { it.toDouble() }
                )
//                println("$usdtBalance ${candlesWithSymbolsForCurrentTime.size} $balances")
            }

            if (btcUsdtDaylyMap.isNotEmpty()) {
                val weekInMilliseconds = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)
                val startWeekCandle = btcUsdtDaylyMap.floorEntry(openTime - weekInMilliseconds).value
                val endWeekCandle = btcUsdtDaylyMap.floorEntry(openTime).value
                if (BigDecimal(startWeekCandle.open).toDouble() < BigDecimal(endWeekCandle.close).toDouble()) {
                    if (buyCoins(
                            currentBest,
                            candlesWithSymbolsForCurrentTime,
                            coinsCount,
                            balances,
                            usdtBalance
                        )
                    ) return usdtBalance
                }
            }
        }
        return usdtBalance
    }

    private fun buyCoins(
        currentBest: MutableList<Pair<String, Candlestick>>,
        candlesWithSymbolsForCurrentTime: Map<String, Candlestick>,
        coinsCount: Int,
        balances: MutableList<Pair<String, BigDecimal>>,
        usdtBalance: BigDecimal
    ): Boolean {
        currentBest.clear()
        currentBest.addAll(candlesWithSymbolsForCurrentTime
            .entries
            .map { it.key to it.value }
            .sortedWith(compareByDescending {
                BigDecimal(it.second.close).divide(
                    BigDecimal(it.second.open),
                    10,
                    RoundingMode.CEILING
                )
            })
            .filter { BigDecimal(it.second.close).toDouble() > BigDecimal(it.second.open).toDouble() }
            .take(coinsCount)
        )
        if (currentBest.size < coinsCount) {
            balances.clear()
        } else {
            val oneBuyAvailableSize = usdtBalance.divide(BigDecimal(coinsCount), 10, RoundingMode.CEILING)
            balances.clear()
            balances.addAll(currentBest.map {
                it.first to oneBuyAvailableSize.divide(BigDecimal(it.second.close), 10, RoundingMode.CEILING)
                    .multiply(
                        BigDecimal(0.999)
                    )
            }.toList())
        }

        if (usdtBalance.toDouble() < 20) {
            return true
        }
        return false
    }
}

internal data class ResultKey(
    val candlestickInterval: CandlestickInterval,
    val coinsCount: Int,
    val stopLoss: BigDecimal?,
    val takeProfit: BigDecimal?
)
internal data class ResultValue(
    val money: BigDecimal,
    val takeProfitsCount: Int,
    val stopLossCount: Int,
    val minusesCount: Int,
    val plusesCount: Int
)