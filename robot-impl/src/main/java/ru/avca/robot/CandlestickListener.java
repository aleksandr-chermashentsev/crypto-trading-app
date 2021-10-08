package ru.avca.robot;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.event.CandlestickEvent;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.event.CandlestickEvents;

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
    private final Object monitorObj = new Object();

    @Inject
    private ApplicationEventPublisher eventPublisher;
    @Inject
    private BinanceApiClientFactory clientFactory;
    private final ConcurrentMap<ListenerKey, Boolean> startedListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<ListenerKey, CountDownLatch> latches = new ConcurrentHashMap<>();

    @Async
    @EventListener
    void onStartListenEvent(StartListenCandlesticksEvent event) {
        ListenerKey key = event.getKey();
        startListenToCandlestick(key);
    }

    @Async
    @EventListener
    void onStopListenEvent(StopListenCandlesticksEvent event) {
        stopListenToCandlestick(event.getKey());
    }

    @Async
    @EventListener
    void onRestartEvent(RestartListenCandlesticksEvent event) {
        restartListener(event.getKey());
    }

    private void stopListenToCandlestick(ListenerKey key) {
        startedListeners.remove(key);

        CountDownLatch countDownLatch = latches.get(key);
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
        LOG.info("Got stop listen event");
    }

    private void restartListener(ListenerKey key) {
        stopListenToCandlestick(key);
        startListenToCandlestick(key);
    }

    private void startListenToCandlestick(ListenerKey key) {

        synchronized (monitorObj) {
            Boolean isStarted = startedListeners.getOrDefault(key, false);
            if (isStarted) {
                LOG.info("Listener is already started for key {}. Ignore start event", key);
                return;
            }
            startedListeners.put(key, true);
        }

        while (startedListeners.getOrDefault(key, false)) {
            CountDownLatch latch = new CountDownLatch(1);
            latches.put(key, latch);
            LOG.info("subscribe on candlestick events");
            Closeable connectionWebSocket = clientFactory.newWebSocketClient().onCandlestickEvent(key.getSymbols(), key.getInterval(), new BinanceApiCallback<>() {
                @Override
                public void onFailure(Throwable cause) {
                    latch.countDown();
                    LOG.error("Unlock client because got exception ", cause);
                }

                @Override
                public void onResponse(CandlestickEvent event) {
                    if (LOG.isDebugEnabled()) {
                        LOG.info("Got new event {} ", event);
                    }
                    eventPublisher.publishEventAsync(new BinanceCandlestickEvent(event, key));
                }
            });

            try {
                eventPublisher.publishEvent(new CandlestickEvents.ListenerHasStartedListenToCandlestickEvent(key));
                //This latch can be unlocked by stop event, failure or if too much time passed without updates
                latch.await();
                connectionWebSocket.close();
                eventPublisher.publishEvent(new ListenerHasStoppedListenToCandlestickEvent(key));
            } catch (InterruptedException | IOException e) {
                LOG.error("Exception on latch ", e);
            }
        }
    }
}
