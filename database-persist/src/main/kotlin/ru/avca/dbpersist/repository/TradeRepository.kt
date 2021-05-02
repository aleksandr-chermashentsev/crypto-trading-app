package ru.avca.dbpersist.repository

import ru.avca.dbpersist.domain.TradeDomain
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 *
 * @author a.chermashentsev
 * Date: 01.05.2021
 **/
interface TradeRepository {

    fun findById(@NotNull id: Long): TradeDomain?

    fun save(@NotBlank tradeDomain: TradeDomain): TradeDomain
}