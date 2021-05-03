package ru.avca.dbpersist.repository

import ru.avca.dbpersist.domain.OpenPositionDomain
import java.util.stream.Stream

/**
 *
 * @author a.chermashentsev
 * Date: 03.05.2021
 **/
interface OpenPositionRepository {

    fun updateOpenPositions(newPositions: List<OpenPositionDomain>)

    fun getAllOpenPositions(): Stream<OpenPositionDomain>
}