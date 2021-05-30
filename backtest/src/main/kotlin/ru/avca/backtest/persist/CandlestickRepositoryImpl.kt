package ru.avca.backtest.persist

import com.binance.api.client.domain.market.CandlestickInterval
import io.micronaut.transaction.annotation.ReadOnly
import java.util.stream.Stream
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.transaction.Transactional

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
@Singleton
open class CandlestickRepositoryImpl(private val em: EntityManager) : CandlestickRepository {
    @ReadOnly
    override fun getAllFromTimestamp(symbol: String, interval: CandlestickInterval, fromTimestamp: Long): Stream<CandlestickEntity> {
        val resultList =
            em.createQuery("select c from CandlestickEntity c where c.closeTime >= :fromTimestamp and c.downloadedFromTimestamp <= :fromTimestamp and c.candlestickKey.candlestickInterval = :interval and c.candlestickKey.symbol = :symbol", CandlestickEntity::class.java)
                .setParameter("fromTimestamp", fromTimestamp)
                .setParameter("interval", interval)
                .setParameter("symbol", symbol)
                .resultList
        return resultList.stream()
    }

    @Transactional
    override fun save(candlestickEntity: CandlestickEntity) {
        em.merge(candlestickEntity)
    }
}