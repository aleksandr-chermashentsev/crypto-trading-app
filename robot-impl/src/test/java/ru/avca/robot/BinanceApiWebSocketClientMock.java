package ru.avca.robot;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.*;
import com.binance.api.client.domain.market.CandlestickInterval;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author a.chermashentsev
 * Date: 07.04.2021
 **/
public class BinanceApiWebSocketClientMock implements BinanceApiWebSocketClient {

    private final boolean sendBinanceEvent;
    private final boolean sendException;
    private final int sendEventsIntervalMs;
    private int sendExceptionCount;
    private volatile boolean isStopped = false;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    BinanceApiWebSocketClientMock(int sendEventsIntervalMs, boolean sendBinanceEvent, int sendExceptionCount, boolean sendException) {
        this.sendEventsIntervalMs = sendEventsIntervalMs;
        this.sendBinanceEvent = sendBinanceEvent;
        this.sendException = sendException;
        this.sendExceptionCount = sendExceptionCount;
    }

    @Override
    public Closeable onDepthEvent(String symbols, BinanceApiCallback<DepthEvent> callback) {
        return null;
    }

    @Override
    public Closeable onCandlestickEvent(String symbols, CandlestickInterval interval, BinanceApiCallback<CandlestickEvent> callback) {
        isStopped = false;
        if (sendBinanceEvent) {
            scheduledExecutorService.scheduleAtFixedRate(
                    () -> {
                        if (!isStopped) {
                            callback.onResponse(new CandlestickEvent());
                        }
                    },
                    0,
                    sendEventsIntervalMs,
                    TimeUnit.MILLISECONDS
            );
        } else if (sendException && sendExceptionCount > 0) {
            sendExceptionCount--;
            callback.onFailure(new IllegalStateException());
        }
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
