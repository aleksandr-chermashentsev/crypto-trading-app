package ru.avca.factory

import com.pengrad.telegrambot.TelegramBot
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value

/**
 *
 * @author a.chermashentsev
 * Date: 23.04.2021
 **/
@Factory
class TgBotFactory {
    @Bean
    fun telegramBot(@Value("\${telegram.token}") telegramToken: String) : TelegramBot {
        return TelegramBot(telegramToken)
    }
}