package ru.avca.robot;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
@Singleton
public class MessageListenerTestHelper {
    private ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue<>();

    @Async
    @EventListener
    public void onEvent(Object event) {
        queue.add(event);
    }

    public Queue getQueue() {
        return queue;
    }

    public <T> Future<T> getEventFromQueue(Class<T> eventClass) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Optional eventOpt = queue.stream()
                    .filter(eventClass::isInstance)
                    .findAny();
            if (eventOpt.isPresent()) {
                future.complete((T) eventOpt.get());
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
        return future;
    }
}
