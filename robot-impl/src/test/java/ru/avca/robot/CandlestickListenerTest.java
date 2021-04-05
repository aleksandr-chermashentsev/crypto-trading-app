package ru.avca.robot;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.*;
import com.binance.api.client.domain.market.CandlestickInterval;
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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
@MicronautTest
public class CandlestickListenerTest {
    private final String symbol = "test";
    private final CandlestickInterval interval = CandlestickInterval.THREE_DAILY;
    @Inject ApplicationEventPublisher eventPublisher;

    @Inject private MessageListenerTestHelper messageListener;
    @Inject private BinanceApiClientFactory binanceApiClientFactory;

    @BeforeEach
    public void stopListener() {
        eventPublisher.publishEvent(new CandlestickEvents.StopListenCandlesticksEvent(symbol, interval));
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void shouldSendCandlesticksWhenGotStartEvent() throws InterruptedException {
        Queue queue = messageListener.getQueue();
        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(symbol, interval));
        while (queue.isEmpty()) {
            Thread.sleep(10);
        }

        //then
        assertEquals(CandlestickEvents.BinanceCandlestickEvent.class, queue.poll().getClass());
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void shouldStopWhenGotStopEvent() throws InterruptedException {
        Queue queue = messageListener.getQueue();
        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(symbol, interval));

        while (queue.isEmpty()) {
            Thread.sleep(10);
        }
        //then
        assertEquals(CandlestickEvents.BinanceCandlestickEvent.class, queue.poll().getClass());

        //when
        eventPublisher.publishEvent(new CandlestickEvents.StopListenCandlesticksEvent(symbol, interval));
        int queueSize = queue.size();
        Thread.sleep(50);

        //then

        //queue size shouldn't change after stop listen event
        assertEquals(queueSize, queue.size());
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void shouldRestartWhenGotStopEvent() throws InterruptedException {
        //when
        eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(symbol, interval));
        eventPublisher.publishEvent(new CandlestickEvents.RestartListenCandlesticksEvent(symbol, interval));
        Thread.sleep(100);

        //then
        //First time newWebSocketClient called on start event, second time on restart
        verify(binanceApiClientFactory, times(2)).newWebSocketClient();
    }


    @Replaces(BinanceApiWebSocketClient.class)
    @Singleton
    static class BinanceApiWebSocketClientMock implements BinanceApiWebSocketClient{

        private volatile boolean isStopped = false;
        @Override
        public Closeable onDepthEvent(String symbols, BinanceApiCallback<DepthEvent> callback) {
            return null;
        }

        @Override
        public Closeable onCandlestickEvent(String symbols, CandlestickInterval interval, BinanceApiCallback<CandlestickEvent> callback) {
            isStopped = false;
            Thread thread = new Thread() {
                @SneakyThrows
                @Override
                public void run() {
                    while (!isStopped) {
                        Thread.sleep(10);
                        if (!isStopped) {
                            callback.onResponse(new CandlestickEvent());
                        }
                    }
                }
            };
            thread.start();
            return () -> {isStopped = true;};
        }

        @Override
        public Closeable onAggTradeEvent(String symbols, BinanceApiCallback<AggTradeEvent> callback) {
            return null;
        }

        @Override
        public Closeable onUserDataUpdateEvent(String listenKey, BinanceApiCallback<UserDataUpdateEvent> callback) {
            return null;
        }

        @Override
        public Closeable onTickerEvent(String symbols, BinanceApiCallback<TickerEvent> callback) {
            return null;
        }

        @Override
        public Closeable onAllMarketTickersEvent(BinanceApiCallback<List<TickerEvent>> callback) {
            return null;
        }

        @Override
        public Closeable onBookTickerEvent(String symbols, BinanceApiCallback<BookTickerEvent> callback) {
            return null;
        }

        @Override
        public Closeable onAllBookTickersEvent(BinanceApiCallback<BookTickerEvent> callback) {
            return null;
        }

        @Override
        public void close() {

        }
    }

}