package ru.avca.backtest.persist

import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Table

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
@Entity
@Table(name = "candlestick")
class CandlestickEntity(
    @EmbeddedId
    val candlestickKey: CandlestickKey,
    val downloadedFromTimestamp: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val closeTime: Long,
    val quoteAssetVolume: String,
    val numberOfTrades: Long,
    val takerBuyBaseAssetVolume: String,
    val takerBuyQuoteAssetVolume: String
) {
    fun toBinanceCandlestick(): Candlestick {
        val candlestick = Candlestick()
        candlestick.open = open
        candlestick.openTime = candlestickKey.openTime
        candlestick.high = high
        candlestick.low = low
        candlestick.close = close
        candlestick.volume = volume
        candlestick.closeTime = closeTime
        candlestick.quoteAssetVolume = quoteAssetVolume
        candlestick.numberOfTrades = numberOfTrades
        candlestick.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume
        candlestick.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume
        return candlestick
    }

    override fun toString(): String {
        return "CandlestickEntity(candlestickKey=$candlestickKey, downloadedFromTimestamp=$downloadedFromTimestamp, open='$open', high='$high', low='$low', close='$close', volume='$volume', closeTime=$closeTime, quoteAssetVolume='$quoteAssetVolume', numberOfTrades=$numberOfTrades, takerBuyBaseAssetVolume='$takerBuyBaseAssetVolume', takerBuyQuoteAssetVolume='$takerBuyQuoteAssetVolume')"
    }
}

fun Candlestick.toEntity(
    candlestick: Candlestick,
    symbol: String,
    interval: CandlestickInterval,
    downloadedFromTimestamp: Long
): CandlestickEntity =
    CandlestickEntity(
        CandlestickKey(interval, symbol, candlestick.openTime),
        downloadedFromTimestamp,
        candlestick.open,
        candlestick.high,
        candlestick.low,
        candlestick.close,
        candlestick.volume,
        candlestick.closeTime,
        candlestick.quoteAssetVolume,
        candlestick.numberOfTrades,
        candlestick.takerBuyBaseAssetVolume,
        candlestick.takerBuyQuoteAssetVolume
    )
