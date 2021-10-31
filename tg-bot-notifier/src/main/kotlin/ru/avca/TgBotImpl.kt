package ru.avca

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.request.SendMessage
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.avca.grpcservices.ProfitInfo
import ru.avca.grpcservices.RobotRestartEvent
import ru.avca.grpcservices.RobotStartEvent
import ru.avca.grpcservices.RobotTradeEvent
import ru.avca.tg.handler.MainHandler
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 *
 * @author a.chermashentsev
 * Date: 23.04.2021
 **/
@Context
open class TgBotImpl(
    @Inject val telegramBot: TelegramBot,
    @Inject val mainHandler: MainHandler,
    @Value("\${telegram.adminUsername}") val adminUserName: String,
    @Value("\${telegram.adminChatId:}") var adminChatIdStr: String,
) {
    private val LOG: Logger = LoggerFactory.getLogger(TgBotImpl::class.java)
    var adminChatIds: List<Long> = listOf()
    private var currentHandler:TgHandler = mainHandler

    @EventListener
    @Async
    open fun onStartup(event: StartupEvent) {
        if (adminChatIdStr.isNotEmpty()) {
            LOG.info("adminChatIdStr is {}. Try to parse", adminChatIdStr)
            adminChatIds = adminChatIdStr.split(",").map { it.toLong() }
            LOG.info("adminChatIds set to {}", adminChatIds)
        } else {
            LOG.info("adminChatIds property is empty, wait for message from {}", adminUserName)
        }
        telegramBot.setUpdatesListener {
            it.forEach {
                currentHandler = currentHandler.handle(it)
            }

            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    @EventListener
    @Async
    open fun onTradeEvent(event: RobotTradeEvent) {
        LOG.info("got event $event")
        if (adminChatIds.isEmpty()) {
            LOG.info("Do nothing on trade event because adminChatId is not set")
            return
        }
        if (event.side == RobotTradeEvent.TradeSide.BUY) {
            val price = event.quoteQty / event.baseQty
            adminChatIds.forEach {
                telegramBot.execute(SendMessage(it,
                    "${event.symbol} was bought\n" +
                            "💵 USDT quantity ${event.quoteQty}\n" +
                            "🗑 Slippage is ${((1 - price / event.expectedPrice) * 100).roundToInt()}%"
                ))
            }
        }
        else {
            adminChatIds.forEach {
                telegramBot.execute(
                    SendMessage(it,
                        "${event.symbol} sell\n" +
                                "💵 USDT quantity ${event.quoteQty}\n"
                    )
                )
            }
        }
    }

    @EventListener
    @Async
    open fun onRestartEvent(event: RobotRestartEvent) {
        if (adminChatIds.isEmpty()) {
            LOG.info("Do nothing on restart event because adminChatId is not set")
            return
        }
        adminChatIds.forEach { telegramBot.execute(SendMessage(it, "Connections issue. Robot listener was restarted")) }

    }

    @EventListener
    @Async
    open fun onRestartEvent(event: RobotStartEvent) {
        if (adminChatIds.isEmpty()) {
            LOG.info("Do nothing on start event because adminChatId is not set")
            return
        }
        adminChatIds.forEach { telegramBot.execute(SendMessage(it, "Robot has started")) }

    }

    @EventListener
    @Async
    open fun onOpenPositionInfo(event: ProfitInfo) {
        if (adminChatIds.isEmpty()) {
            LOG.info("Do nothing on start event because adminChatId is not set")
            return
        }
        if (event.openPositionsUsdtBalance <= 0) {
            adminChatIds.forEach {
                telegramBot.execute(SendMessage(it,
                    "All positions closed, current balance is ${event.openPositionsUsdtBalance}"
                ))
            }

            return
        }
        var rocketSign = "\uD83D\uDE80"//🚀
        if (event.oldUsdtBalance > event.openPositionsUsdtBalance) {
            rocketSign = "\uD83D\uDCC9"//📉
        }

        adminChatIds.forEach {
            telegramBot.execute(SendMessage(it,
                "$rocketSign Current balance is ${event.openPositionsUsdtBalance}, old balance is ${event.oldUsdtBalance}"
            ))
        }
    }
}