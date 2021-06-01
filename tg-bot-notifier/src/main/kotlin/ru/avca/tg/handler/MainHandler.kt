package ru.avca.tg.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import ru.avca.TgHandler
import ru.avca.grpcservices.RobotStateManagerGrpc
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * @author a.chermashentsev
 * Date: 18.05.2021
 **/
@Singleton
class MainHandler(
    @Inject val tgBot: TelegramBot,
    @Inject val applicationContext: ApplicationContext,
    @Inject val stub: RobotStateManagerGrpc.RobotStateManagerBlockingStub
) : TgHandler {

    lateinit var changeUsdHandler: ChangeUsdHandler
    lateinit var closePositionsHandler: ClosePositionsHandler
    lateinit var helpHandler: HelpHandler
    lateinit var handlers: Map<String, TgHandler>

    @EventListener
    fun init(event:StartupEvent) {
        this.helpHandler = applicationContext.getBean(HelpHandler::class.java)
        this.changeUsdHandler = applicationContext.getBean(ChangeUsdHandler::class.java)
        this.closePositionsHandler = applicationContext.getBean(ClosePositionsHandler::class.java)
        handlers = mapOf(
            "/help" to helpHandler,
            "/changeUsd" to SendTextAndMoveToOtherHandler(
                "Submit your new usd balance. Use . as delimiter if you want to specify fractional part.",
                changeUsdHandler,
                tgBot
            ),
            "/getProfitInfo" to GetCurrentProfitInfoHandler(
                tgBot,
                stub,
                this
            ),
            "/closePositions" to closePositionsHandler
        )
    }


    override fun handle(update: Update): TgHandler {
        val message = update.message() ?: return this
        val tgHandler = handlers[message.text()]
        return tgHandler?.handle(update) ?: handlers["/help"]!!.handle(update)
    }
}