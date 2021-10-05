package ru.avca.backtest

/**
 *
 * @author a.chermashentsev
 * Date: 23.09.2021
 **/
data class BacktestStrategyOperation(
    val symbol: String,
    val side: OperationSide,
    val quantity: Double,
    val price: Double
)

data class Balance(
    val symbol: String,
    val quantity: Double,
    val openPriceToUsdt: Double,
    val tradesNumber: Int = 1
) {
    fun merge(operation: BacktestStrategyOperation): Balance {
        if (operation.symbol != symbol) {
            return this
        }
        if (operation.side == OperationSide.BUY) {
            return copy(
                quantity = quantity + operation.quantity,
                openPriceToUsdt =
                            (quantity * openPriceToUsdt + operation.quantity * operation.price) /
                                (quantity + operation.quantity),
                tradesNumber = tradesNumber + 1
            )
        }
        else if (operation.side == OperationSide.SELL) {
            return copy(
                quantity = quantity - operation.quantity,
                tradesNumber = tradesNumber + 1
            )
        }

        throw IllegalArgumentException("Unknown operation side ${operation.side}")
    }

    fun minus(quantity: Double): Balance {
        return copy(quantity = this.quantity - quantity)
    }

    fun plus(quantity: Double): Balance {
        return copy(quantity = this.quantity + quantity)
    }
}

data class Context(
    val balances: Map<String, Balance>,
)

enum class OperationSide {
    BUY,
    SELL
}
