package ru.avca.robot.event;

import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;
import lombok.Value;

/**
 * @author a.chermashentsev
 * Date: 01.04.2021
 **/
public class CandlestickEvents {
    @Value
    public static class BinanceCandlestickEvent {
        CandlestickEvent binanceEvent;
    }

    @Value
    public static class StartListenCandlesticksEvent {
        String symbol;
        CandlestickInterval interval;
    }

    @Value
    public static class StopListenCandlesticksEvent {
        String symbol;
        CandlestickInterval interval;
    }

    @Value
    public static class RestartListenCandlesticksEvent {
        String symbol;
        CandlestickInterval interval;
    }

}
