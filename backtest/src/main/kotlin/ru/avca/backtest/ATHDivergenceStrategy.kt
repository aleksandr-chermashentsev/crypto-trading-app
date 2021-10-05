package ru.avca.backtest

import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import org.apache.commons.collections4.queue.CircularFifoQueue
import java.util.function.Predicate
import kotlin.math.max

/**
 *
 * @author a.chermashentsev
 * Date: 23.09.2021
 **/
class ATHDivergenceStrategy(
    val periodLength: Int,
    val symbolsPredicate: Predicate<String>,
    val profitPricePercent: Double,
    val lossPricePercent: Double,
    val athDeviationPercent: Double,
    val increasePositionPercent: Double,
    val maxNumberOfOpenPositions: Int,
    val partOfUsdBalanceUse: Double,
    val cooldownPeriods: Int
) : BacktestStrategy<Int, ATHDivergenceStrategy.AthValues> {

    override fun sharedData(): BacktestStrategy.SharedData<Int, AthValues> {
        return object : BacktestStrategy.SharedData<Int, AthValues> {
            override fun key(): Int {
                return periodLength
            }

            override fun collectData(candles: Map<String, Candlestick>, athValues: AthValues?): AthValues {
                val lastNHighBySymbols = athValues?.lastNHighBySymbols?.toMutableMap()?: HashMap()
                val athBySymbols = athValues?.athBySymbols?.toMutableMap() ?: HashMap()
                //update ath values
                candles
                    .forEach {
                        val lastNHigh = lastNHighBySymbols.getOrPut(it.key) { CircularFifoQueue(periodLength) }
                        val high = it.value.high.toDouble()

                        //if after add this high, our lastNHigh queue will be full, we have to find max in prev high values
                        if (lastNHigh.size == periodLength - 1) {
                            val prevHigh = lastNHigh.maxOrNull()!!
                            athBySymbols.put(it.key, max(prevHigh, high))
                        }

                        if (lastNHigh.size == lastNHigh.maxSize()) {
                            val nextForRemove = lastNHigh.first()
                            lastNHigh.add(high)
                            var prevHigh = athBySymbols.getOrDefault(it.key, 0.0)
                            if (prevHigh == nextForRemove) {
                                prevHigh = lastNHigh.maxOrNull()!!
                            }
                            athBySymbols.put(it.key, max(prevHigh, high))
                        } else {
                            lastNHigh.add(high)
                        }
                    }
                return AthValues(athBySymbols, lastNHighBySymbols)
            }
        }
    }

    override fun intervals(): List<CandlestickInterval> {
        return listOf(CandlestickInterval.DAILY)
    }

    val usdSymbol = "USDT"
    val sellStopLossSymbols: MutableMap<String, Int> = HashMap()
    var period = 0

    override fun onTimeTick(candles: Map<String, Candlestick>, context: Context, athValues: AthValues): List<BacktestStrategyOperation> {
        period++
        val candles = candles.filter { symbolsPredicate.test(it.key) }
        val operations: MutableList<BacktestStrategyOperation> = ArrayList()
        //check profit close open positions
        val notUsdtBalances = context.balances.values
            .filter { it.symbol != usdSymbol }
        val usdBalance = context.balances[usdSymbol]!!
        if (usdBalance.quantity < 20 && notUsdtBalances.isEmpty()) {
            return emptyList()
        }
        val athBySymbols = athValues.athBySymbols

        notUsdtBalances
            .filter { balance -> operations.none { it.symbol == balance.symbol } }
            .filter { candles.containsKey(it.symbol) }
            .filter { it.openPriceToUsdt * profitPricePercent <= candles.get(it.symbol)!!.high.toDouble() }
            .forEach {
                operations.add(
                    BacktestStrategyOperation(
                        it.symbol,
                        OperationSide.SELL,
                        it.quantity,
                        it.openPriceToUsdt * profitPricePercent
                    )
                )
            }

        //check loss close open positions
        notUsdtBalances
            .filter { balance -> operations.none { it.symbol == balance.symbol } }
            .filter { candles.containsKey(it.symbol) }
            .filter {it.tradesNumber > 1}
            .filter { balance -> operations.none { it.symbol == balance.symbol } }
            .filter { it.openPriceToUsdt * lossPricePercent >= candles.get(it.symbol)!!.low.toDouble() }
            .forEach {
                sellStopLossSymbols.put(it.symbol, period)
                operations.add(
                    BacktestStrategyOperation(
                        it.symbol,
                        OperationSide.SELL,
                        it.quantity,
                        it.openPriceToUsdt * lossPricePercent
                    )
                )
            }


        //check increase already opened positions in case of bigger deviation from ATH
        notUsdtBalances
            .filter { operations.size + notUsdtBalances.size < maxNumberOfOpenPositions }
            .filter { balance -> operations.none { it.symbol == balance.symbol } }
            .filter { candles.containsKey(it.symbol) }
            .filter { it.openPriceToUsdt * increasePositionPercent >= candles.get(it.symbol)!!.low.toDouble() }
            .filter {it.tradesNumber == 1}
            .forEach {
                val price = it.openPriceToUsdt * increasePositionPercent
                val halfOfOpenQuantity = it.openPriceToUsdt * it.quantity / 2
                if (halfOfOpenQuantity < usdBalance.quantity && operations.size + notUsdtBalances.size < maxNumberOfOpenPositions) {
                    operations.add(
                        BacktestStrategyOperation(
                            it.symbol,
                            OperationSide.BUY,
                            halfOfOpenQuantity / price,
                            price
                        )
                    )
                }
            }
        //check open new positions in case of big deviation from ATH
        if (notUsdtBalances.size < maxNumberOfOpenPositions) {
            candles
                .filter { athBySymbols.containsKey(it.key) }
                .filter { candle -> operations.none { it.symbol == candle.key } }
                .filter { operations.size + notUsdtBalances.size < maxNumberOfOpenPositions }
                .filter { athBySymbols.get(it.key)!! * athDeviationPercent >= it.value.low.toDouble() }
                .filter { !context.balances.containsKey(it.key)}
                .forEach {
                    val price = Math.min(
                        athBySymbols.get(it.key)!! * athDeviationPercent,
                        it.value.open.toDouble()
                    )
                    val usdQuantity = usdBalance.quantity * partOfUsdBalanceUse
                    if (period - sellStopLossSymbols.getOrDefault(
                            it.key,
                            0
                        ) > cooldownPeriods && usdQuantity > 20 && operations.size + notUsdtBalances.size < maxNumberOfOpenPositions
                    ) {
                        operations.add(BacktestStrategyOperation(it.key, OperationSide.BUY, usdQuantity / price, price))
                    }
                }
        }

        return operations
    }

    override fun toString(): String {
        return "ATHDivergenceStrategy(periodLength=$periodLength, symbolsPredicate=$symbolsPredicate, profitPricePercent=$profitPricePercent, lossPricePercent=$lossPricePercent, athDeviationPercent=$athDeviationPercent, increasePositionPercent=$increasePositionPercent, maxNumberOfOpenPositions=$maxNumberOfOpenPositions, partOfUsdBalanceUse=$partOfUsdBalanceUse)"
    }

    data class AthValues(
        val athBySymbols: Map<String, Double> = mapOf(),
        val lastNHighBySymbols: Map<String, CircularFifoQueue<Double>> = mapOf()
    )

}