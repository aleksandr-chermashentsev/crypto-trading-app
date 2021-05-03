package ru.avca.dbpersist.repository

import ru.avca.dbpersist.domain.CurrencyBalanceDomain
import java.util.stream.Stream
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Singleton
open class CurrencyBalanceRepositoryImpl(
    private val em: EntityManager
) : CurrencyBalanceRepository {

    @Transactional
    override fun saveCurrencyBalance(currencyBalanceDomain: CurrencyBalanceDomain) {
        em.merge(currencyBalanceDomain)
    }

    override fun getAllCurrencyBalances(): Stream<CurrencyBalanceDomain> {
        val resultList = em.createQuery("select cb from CurrencyBalanceDomain cb", CurrencyBalanceDomain::class.java).resultList
        return resultList.stream()
    }
}