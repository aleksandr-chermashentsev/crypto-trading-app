package ru.avca

import io.micronaut.runtime.Micronaut.build


fun main(args: Array<String>) {
    build()
        .args(*args)
        .packages("ru.avca")
        .start()
}

