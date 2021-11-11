package ru.avca.robot.event;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;
import lombok.Value;

import java.math.BigDecimal;

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

    @Value
    public static class SignalEvent {
        String symbol;
        OrderSide orderSide;
        double athDivergencePrice;
    }

    @Value
    public static class ExecutionEvent {
        OrderSide side;
        String symbol;
        BigDecimal baseQty;
        BigDecimal quoteQty;
        BigDecimal price;
    }

    @Value
    public static class MarketOrderEvent {
        OrderSide side;
        String symbol;
        BigDecimal quantity;
    }
}
