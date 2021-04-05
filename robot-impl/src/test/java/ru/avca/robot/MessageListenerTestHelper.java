package ru.avca.robot;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import ru.avca.robot.event.CandlestickEvents;

import javax.inject.Singleton;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
@Singleton
public class MessageListenerTestHelper {
    private ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue<>();

    @Async
    @EventListener
    public void onEvent(CandlestickEvents.BinanceCandlestickEvent event) {
        queue.add(event);
    }

    public Queue getQueue() {
        return queue;
    }
}
