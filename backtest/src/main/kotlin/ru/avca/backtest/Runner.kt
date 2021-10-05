package ru.avca.backtest

import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.general.SymbolStatus
import com.binance.api.client.domain.market.CandlestickInterval.*
import io.micronaut.context.event.StartupEvent
import ru.avca.backtest.history.HistoryLoader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
@Singleton
open class Runner(
    @Inject val restClient: BinanceApiRestClient,
    @Inject val historyLoader: HistoryLoader
) {

//    @EventListener
    open fun onStart(event: StartupEvent) {
        val fromTimestamp = timestampOfTheDate("2018-01-01")

        for (interval in arrayOf(
            MONTHLY, WEEKLY, THREE_DAILY,
            DAILY, TWELVE_HOURLY, EIGHT_HOURLY,
            SIX_HOURLY, FOUR_HOURLY, TWO_HOURLY, HOURLY
        )) {
            restClient.exchangeInfo.symbols.stream()
                .filter { it.quoteAsset == "USDT" }
                .filter { it.status == SymbolStatus.TRADING }
                .map { it.symbol }
                .forEach { symbol ->
                    val count = historyLoader.loadHistory(symbol, interval, fromTimestamp, Instant.now().toEpochMilli()).count()
                    println("Loaded $count entries for $symbol in interval $interval")
                }
        }
    }

    private fun timestampOfTheDate(dateStr: String) =
        LocalDate.parse(dateStr).atStartOfDay().toEpochSecond(
            ZoneOffset.of("Z")
        ) * 1000
}