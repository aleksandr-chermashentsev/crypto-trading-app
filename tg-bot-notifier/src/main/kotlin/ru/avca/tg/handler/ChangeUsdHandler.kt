package ru.avca.tg.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.avca.TgHandler
import ru.avca.grpcservices.RobotStateManagerGrpc
import ru.avca.grpcservices.SetUsdBalanceMsg
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author a.chermashentsev
 * Date: 18.05.2021
 **/
@Singleton
class ChangeUsdHandler (
    @Inject val telegramBot: TelegramBot,
    @Inject val stub: RobotStateManagerGrpc.RobotStateManagerBlockingStub,
    @Inject val mainHandler: MainHandler
    ) : TgHandler{
    private val LOG: Logger = LoggerFactory.getLogger(ChangeUsdHandler::class.java)
    override fun handle(update: Update):TgHandler {
        val text = update.message().text()
        val chatId = update.message().chat().id()
        when(text.toBigDecimalOrNull()) {
            null -> telegramBot.execute(SendMessage(
                chatId, "❌ fail to parse sent balance"
            ))
            else -> {
                try {
                    val response = stub.setUsdBalance(
                        SetUsdBalanceMsg
                            .newBuilder()
                            .setBalance(text)
                            .build()
                    )
                    if (response.success) {
                        telegramBot.execute(SendMessage(
                            chatId, "✔ usd balance updated"
                        ))
                    } else {
                        sendError(chatId)
                    }
                } catch (e:Exception) {
                    LOG.error("Exception when try to connect to robotStateManager", e)
                    sendError(chatId)
                }
            }
        }
        return mainHandler
    }

    private fun sendError(chatId: Long?) {
        telegramBot.execute(
            SendMessage(
                chatId, "❌ something went wrong when try to update usd balance"
            )
        )
    }
}