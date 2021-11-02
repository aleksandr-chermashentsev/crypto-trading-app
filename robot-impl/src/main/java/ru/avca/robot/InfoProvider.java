package ru.avca.robot;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author a.chermashentsev
 * Date: 27.10.2021
 **/
@Context
public class InfoProvider {

    @Inject private MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicDouble> athValueGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> candleStickLastUpdateTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> candleStickGauges = new ConcurrentHashMap<>();

    @EventListener
    @Async
    public void onCloseToAthValues(RobotEvents.CloseToAthValues closeToAthValues) {
        AtomicDouble atomicDouble = athValueGauges.computeIfAbsent(closeToAthValues.getSymbol(), key -> meterRegistry.gauge("robot.close_to_ath", List.of(Tag.of("symbol", key)),
                new AtomicDouble()
        ));
        atomicDouble.set(closeToAthValues.getClosePercent());
    }

    @EventListener
    @Async
    public void onCandlestickEvent(CandlestickEvents.BinanceCandlestickEvent binanceCandlestickEvent) {
        long currentTime = System.currentTimeMillis();
        String symbol = binanceCandlestickEvent.getBinanceEvent().getSymbol();
        AtomicLong lastUpdateTime = candleStickLastUpdateTimes.computeIfAbsent(symbol, key -> new AtomicLong());
        long lastUpdateTimeUnboxed = lastUpdateTime.get();
        if (lastUpdateTimeUnboxed > 0) {
            AtomicInteger diffBetweenUpdateTime = candleStickGauges.computeIfAbsent(symbol,
                    key -> meterRegistry.gauge("robot.candle_update_interval", List.of(Tag.of("symbol", symbol)), new AtomicInteger())
            );

            diffBetweenUpdateTime.set((int) (currentTime - lastUpdateTimeUnboxed));
        }

        lastUpdateTime.set(currentTime);
    }
}
