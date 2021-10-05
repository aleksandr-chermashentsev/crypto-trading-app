package ru.avca.backtest

import com.binance.api.client.domain.market.Candlestick

/**
 * @author a.chermashentsev
 * Date: 25.09.2021
 */
class TickProcessor(
    val strategies: List<BacktestStrategy<out Any, in Any>>,
    val usdtQuantity: Double,
    val shouldPrint: Boolean = true
) {
    val contextes: MutableMap<String, Context>
    val usdSymbol = "USDT"
    val sharedData = HashMap<Any, Any>()
    val stats = HashMap<String, Stat>()

    init {
        this.contextes = strategies
            .map { it.toString() to Context(mapOf(usdSymbol to Balance(usdSymbol, usdtQuantity, 1.0))) }
            .toMap()
            .toMutableMap()
    }

    fun onTimeTick(candles: Map<String, Candlestick>) {
        this.strategies.map { it.sharedData().key() to it.sharedData() }
            .toMap()
            .map { it.key to it.value.collectData(candles, sharedData[it.key]) }
            .forEach { sharedData.put(it.first, it.second!!) }

        this.strategies.forEach {
            val localSharedData = sharedData[it.sharedData().key()]!!

            val context = contextes[it.toString()]!!
            val updatedBalances = context.balances.toMutableMap()
            val stat = stats.getOrPut(it.toString()) { Stat(0, 0, 0, 0) }
            it.onTimeTick(candles, context, localSharedData)
                .forEach { operation ->
                    val balance = context.balances[operation.symbol]
                    if (balance == null) {
                        check(operation.side == OperationSide.BUY) { "First operation for symbol ${operation.symbol} could be only BUY" }
                        updatedBalances[operation.symbol] =
                            Balance(operation.symbol, operation.quantity, operation.price)
                        updatedBalances[usdSymbol] =
                            updatedBalances[usdSymbol]!!.minus(operation.quantity * operation.price)
                    } else {
                        updatedBalances[operation.symbol] = balance.merge(operation)
                        if (operation.side == OperationSide.BUY) {
                            updatedBalances[usdSymbol] =
                                updatedBalances[usdSymbol]!!.minus(operation.quantity * operation.price)
                        }
                        if (operation.side == OperationSide.SELL) {
                            updatedBalances[usdSymbol] =
                                updatedBalances[usdSymbol]!!.plus(operation.quantity * operation.price)
                        }
                    }

                    when (operation.side) {
                        OperationSide.BUY ->
                            if (updatedBalances[operation.symbol]!!.tradesNumber == 1) {
                                stat.firstBuyCount++
                            } else {
                                stat.rebuyCount++
                            }
                        OperationSide.SELL ->
                            if (operation.price > context.balances[operation.symbol]!!.openPriceToUsdt) {
                                stat.profitSalesCount++
                            } else {
                                stat.lossSalesCount++
                            }
                    }

                    if (shouldPrint) {
                        val openTime = candles.values.find { true }!!.openTime
                        if (operation.side == OperationSide.SELL && balance != null) {
                            println("${operation.side} ${(operation.price / balance.openPriceToUsdt - 1) * 100}% ${operation.symbol} at time $openTime ")
                        } else {
                            println("${operation.side} ${operation.symbol} at time $openTime")
                        }
                    }
                }
            contextes[it.toString()] = context.copy(
                balances = updatedBalances.filter { it.key == usdSymbol || it.value.quantity > 0.0000001 }
            )
        }
    }

    data class Stat(
        var firstBuyCount: Int,
        var rebuyCount: Int,
        var profitSalesCount: Int,
        var lossSalesCount: Int
    )
}