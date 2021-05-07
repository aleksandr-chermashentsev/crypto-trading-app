package ru.avca

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.request.SendMessage
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async
import ru.avca.grpcservices.RobotTradeEvent
import javax.inject.Inject

/**
 *
 * @author a.chermashentsev
 * Date: 23.04.2021
 **/
@Context
open class TgBotImpl(
    @Inject val telegramBot: TelegramBot,
    @Value("\${telegram.adminUsername}") val adminUserName: String,
    @Volatile @Value("\${telegram.adminChatId:null}") var adminChatId: Long?
) {
    @EventListener
    @Async
    open fun onStartup(event: StartupEvent) {
        telegramBot.setUpdatesListener {
            it.forEach {
                if (adminChatId == null && it.message().from().username() == adminUserName.trim()) {
                    adminChatId = it.message().chat().id()
                }
            }

            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    @EventListener
    @Async
    open fun onTradeEvent(event: RobotTradeEvent) {
        if (event.side == RobotTradeEvent.TradeSide.BUY) {
            telegramBot.execute(SendMessage(adminChatId, "Something was bought"))
        }
    }
}