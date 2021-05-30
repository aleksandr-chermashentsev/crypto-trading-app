package ru.avca.backtest.persist

import com.binance.api.client.domain.market.CandlestickInterval
import java.io.Serializable
import javax.persistence.Embeddable

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
@Embeddable
class CandlestickKey(
    var candlestickInterval: CandlestickInterval,
    var symbol: String,
    var openTime: Long
) : Serializable {
}