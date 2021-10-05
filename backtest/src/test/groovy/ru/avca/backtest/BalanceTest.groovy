package ru.avca.backtest

import spock.lang.Specification

import static ru.avca.backtest.OperationSide.*

/**
 *
 * @author a.chermashentsev
 * Date: 25.09.2021
 * */
class BalanceTest extends Specification {

    def "balance"() {
        when:
        def initialBalance = new Balance(bSymbol, bQuantity, bOpenPriceToUsdt)
        def incomeOperation = new BacktestStrategyOperation(oSymbol, oSide, oQuantity, oPrice)

        then:
        initialBalance.merge(incomeOperation) == new Balance(bSymbol, expectedQuantity, expectedPrice)

        where:
        bSymbol <<          ["b", "a", "c", "d",    "e"]
        bQuantity <<        [100, 100, 100, 100,    50]
        bOpenPriceToUsdt << [100, 50,  100, 50,     50]
        oSymbol <<          ["b", "b", "c", "d",    "e"]
        oSide <<            [BUY, BUY, BUY, BUY,    SELL]
        oQuantity <<        [100, 200, 100, 60,     20]
        oPrice <<           [100, 200, 50,  75,     100]
        expectedQuantity << [200, 100, 200, 160,    30]
        expectedPrice <<    [100, 50 , 75,  59.375, 50]
    }
}
