package ru.avca.robot;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.factory.BinanceFactory;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author a.chermashentsev
 * Date: 06.04.2021
 **/
class CandlestickListenerHealthCheckerTest {
    private final CandlestickEvents.ListenerKey key = new CandlestickEvents.ListenerKey("test", CandlestickInterval.ONE_MINUTE);

    @Test
    public void shouldSendRestartWhenDoesntGetUpdatesForLongTime() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("robot.candlestickListener.possible_gap_in_updates_ms", 1);
        values.put("robot.candlestickListener.check_time_period_multiplier", 1);
        values.put("test.send_binance_events", false);
        values.put("test.send_exception", false);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));

        Thread.sleep(100);

        messageListenerTestHelper.getQueue().poll();
        Object restartEvent = messageListenerTestHelper.getQueue().poll();

        assertNotNull(restartEvent);
        assertEquals(CandlestickEvents.RestartListenCandlesticksEvent.class, restartEvent.getClass());

    }

    @Test
    public void shouldNotSendRestartWhenGetUpdatesPeriodically() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("robot.candlestickListener.possible_gap_in_updates_ms", 30);
        values.put("robot.candlestickListener.check_time_period_multiplier", 5);
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 1);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));

        Thread.sleep(100);

        boolean hasRestartEvent = messageListenerTestHelper.getQueue().stream()
                .anyMatch(CandlestickEvents.RestartListenCandlesticksEvent.class::isInstance);
        assertFalse(hasRestartEvent, "Should not have restart event");

    }

    @Test
    public void shouldNotSendRestartWhenListenerStopped() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("robot.candlestickListener.possible_gap_in_updates_ms", 30);
        values.put("robot.candlestickListener.check_time_period_multiplier", 1);
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 5);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        Thread.sleep(20);
        eventPublisher.publishEvent(new CandlestickEvents.StopListenCandlesticksEvent(key));

        Thread.sleep(100);

        boolean hasRestartEvent = messageListenerTestHelper.getQueue().stream()
                .anyMatch(CandlestickEvents.RestartListenCandlesticksEvent.class::isInstance);
        assertFalse(hasRestartEvent, "Should not have restart event");

    }
}