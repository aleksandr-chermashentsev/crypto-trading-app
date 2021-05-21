package ru.avca

import com.pengrad.telegrambot.model.Update

/**
 *
 * @author a.chermashentsev
 * Date: 18.05.2021
 **/
interface TgHandler {
    fun handle(update: Update): TgHandler
}