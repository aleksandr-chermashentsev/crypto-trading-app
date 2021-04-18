package ru.avca.eventhub

import io.micronaut.context.annotation.Context
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async

/**
 *
 * @author a.chermashentsev
 * Date: 18.04.2021
 **/
@Context
open class SimpleService {

    @EventListener
    @Async
    open fun doSomethingOnStart(event: StartupEvent) {

    }
}