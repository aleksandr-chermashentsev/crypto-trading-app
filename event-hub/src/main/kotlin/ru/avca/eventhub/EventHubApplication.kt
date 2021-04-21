package ru.avca.eventhub

import io.micronaut.runtime.Micronaut

/**
 *
 * @author a.chermashentsev
 * Date: 18.04.2021
 **/
fun main(args: Array<String>) {
    Micronaut.build()
            .args(*args)
            .packages("ru.avca.*")
            .start()
}