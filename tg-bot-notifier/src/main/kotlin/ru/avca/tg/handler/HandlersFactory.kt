//package ru.avca.tg.handler
//
//import com.pengrad.telegrambot.TelegramBot
//import io.micronaut.context.annotation.Bean
//import io.micronaut.context.annotation.Factory
//import io.micronaut.context.event.StartupEvent
//import io.micronaut.runtime.event.annotation.EventListener
//import javax.inject.Inject
//import javax.inject.Singleton
//
///**
// *
// * @author a.chermashentsev
// * Date: 20.05.2021
// **/
//@Factory
//class HandlersFactory(
//    @Inject val tgBot: TelegramBot
//) {
//
//    @EventListener
//    fun onStartupEvent(event: StartupEvent) {
//        mainHandler().handlers = map
//    }
//
//    @Singleton
//    fun mainHandler(): MainHandler {
//        return MainHandler(tgBot, )
//    }
//}