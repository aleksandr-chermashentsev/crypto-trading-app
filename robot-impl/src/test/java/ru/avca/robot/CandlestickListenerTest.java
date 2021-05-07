package ru.avca.robot;

import com.binance.api.client.BinanceApiClientFactory;
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
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
@MicronautTest
@Timeout(value = 3)
@Disabled
public class CandlestickListenerTest {
    private final CandlestickEvents.ListenerKey key = new CandlestickEvents.ListenerKey("test", CandlestickInterval.THREE_DAILY);

    @Test
    public void shouldSendCandlesticksWhenGotStartEvent() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.send_binance_events", true);
        values.put("test.send_binance_events_interval_ms", 20);
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        MessageListenerTestHelper messageListener = context.getBean(MessageListenerTestHelper.class);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        Queue queue = messageListener.getQueue();
        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));

        //wait for event or fail by timeout
        Future<CandlestickEvents.BinanceCandlestickEvent> eventFuture = messageListener.getEventFromQueue(CandlestickEvents.BinanceCandlestickEvent.class);
        assertNotNull(eventFuture.get(), "BinanceCandlestickEvent must preset");

        context.close();
    }

    @Test
    public void shouldStopWhenGotStopEvent() throws InterruptedException, ExecutionException {
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
        Future<CandlestickEvents.BinanceCandlestickEvent> eventFuture = messageListener.getEventFromQueue(CandlestickEvents.BinanceCandlestickEvent.class);
        assertNotNull(eventFuture.get(), "BinanceCandlestickEvent must preset");

        //when
        eventPublisher.publishEvent(new CandlestickEvents.StopListenCandlesticksEvent(key));
        Thread.sleep(300);
        queue.clear();

        eventFuture = messageListener.getEventFromQueue(CandlestickEvents.BinanceCandlestickEvent.class);
        try {
            eventFuture.get(2, TimeUnit.SECONDS);
            fail("There is should be no more BinanceCandlestickEvent but got one" + eventFuture.get());
        } catch (Exception e) {

        }

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