package ru.avca.dbpersist.repository

import io.micronaut.transaction.annotation.ReadOnly
import ru.avca.dbpersist.domain.TradeDomain
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.transaction.Transactional

/**
 *
 * @author a.chermashentsev
 * Date: 02.05.2021
 **/
@Singleton
open class TradeRepositoryImpl(
    private val em: EntityManager
) : TradeRepository {

    @ReadOnly
    override fun findById(id: Long): TradeDomain =
        em.find(TradeDomain::class.java, id)

    @Transactional
    override fun save(tradeDomain: TradeDomain) =
        em.merge(tradeDomain)


}