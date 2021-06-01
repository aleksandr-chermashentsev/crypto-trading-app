package ru.avca.tg.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.SendMessage
import ru.avca.TgHandler
import ru.avca.grpcservices.Empty
import ru.avca.grpcservices.RobotStateManagerGrpc
import javax.inject.Inject

/**
 *
 * @author a.chermashentsev
 * Date: 01.06.2021
 **/
class GetCurrentProfitInfoHandler(
    @Inject private val telegramBot: TelegramBot,
    @Inject private val stub: RobotStateManagerGrpc.RobotStateManagerBlockingStub,
    @Inject private val mainHandler: MainHandler
) : TgHandler{

    override fun handle(update: Update): TgHandler {
        val chatId = update.message().chat().id()
        try {
            val currentProfitInfo = stub.getCurrentProfit(Empty.getDefaultInstance())
            if (currentProfitInfo.openPositionsUsdtBalance <= 0) {
                telegramBot.execute(SendMessage(chatId,
                    "All positions closed, current balance is $currentProfitInfo.currentUsdtBalance"
                ))
                return mainHandler
            }
            var rocketSign = "\uD83D\uDE80"//🚀
            if (currentProfitInfo.oldUsdtBalance > currentProfitInfo.openPositionsUsdtBalance) {
                rocketSign = "\uD83D\uDCC9"//📉
            }

            telegramBot.execute(SendMessage(chatId,
                "$rocketSign Current balance is $currentProfitInfo.currentUsdtBalance, old balance is $currentProfitInfo.usdtBalance"
            ))
        } catch (e:Exception) {
            telegramBot.execute(SendMessage(chatId,
                "Some error occurred, please check logs"
            ))
        }

        return mainHandler
    }
}