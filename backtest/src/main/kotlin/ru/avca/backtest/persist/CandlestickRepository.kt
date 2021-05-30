package ru.avca.backtest.persist

import com.binance.api.client.domain.market.CandlestickInterval
import java.util.stream.Stream

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
interface CandlestickRepository {
    fun getAllFromTimestamp(symbol: String, interval: CandlestickInterval, fromTimestamp: Long): Stream<CandlestickEntity>

    fun save(candlestickEntity: CandlestickEntity)
}