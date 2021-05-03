package ru.avca.dbpersist.domain

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 *
 * @author a.chermashentsev
 * Date: 03.05.2021
 **/
@Entity
@Table(name = "currency_balance")
class CurrencyBalanceDomain(
    @Id
    val symbol: String,
    val balance: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CurrencyBalanceDomain

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun toString(): String {
        return "CurrencyBalanceDomain(symbol='$symbol', balance='$balance')"
    }
}