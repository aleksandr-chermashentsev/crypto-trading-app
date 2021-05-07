package ru.avca.robot;

import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.avca.robot.event.CandlestickEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author a.chermashentsev
 * Date: 06.04.2021
 **/
@MicronautTest
@Timeout(3)
@Disabled
class CandlestickListenerHealthCheckerTest {
    private final CandlestickEvents.ListenerKey key = new CandlestickEvents.ListenerKey("test", CandlestickInterval.ONE_MINUTE);

    @Test
    public void shouldSendRestartWhenDoesntGetUpdatesForLongTime() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("candlestickListener.possible_gap_in_updates_ms", 1);
        values.put("candlestickListener.check_time_period_multiplier", 1);
        values.put("test.send_binance_events", false);
        values.put("test.send_exception", false);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);

        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent must present");

        Future<CandlestickEvents.RestartListenCandlesticksEvent> restartEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.RestartListenCandlesticksEvent.class);

        assertNotNull(restartEventFuture.get(), "RestartListenCandlesticksEvent must present");
    }

    @Test
    public void shouldNotSendRestartWhenGetUpdatesPeriodically() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("candlestickListener.possible_gap_in_updates_ms", 30);
        values.put("candlestickListener.check_time_period_multiplier", 5);
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 1);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));

        try {
            Future<CandlestickEvents.RestartListenCandlesticksEvent> restartEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.RestartListenCandlesticksEvent.class);
            CandlestickEvents.RestartListenCandlesticksEvent restartEvent = restartEventFuture.get(1, TimeUnit.SECONDS);
            fail("There is should be no restart event but got one " + restartEvent);
        } catch (Exception e) {

        }
    }

    @Test
    public void shouldNotSendRestartWhenListenerStopped() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("candlestickListener.possible_gap_in_updates_ms", 30);
        values.put("candlestickListener.check_time_period_multiplier", 1);
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 5);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        Thread.sleep(20);
        eventPublisher.publishEvent(new CandlestickEvents.StopListenCandlesticksEvent(key));

        try {
            Future<CandlestickEvents.RestartListenCandlesticksEvent> restartEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.RestartListenCandlesticksEvent.class);
            CandlestickEvents.RestartListenCandlesticksEvent restartEvent = restartEventFuture.get(1, TimeUnit.SECONDS);
            fail("There is should be no restart event but got one " + restartEvent);
        } catch (Exception e) {

        }
    }
}