package ru.avca.robot;

import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.Scheduled;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.event.CandlestickEvents;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ru.avca.robot.utils.TimeUtils.getCurrentTimeUtc;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
@Context
public class CandlestickListenerHealthChecker {
    private static final Logger LOG = LoggerFactory.getLogger(CandlestickListenerHealthChecker.class);
    @Value("${candlestickListener.possible_gap_in_updates_ms:60000}")
    private int possibleGapInUpdatesMs;
    @Value("${candlestickListener.check_time_period_multiplier:10}")
    private int checkTimePeriodMultiplier;
    @Inject
    private ApplicationEventPublisher eventPublisher;
    protected final TaskScheduler taskScheduler;

    private int checkTimePeriodMs;

    private final ConcurrentHashMap<CandlestickEvents.ListenerKey, Long> startedSymbols = new ConcurrentHashMap<>();
    private final AtomicBoolean isStartedSymbolsEmptyOnStartOfScheduleInterval = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> scheduledFuture;

    public CandlestickListenerHealthChecker(@Named(TaskExecutors.SCHEDULED) TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Async
    @EventListener
    public void onStartCandlestickListen(CandlestickEvents.ListenerHasStartedListenToCandlestickEvent event) {
        startedSymbols.put(event.getKey(), getCurrentTimeUtc());
    }

    @Async
    @EventListener
    public void onStopCandlestickListen(CandlestickEvents.ListenerHasStoppedListenToCandlestickEvent event) {
        LOG.info("Stop event {}", event);
        startedSymbols.remove(event.getKey());
    }

    @Async
    @EventListener
    public void onCandlestickEvent(CandlestickEvents.BinanceCandlestickEvent candlestickEvent) {
        startedSymbols.put(candlestickEvent.getKey(), getCurrentTimeUtc());
    }

    @Async
    @EventListener
    public void onApplicationStart(StartupEvent event) {
        checkTimePeriodMs = possibleGapInUpdatesMs * checkTimePeriodMultiplier;
        LOG.info("Start ListenerHealthchecker with params possibleGapInUpdatesMs={}, checkTimePeriodMs={}", possibleGapInUpdatesMs, checkTimePeriodMs);
        Duration duration = Duration.ofMillis(checkTimePeriodMs);
        scheduledFuture = taskScheduler.scheduleAtFixedRate(duration, duration, () -> {
            if (isStartedSymbolsEmptyOnStartOfScheduleInterval.get()) {
                boolean isStartedSymbolsEmpty = startedSymbols.isEmpty();
                isStartedSymbolsEmptyOnStartOfScheduleInterval.set(isStartedSymbolsEmpty);
                LOG.info("Started symbols empty. New value {}", isStartedSymbolsEmpty);
                return;
            }
            boolean isEmpty = startedSymbols.isEmpty();
            LOG.info("Started symbols not empty. New value {}", isEmpty);
            isStartedSymbolsEmptyOnStartOfScheduleInterval.set(isEmpty);

            long currentTimeUtc = getCurrentTimeUtc();
            List<CandlestickEvents.ListenerKey> notUpdatedListenerKeysForLongTime = startedSymbols.entrySet().stream()
                    .filter(entry -> entry.getValue() + possibleGapInUpdatesMs < currentTimeUtc)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (notUpdatedListenerKeysForLongTime.size() > 0) {
                LOG.info("Not updated listener keys {}. Listener for this keys will be restarted", notUpdatedListenerKeysForLongTime);
                notUpdatedListenerKeysForLongTime.forEach(key ->
                        eventPublisher.publishEventAsync(new CandlestickEvents.RestartListenCandlesticksEvent(key)));
            }
        });
    }

}
