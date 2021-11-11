package ru.avca.dbpersist.repository

import io.micronaut.transaction.annotation.TransactionalAdvice
import ru.avca.dbpersist.domain.TurnedOffSymbols
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Singleton
@TransactionalAdvice
open class TurnedOffSymbolsRepositoryImpl(
    private val em: EntityManager
) : TurnedOffSymbolsRepository {
    @Transactional
    override fun getAllSymbols(): List<TurnedOffSymbols> {
        return em.createQuery("select s from TurnedOffSymbols s", TurnedOffSymbols::class.java).resultList
    }

    override fun turnOffSymbol(symbol: String) {
        em.persist(TurnedOffSymbols(symbol))
    }

    override fun turnOnSymbol(symbol: String) {
        em.remove(TurnedOffSymbols(symbol))
    }
}