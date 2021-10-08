package ru.avca.backtest

import com.binance.api.client.domain.market.Candlestick
import spock.lang.Specification

/**
 *
 * @author a.chermashentsev
 * Date: 24.09.2021
 * */
class ATHDivergenceStrategyTest extends Specification {

    def "should not buy if empty ath values"() {
        when:
        def strategy = createNewStrategy(['periodLength': 1, 'athDeviationPercent': 0.99D])
        def ticks = [
                ["BTCUSDT": createCandlestick(high: 120D)] as HashMap,
                ["BTCUSDT": createCandlestick(low: 10D)] as HashMap,
        ]
        then:
        ticks.stream().allMatch {
            strategy.onTimeTick(it, createContext(), new ATHDivergenceStrategy.AthValues()) == []
        }
    }

    def "should not buy if not enough money"() {
        when:
        def strategy = createNewStrategy(['periodLength': 1, 'athDeviationPercent': 0.99D])
        def ticks = [
                ["BTCUSDT": createCandlestick(high: 120D)] as HashMap,
                ["BTCUSDT": createCandlestick(low: 10D)] as HashMap,
        ]
        then:
        ticks.stream().allMatch {
            strategy.onTimeTick(it, createContext(10D, []), createAthValues(["BTCUSDT": 1000D])) == []
        }
    }

    def createAthValues(currentAthValues) {
        return new ATHDivergenceStrategy.AthValues(currentAthValues, [:] as Map)
    }

    def "should buy by athDeviationPercent * ath price if has divergence not in the first period"() {
        when:
        def strategy = createNewStrategy(['periodLength': 2, 'athDeviationPercent': 0.8D, 'partOfUsdBalanceUse': 0.5])
        def ticks = [
                ["BTCUSDT": createCandlestick(high: 100D)] as HashMap,
                ["BTCUSDT": createCandlestick(high: 100D, low: 90D)] as HashMap,
                ["BTCUSDT": createCandlestick(high: 90D, low: 10D)] as HashMap,
        ]
        then:
        ticks.collect {strategy.onTimeTick(it, createContext(), createAthValues(["BTCUSDT": 100]))}
                == [
                [],[],
                [new BacktestStrategyOperation("BTCUSDT", OperationSide.BUY, 500D/80D, 80D)]
        ]
    }

    def "should sell if go throw profit price"() {
        when:
        def strategy = createNewStrategy(['periodLength': 2, 'athDeviationPercent': 0.8D, 'partOfUsdBalanceUse': 0.5, 'profitPricePercent': 1.1])
        def ticks = [
                ["BTCUSDT": createCandlestick(high: 111, low: 100)] as HashMap,
        ]
        then:
        ticks.collect {strategy.onTimeTick(it, createContext(
                1000, ["BTCUSDT": new Balance('BTCUSDT', 50, 100, 1)]
        ), createAthValues(["BTCUSDT": 111]))} == [
                [new BacktestStrategyOperation("BTCUSDT", OperationSide.SELL, 50D, 100D*1.1D)]
        ]
    }

    def "should sell if go throw loss price and have 2 trades"() {
        when:
        def strategy = createNewStrategy(['periodLength': 2, 'athDeviationPercent': 0.8D, 'partOfUsdBalanceUse': 0.5, 'increasePositionPercent': 0.9, 'lossPricePercent': 0.85])
        def ticks = [
                ["BTCUSDT": createCandlestick(high: 100, low: 80)] as HashMap,
        ]

        then:
        ticks.collect {strategy.onTimeTick(it, createContext(
                1000, ["BTCUSDT": new Balance('BTCUSDT', 5, 100, 2)]
        ), createAthValues("BTCUSDT": 100))} == [
                [new BacktestStrategyOperation("BTCUSDT", OperationSide.SELL, 5, 85)]
        ]
    }


    def "should buy if go throw increasePositionPrice"() {
        when:
        def strategy = createNewStrategy(['periodLength': 2, 'athDeviationPercent': 0.8D, 'partOfUsdBalanceUse': 0.5, 'increasePositionPercent': 0.9, 'lossPricePercent': 0.80])
        def ticks = [
                ["BTCUSDT": createCandlestick(high: 100, low: 70)] as HashMap,
        ]

        then:
        ticks.collect {strategy.onTimeTick(it, createContext(
                1000, ["BTCUSDT": new Balance('BTCUSDT', 5, 100, 1)]
        ), createAthValues("BTCUSDT": 100))} == [
                [new BacktestStrategyOperation("BTCUSDT", OperationSide.BUY, 500D/2/90, 90D)]
        ]
    }

    def "should have ATH values "() {
        when:
        def strategy = createNewStrategy(["periodLength": 2])
        then:
        def athValues = strategy.sharedData().collectData(["BTCUSDT":createCandlestick(high: 100, low: 70)], new ATHDivergenceStrategy.AthValues())
        athValues.athBySymbols == [:]
        def newAthValues = strategy.sharedData().collectData(["BTCUSDT":createCandlestick(high: 120, low: 70)], athValues)
        newAthValues.athBySymbols == ["BTCUSDT": 120] as Map
        def updatedAthValues = strategy.sharedData().collectData(["BTCUSDT": createCandlestick(high: 90, low: 70)], newAthValues)
        updatedAthValues.athBySymbols == ["BTCUSDT": 120] as Map
        def athValues5 = strategy.sharedData().collectData(["BTCUSDT": createCandlestick(high: 90, low: 70)], updatedAthValues)
        athValues5.athBySymbols == ["BTCUSDT": 90] as Map
    }

    static def createContext(usdBalance = 1000D, balances = [:] as Map<String, Balance>) {
        return new Context(["USDT": new Balance("USDT", usdBalance, 1.0D, 1)] + balances as Map)
    }

    static def createCandlestick(Map args) {
        def candle = new Candlestick()
        candle.openTime = args.get('openTime', 100L)
        candle.open = args.get('open', 100D)
        candle.high = args.get('high', 120D)
        candle.low = args.get('low', 80D)
        candle.close = args.get('close', 110D)
        candle.volume = args.get('volume', 1_000_000D)
        candle.closeTime = args.get('closeTime', 200L)
        candle.quoteAssetVolume = args.get('quoteAssetVolume', 1_000_000D)
        candle.numberOfTrades = args.get('numberOfTrades', 1_000_000L)
        candle.takerBuyBaseAssetVolume = args.get('takerBuyBaseAssetVolume', 1_000_000D)
        candle.takerBuyQuoteAssetVolume = args.get('takerBuyQuoteAssetVolume', 1_000_000D)
        return candle
    }

    static def createNewStrategy(
            Map args
    ) {
        return new ATHDivergenceStrategy(
                args.get('periodLength', 3),
                args.get('predicate', { str -> true }),
                args.get('profitPricePercent', 1.1),
                args.get('lossPricePercent', 0.9),
                args.get('athDeviationPercent', 0.8),
                args.get('increasePositionPercent', 0.9),
                args.get('maxNumberOfOpenPositions', 3),
                args.get('partOfUsdBalanceUse', 1.0),
                args.get('cooldownPeriods', 1),
        )
    }
}
