package ru.avca.backtest

import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import java.util.stream.Stream

/**
 *
 * @author a.chermashentsev
 * Date: 23.09.2021
 **/
interface BacktestStrategy<SHARED_DATA_KEY, SHARED_DATA> {

    /**
     * @return intervals, required by strategy
     */
    fun intervals() : List<CandlestickInterval>

    /**
     * @param candles slice of candles with same CandlestickInterval and endTime.
     * @param context state of virtual account with balances
     * @return stream of executed buy/sell operations
     */
    fun onTimeTick(candles: Map<String, Candlestick>, context: Context, sharedData: SHARED_DATA): List<BacktestStrategyOperation>

    fun sharedData(): SharedData<SHARED_DATA_KEY, SHARED_DATA>

    interface SharedData<KEY, DATA> {
        fun key(): KEY
        fun collectData(candles: Map<String, Candlestick>, sharedData: DATA?): DATA
    }
}