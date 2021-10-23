package ru.avca.dbpersist.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 *
 * @author a.chermashentsev
 * Date: 03.05.2021
 **/
@Entity
@Table(name = "open_position")
class OpenPositionDomain(
    @Id
    val symbol: String,
    val price: String,
    val balance: String,
    val robotName: String,
    @Column(columnDefinition = "integer default 0")
    val rebuyCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenPositionDomain

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun toString(): String {
        return "OpenPositionDomain(symbol='$symbol', price='$price', balance='$balance', robotName='${robotName}' rebuyCount='$rebuyCount')"
    }

}