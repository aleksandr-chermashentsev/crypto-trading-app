package ru.avca.backtest.factory

import io.micronaut.context.annotation.Factory
import ru.avca.backtest.ATHDivergenceStrategy
import ru.avca.backtest.BacktestStrategy
import ru.avca.backtest.TickProcessor
import javax.inject.Singleton

/**
 *
 * @author a.chermashentsev
 * Date: 26.09.2021
 **/
@Factory
class StrategyFactory {

    @Singleton
    fun tickProcessor(): TickProcessor {
        val strategies = ArrayList<ATHDivergenceStrategy>()
        for (periodLength in 30 until 250 step 20) {
            for (profitPricePercent in 10 until 30 step 10) {
                for (lossPricePercent in 10 until 50 step 5) {
                    for (athDeviationPercent in 10 until 50 step 5) {
                        for (increasePositionPercent in 10 until 50 step 5) {
                            for (maxNumberOfOpenPositions in 1 until 10) {
                                strategies.add(ATHDivergenceStrategy(
                                    periodLength = periodLength,
                                    symbolsPredicate = {true},
                                    profitPricePercent = 1 + profitPricePercent/100.0,
                                    lossPricePercent = 1 - lossPricePercent/100.0,
                                    athDeviationPercent = 1 - athDeviationPercent/100.0,
                                    increasePositionPercent = 1 - increasePositionPercent/100.0,
                                    maxNumberOfOpenPositions = maxNumberOfOpenPositions,
                                    partOfUsdBalanceUse = 1/(maxNumberOfOpenPositions + maxNumberOfOpenPositions * 0.8),
                                    cooldownPeriods = 5))
                            }
                        }
                    }
                }
            }
        }
        return TickProcessor(strategies as List<BacktestStrategy<out Any, in Any>>, 1000.0, false)
//        return TickProcessor(listOf(ATHDivergenceStrategy(
//            periodLength = 30,
//            symbolsPredicate = {true},
////            symbolsPredicate = {!it.toLowerCase().contains("down") && !it.toLowerCase().contains("up")},
//            profitPricePercent = 1.2,
//            lossPricePercent = 0.55,
//            athDeviationPercent = 0.8,
//            increasePositionPercent = 0.95,
//            maxNumberOfOpenPositions = 5,
//            partOfUsdBalanceUse = 0.11,
//            cooldownPeriods = 5
//        )), 1000.0)
    }
}