package ru.avca.robot;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.*;
import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.avca.robot.event.CandlestickEvents;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
@MicronautTest
public class CandlestickListenerTest {
    private final CandlestickEvents.ListenerKey key = new CandlestickEvents.ListenerKey("test", CandlestickInterval.THREE_DAILY);

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void shouldSendCandlesticksWhenGotStartEvent() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 20);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        MessageListenerTestHelper messageListener = context.getBean(MessageListenerTestHelper.class);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        Queue queue = messageListener.getQueue();
        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        boolean hasCandlestickEvents = false;

        //wait for event or fail by timeout
        while (!hasCandlestickEvents) {
            Thread.sleep(10);
            hasCandlestickEvents = queue.stream()
                    .anyMatch(CandlestickEvents.BinanceCandlestickEvent.class::isInstance);
        }

        context.close();
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void shouldStopWhenGotStopEvent() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 20);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        MessageListenerTestHelper messageListener = context.getBean(MessageListenerTestHelper.class);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        Queue queue = messageListener.getQueue();
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        boolean hasCandlestickEvents = false;

        //wait for event or fail by timeout
        while (!hasCandlestickEvents) {
            Thread.sleep(10);
            hasCandlestickEvents = queue.stream()
                    .anyMatch(CandlestickEvents.BinanceCandlestickEvent.class::isInstance);
        }

        //when
        eventPublisher.publishEvent(new CandlestickEvents.StopListenCandlesticksEvent(key));
        //fix queue size right after publish stop event
        int queueSize = queue.size();
        Thread.sleep(50);

        //then

        //queue size shouldn't change after stop listen event
        assertEquals(queueSize, queue.size());

        context.close();
    }

    @Test
    public void shouldRestartWhenGotStopEvent() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 20);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        BinanceApiClientFactory binanceApiClientFactory = context.getBean(BinanceApiClientFactory.class);

        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        eventPublisher.publishEvent(new CandlestickEvents.RestartListenCandlesticksEvent(key));
        Thread.sleep(100);

        //then
        //First time newWebSocketClient called on start event, second time on exception
        verify(binanceApiClientFactory, times(2)).newWebSocketClient();
    }

    @Test
    public void shouldRestartWhenGotException() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.send_binance_events", false);
        values.put("test.send_exception", true);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        BinanceApiClientFactory binanceApiClientFactory = context.getBean(BinanceApiClientFactory.class);

        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        Thread.sleep(100);

        //then
        //First time newWebSocketClient called on start event, second time on restart
        verify(binanceApiClientFactory, times(2)).newWebSocketClient();
    }

    @Test
    public void shouldCreateOnlyOneConnectionIfGotTwoStartEvents() throws InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.send_binance_events", true);
        values.put("test.send_exception_count", 1);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        BinanceApiClientFactory binanceApiClientFactory = context.getBean(BinanceApiClientFactory.class);

        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        Thread.sleep(100);

        //then
        verify(binanceApiClientFactory, times(1)).newWebSocketClient();
    }
}