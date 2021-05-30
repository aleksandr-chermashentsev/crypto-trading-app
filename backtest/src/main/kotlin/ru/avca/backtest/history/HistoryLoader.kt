package ru.avca.backtest.history

import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.avca.backtest.persist.CandlestickRepository
import ru.avca.backtest.persist.toEntity
import java.util.stream.Stream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.streams.toList

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
@Singleton
class HistoryLoader(
    @Inject val binanceHistoryLoader: BinanceHistoryLoader,
    @Inject val candlestickRepository: CandlestickRepository
) {
    private val LOG: Logger = LoggerFactory.getLogger(HistoryLoader::class.java)
    fun loadHistory(symbol:String, interval: CandlestickInterval, fromTimestamp:Long): Stream<Candlestick> {
        val fromDb = candlestickRepository.getAllFromTimestamp(symbol, interval, fromTimestamp)
            .toList()
        if (fromDb.isEmpty()) {
            LOG.info("Could not find $symbol $interval $fromTimestamp in db so load it from binance")
            return binanceHistoryLoader.loadHistory(symbol, interval, fromTimestamp)
                .peek {candlestickRepository.save(it.toEntity(it, symbol, interval, fromTimestamp))}
                .toList()
                .stream()
        }
        return fromDb.stream().map { it.toBinanceCandlestick() }
    }
}