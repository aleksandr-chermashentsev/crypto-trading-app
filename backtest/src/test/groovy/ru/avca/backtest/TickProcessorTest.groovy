package ru.avca.backtest

import spock.lang.Specification

/**
 *
 * @author a.chermashentsev
 * Date: 25.09.2021
 * */
class TickProcessorTest extends Specification {

    def "should add btc balance on buy new and reduce usdt balance"() {
        given:
        def strategy = Mock(BacktestStrategy)
        def subj = new TickProcessor([strategy], 1000)
        strategy.onTimeTick(_, _) >> [new BacktestStrategyOperation("btc", OperationSide.BUY, 1, 100)]
        when:
        subj.onTimeTick([:])
        then:
        subj.contextes[strategy.toString()].balances == [
                "USDT": new Balance("USDT", 900, 1.0),
                "btc" : new Balance("btc", 1, 100)
        ]

    }

    def "should remove btc balance when it sold"() {
        given:
        def strategy = Mock(BacktestStrategy)
        def subj = new TickProcessor([strategy], 1000)
        strategy.onTimeTick(_, _) >>> [
                [new BacktestStrategyOperation("btc", OperationSide.BUY, 10, 100)],
                [new BacktestStrategyOperation("btc", OperationSide.SELL, 10, 100)]
        ]
        when:
        subj.onTimeTick([:])
        subj.onTimeTick([:])
        then:
        subj.contextes[strategy.toString()].balances == [
                "USDT": new Balance("USDT", 1000, 1.0)
        ]
    }

}
