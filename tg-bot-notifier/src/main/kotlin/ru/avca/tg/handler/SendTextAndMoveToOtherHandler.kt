package ru.avca.tg.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import ru.avca.TgHandler

/**
 *
 * @author a.chermashentsev
 * Date: 19.05.2021
 **/
class SendTextAndMoveToOtherHandler(
    private val text: String,
    private val nextHandler: TgHandler,
    private val tgBot: TelegramBot
) : TgHandler {
    override fun handle(update: Update): TgHandler {
        tgBot.execute(SendMessage(
            update.message().chat().id(),
            text
        ))
        return nextHandler
    }
}