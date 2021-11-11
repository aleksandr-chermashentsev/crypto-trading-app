package ru.avca.dbpersist.repository

import ru.avca.dbpersist.domain.TurnedOffSymbols

/**
 *
 * @author a.chermashentsev
 * Date: 10.11.2021
 **/
interface TurnedOffSymbolsRepository {
    fun getAllSymbols(): List<TurnedOffSymbols>
    fun turnOffSymbol(symbol: String)
    fun turnOnSymbol(symbol: String)
}