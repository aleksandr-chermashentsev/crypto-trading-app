package ru.avca.dbpersist.repository

import ru.avca.dbpersist.domain.CurrencyBalanceDomain
import java.util.stream.Stream

/**
 *
 * @author a.chermashentsev
 * Date: 03.05.2021
 **/
interface CurrencyBalanceRepository {

    fun saveCurrencyBalance(currencyBalanceDomain: CurrencyBalanceDomain)

    fun getAllCurrencyBalances(): Stream<CurrencyBalanceDomain>
}