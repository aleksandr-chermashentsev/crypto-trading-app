package ru.avca.dbpersist.repository

import io.micronaut.transaction.annotation.TransactionalAdvice
import ru.avca.dbpersist.domain.OpenPositionDomain
import java.util.stream.Stream
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Singleton
@TransactionalAdvice
open class OpenPositionRepositoryImpl(
    private val em: EntityManager
) : OpenPositionRepository {

    @Transactional
    override fun updateOpenPositions(newPositions: List<OpenPositionDomain>) {
        em.createQuery("delete from OpenPositionDomain ")
            .executeUpdate()
        newPositions.forEach { em.persist(it) }
    }

    override fun getAllOpenPositions(robotName: String): Stream<OpenPositionDomain> {
        return em.createQuery("select op from OpenPositionDomain op where op.robotName=:robotName", OpenPositionDomain::class.java)
            .setParameter("robotName", robotName)
            .resultList
            .stream()
    }
}