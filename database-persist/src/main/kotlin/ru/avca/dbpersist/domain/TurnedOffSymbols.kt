package ru.avca.dbpersist.domain

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 *
 * @author a.chermashentsev
 * Date: 10.11.2021
 **/
@Entity
@Table(name = "turned_off_symbols")
class TurnedOffSymbols(
    @Id
    val symbol: String
) {
}