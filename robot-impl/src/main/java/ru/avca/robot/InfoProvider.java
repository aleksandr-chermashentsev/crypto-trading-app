package ru.avca.robot;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import ru.avca.robot.event.RobotEvents;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author a.chermashentsev
 * Date: 27.10.2021
 **/
@Context
public class InfoProvider {

    @Inject private MeterRegistry meterRegistry;
    @Inject private ApplicationEventPublisher publisher;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, AtomicDouble> gauges = new ConcurrentHashMap<>();

    @EventListener
    @Async
    public void onCloseToAthValues(RobotEvents.CloseToAthValues closeToAthValues) {
        AtomicDouble atomicDouble = gauges.computeIfAbsent(closeToAthValues.getSymbol(), key -> meterRegistry.gauge("robot.close_to_ath." + key,
                new AtomicDouble()
        ));
        atomicDouble.set(closeToAthValues.getClosePercent());
    }
}
