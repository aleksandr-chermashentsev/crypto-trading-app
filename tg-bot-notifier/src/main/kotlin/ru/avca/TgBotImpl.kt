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
import ru.avca.grpcservices.RobotTradeEvent
import ru.avca.tg.handler.MainHandler
import javax.inject.Inject

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
    @Volatile var adminChatId: Long? = null
    private var currentHandler:TgHandler = mainHandler

    @EventListener
    @Async
    open fun onStartup(event: StartupEvent) {
        if (adminChatIdStr.isNotEmpty()) {
            LOG.info("adminChatIdStr is {}. Try to parse", adminChatIdStr)
            adminChatId = adminChatIdStr.toLong()
            LOG.info("adminChatId set to {}", adminChatId)
        } else {
            LOG.info("adminChatId property is empty, wait for message from {}", adminUserName)
        }
        telegramBot.setUpdatesListener {
            it.forEach {
                if (adminChatId == null && it.message().from().username() == adminUserName.trim()) {
                    adminChatId = it.message().chat().id()
                }
                currentHandler = currentHandler.handle(it)
            }

            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    @EventListener
    @Async
    open fun onTradeEvent(event: RobotTradeEvent) {
        if (adminChatId == null) {
            LOG.info("Do nothing on trade event because adminChatId is not set")
            return
        }
        if (event.side == RobotTradeEvent.TradeSide.BUY) {
            telegramBot.execute(SendMessage(adminChatId, "Something was bought"))
        }
    }
}