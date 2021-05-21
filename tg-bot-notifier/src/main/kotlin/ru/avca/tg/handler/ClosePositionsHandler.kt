package ru.avca.tg.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import ru.avca.TgHandler
import ru.avca.grpcservices.ClosePositionsMsg
import ru.avca.grpcservices.RobotStateManagerGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClosePositionsHandler(
    @Inject private val telegramBot: TelegramBot,
    @Inject private val stub: RobotStateManagerGrpc.RobotStateManagerBlockingStub,
    @Inject private val mainHandler: MainHandler
) : TgHandler {
    override fun handle(update: Update): TgHandler {
        val chatId = update.message().chat().id()
        try {
            val closePositions = stub.closePositions(ClosePositionsMsg.getDefaultInstance())
            if (closePositions.success) {
                telegramBot.execute(SendMessage(
                    chatId,
                    "✔ position was successfully closed"
                ))
            }
            else {
                sendError(chatId)
            }
        } catch (e:Exception) {
            sendError(chatId)
        }

        return mainHandler
    }

    private fun sendError(chatId: Long) {
        telegramBot.execute(
            SendMessage(
                chatId,
                "❌ position wasn't closed because of some exception occurs"
            )
        )
    }

}
