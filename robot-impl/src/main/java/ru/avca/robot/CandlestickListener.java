package ru.avca.robot;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import static ru.avca.robot.event.CandlestickEvents.*;

/**
 * @author a.chermashentsev
 * Date: 14.01.2021
 **/
@Singleton
public class CandlestickListener {
    private static final Logger LOG = LoggerFactory.getLogger(CandlestickListener.class);

    @Inject
    private ApplicationEventPublisher eventPublisher;
    @Inject
    private BinanceApiClientFactory clientFactory;
    private final ConcurrentMap<StartedListenerKey, Boolean> startedListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<StartedListenerKey, CountDownLatch> latches = new ConcurrentHashMap<>();

    @Async
    @EventListener
    void onStartListenEvent(StartListenCandlesticksEvent event) {
        startListenToCandlestick(event.getSymbol(), event.getInterval());
    }

    @Async
    @EventListener
    void onStopListenEvent(StopListenCandlesticksEvent event) {
        stopListenToCandlestick(event.getSymbol(), event.getInterval());
    }

    @Async
    @EventListener
    void onRestartEvent(RestartListenCandlesticksEvent event) {
        restartListener(event.getSymbol(), event.getInterval());
    }

    private void stopListenToCandlestick(String symbol, CandlestickInterval interval) {
        StartedListenerKey key = new StartedListenerKey(symbol, interval);
        startedListeners.remove(key);

        CountDownLatch countDownLatch = latches.get(key);
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
        LOG.info("Got stop listen event");
    }

    private void restartListener(String symbol, CandlestickInterval interval) {
        StartedListenerKey key = new StartedListenerKey(symbol, interval);

        CountDownLatch countDownLatch = latches.get(key);
        countDownLatch.countDown();
    }

    private void startListenToCandlestick(String symbol, CandlestickInterval interval) {
        StartedListenerKey key = new StartedListenerKey(symbol, interval);

        startedListeners.put(key, true);

        while (startedListeners.getOrDefault(key, false)) {
            CountDownLatch latch = new CountDownLatch(1);
            latches.put(key, latch);
            LOG.info("subscribe on candlestick events");
            Closeable connectionWebSocket = clientFactory.newWebSocketClient().onCandlestickEvent(symbol, interval, new BinanceApiCallback<>() {
                @Override
                public void onFailure(Throwable cause) {
                    latch.countDown();
                    LOG.error("Unlock client because got exception ", cause);
                }

                @Override
                public void onResponse(CandlestickEvent event) {
                    LOG.debug("Got new event {} ", event);
                    eventPublisher.publishEventAsync(new BinanceCandlestickEvent(event));
                }
            });

            try {
                //This latch can be unlocked by stop event, failure or if too much time passed without updates
                latch.await();
                connectionWebSocket.close();
            } catch (InterruptedException | IOException e) {
                LOG.error("Exception on latch ", e);
            }
        }
    }

    @lombok.Value
    private static class StartedListenerKey {
        String symbol;
        CandlestickInterval interval;
    }

}
