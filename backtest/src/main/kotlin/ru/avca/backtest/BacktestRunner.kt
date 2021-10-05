package ru.avca.backtest

import com.binance.api.client.BinanceApiRestClient
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import ru.avca.backtest.history.BinanceHistoryLoader
import ru.avca.backtest.history.HistoryLoader
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.streams.toList

/**
 *
 * @author a.chermashentsev
 * Date: 25.09.2021
 **/
@Singleton
class BacktestRunner(
    @Inject val historyLoader: HistoryLoader,
    @Inject val restClient: BinanceApiRestClient,
    @Inject val tickProcessor: TickProcessor
) {
    val startTime = LocalDate.parse("01-01-2021", DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay()
        .toInstant(ZoneOffset.UTC).toEpochMilli()
    val endTime = Instant.now().toEpochMilli()

    @EventListener
    fun onStart(event: StartupEvent) {
        val symbolsMap = tickProcessor.strategies.flatMap { it.intervals() }
            .distinct()
            .map { interval ->
                restClient.exchangeInfo.symbols
                    .filter { it.quoteAsset == "USDT" }
                    .flatMap { symbol ->
                        historyLoader.loadHistory(symbol.symbol, interval, startTime, endTime)
                            .map { symbol.symbol to it }.toList()
                    }
                    .groupBy { (_, candle) -> candle.openTime }
                    .toSortedMap()
            }
            .map {
                it.values.map { it.toMap() }
                    .forEach {
                        println(it.values.find { true }!!.openTime)
                        tickProcessor.onTimeTick(it)
                    }
                it
            }

        tickProcessor.contextes.map {
            val sum = it.value.balances
                .filter { it.key != "USDT" }
                .map { symbolToBalance ->
                    val closePrice = symbolsMap.last().values.last().find { it.first == symbolToBalance.key }?.second?.close?.toDouble() ?: symbolToBalance.value.openPriceToUsdt
                    symbolToBalance.value.quantity * closePrice
                }
                .sum() + (it.value.balances["USDT"]?.quantity ?: 0.0)
            it.key to sum
        }
            .sortedByDescending { it.second }
            .forEach {
                println(it.first)
                println(it.second.toString())
                println(tickProcessor.stats[it.first]!!)
                println("========")
            }
    }
}