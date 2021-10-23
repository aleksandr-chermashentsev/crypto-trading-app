package ru.avca.robot.utils

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.general.SymbolStatus
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import com.binance.api.client.domain.market.CandlestickInterval.*
import java.util.*
import java.util.concurrent.TimeUnit.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import javax.inject.Inject

/**
 *
 * @author a.chermashentsev
 * Date: 06.10.2021
 **/
class RobotUtils(
    @Inject private val binanceApiClientFactory: BinanceApiClientFactory
) {
    fun loadSymbols(usdCoin: String): Stream<String> {
        return binanceApiClientFactory.newRestClient().exchangeInfo.symbols
            .stream()
            .filter { symbolInfo: SymbolInfo -> usdCoin == symbolInfo.quoteAsset }
            .filter { symbolInfo: SymbolInfo -> symbolInfo.status == SymbolStatus.TRADING }
            .map<String> { obj: SymbolInfo -> obj.symbol }
    }

    fun loadHistory(symbol:String, interval: CandlestickInterval, fromTimestamp:Long, toTimestamp: Long = Date().time): Stream<Candlestick> {

            val result: TreeSet<Candlestick> =
                TreeSet<Candlestick>(Comparator.comparingLong { it.openTime })
            var currentFromTimestamp = fromTimestamp;
            while (toTimestamp - currentFromTimestamp >= interval.timeInMillis()) {
                result.addAll(loadHistoryImpl(symbol, interval, currentFromTimestamp, toTimestamp))
                if (result.isEmpty() || result.last().closeTime == currentFromTimestamp) {
                    return Stream.empty()
                }
                currentFromTimestamp = result.last().closeTime
            }
            return result.stream()
                .filter({ obj: Candlestick? -> Objects.nonNull(obj) })
        }
        private fun loadHistoryImpl(
            symbol: String,
            interval: CandlestickInterval,
            fromTimestamp: Long,
            toTimestamp: Long
        ): TreeSet<Candlestick> {
            if (toTimestamp - fromTimestamp < interval.timeInMillis()) {
                return TreeSet()
            }
            val isExecuted = AtomicBoolean(false)
            val result: TreeSet<Candlestick> =
                TreeSet<Candlestick>(Comparator.comparingLong { it.openTime })
            binanceApiClientFactory.newAsyncRestClient().getCandlestickBars(symbol,
                interval,
                1000,
                fromTimestamp,
                toTimestamp
            ) { candles: List<Candlestick?> ->
                result.addAll(
                    candles.map { it!! }
                )
                isExecuted.set(true)
            }
            while (!isExecuted.get()) {
            }
            return result
        }

        fun CandlestickInterval.timeInMillis(): Long {

            return when (this) {
                 ONE_MINUTE -> MINUTES.toMillis(1);
                 THREE_MINUTES -> MINUTES.toMillis(3);
                 FIVE_MINUTES -> MINUTES.toMillis(5);
                 FIFTEEN_MINUTES -> MINUTES.toMillis(15);
                 HALF_HOURLY -> MINUTES.toMillis(30);
                 HOURLY -> HOURS.toMillis(1);
                 TWO_HOURLY -> HOURS.toMillis(2);

                 FOUR_HOURLY -> HOURS.toMillis(4);
                 SIX_HOURLY -> HOURS.toMillis(6);
                 EIGHT_HOURLY -> HOURS.toMillis(8);
                 TWELVE_HOURLY -> HOURS.toMillis(12);
                 DAILY -> DAYS.toMillis(1);
                 THREE_DAILY -> DAYS.toMillis(3);
                 WEEKLY -> DAYS.toMillis(7);
                 MONTHLY -> DAYS.toMillis(30);
                else -> throw IllegalArgumentException("Please support $this interval")
            }
        }
}