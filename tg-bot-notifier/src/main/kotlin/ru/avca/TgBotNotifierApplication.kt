package ru.avca

import io.micronaut.runtime.Micronaut.build

class TgBotNotifierApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            build()
                .args(*args)
                .packages("ru.avca.*")
                .start()
        }
    }
}

