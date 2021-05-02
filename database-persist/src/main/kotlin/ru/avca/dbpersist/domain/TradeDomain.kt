package ru.avca.dbpersist.domain

import javax.persistence.*

/**
 *
 * @author a.chermashentsev
 * Date: 30.04.2021
 **/
@Entity
@Table(name = "trade")
class TradeDomain(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long?,
    val eventTime: Long,
    val symbol: String,
    val baseQty: String,
    val quoteQty: String,
    val side: String
) {
    override fun hashCode() = 25
    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (this === other) return true
        if (this.javaClass != other.javaClass) return false
        other as TradeDomain
        other.id != null && other.id == id
        return false
    }

    override fun toString(): String {
        return "TradeDomain(id=$id, eventTime=$eventTime, symbol='$symbol', baseQty='$baseQty', quoteQty='$quoteQty', side='$side')"
    }

}