package ru.avca.dbpersist.repository

import ru.avca.dbpersist.domain.OpenPositionDomain
import java.util.stream.Stream
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Singleton
open class OpenPositionRepositoryImpl(
    private val em: EntityManager
) : OpenPositionRepository {

    @Transactional
    override fun updateOpenPositions(newPositions: List<OpenPositionDomain>) {
        em.createQuery("delete from OpenPositionDomain ")
            .executeUpdate()
        newPositions.forEach { em.persist(OpenPositionDomain(it.symbol, it.price, it.balance)) }
    }

    override fun getAllOpenPositions(): Stream<OpenPositionDomain> {
        return em.createQuery("select op from OpenPositionDomain op", OpenPositionDomain::class.java)
            .resultStream
    }
}