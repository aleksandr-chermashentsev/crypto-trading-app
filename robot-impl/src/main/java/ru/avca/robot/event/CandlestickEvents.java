package ru.avca.robot.event;

import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;
import lombok.Value;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
public class CandlestickEvents {
    private CandlestickEvents(){}
    @Value
    public static class ListenerKey {
        String symbols;
        CandlestickInterval interval;
    }
    @Value
    public static class BinanceCandlestickEvent {
        CandlestickEvent binanceEvent;
        ListenerKey key;
    }

    @Value
    public static class StartListenCandlesticksEvent {
        ListenerKey key;
    }

    @Value
    public static class StopListenCandlesticksEvent {
        ListenerKey key;
    }

    @Value
    public static class RestartListenCandlesticksEvent {
        ListenerKey key;
    }

    @Value
    public static class ListenerHasStartedListenToCandlestickEvent {
        ListenerKey key;
    }

    @Value
    public static class ListenerHasStoppedListenToCandlestickEvent {
        ListenerKey key;
    }
}
