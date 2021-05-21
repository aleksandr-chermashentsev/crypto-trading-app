package ru.avca.tg.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import ru.avca.TgHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author a.chermashentsev
 * Date: 18.05.2021
 **/
@Singleton
class HelpHandler(
    @Inject val telegramBot: TelegramBot,
    ) : TgHandler {
    lateinit var mainHandler: MainHandler
        @Inject set

    override fun handle(update: Update): TgHandler {
        telegramBot.execute(
            SendMessage(
                update.message().chat().id(),
                "💵 in order to change usd balance send /changeUsd\n" +
                    "🚏 in order to close all current positions send /closePositions"
            )
        )
        return mainHandler
    }
}