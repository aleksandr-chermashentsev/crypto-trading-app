package ru.avca.robot;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.factory.BinanceFactory;

import javax.inject.Singleton;

import static org.mockito.Mockito.*;

/**
 * @author a.chermashentsev
 * Date: 05.04.2021
 **/
@Factory
public class TestFactory {

    @Singleton
    @Replaces(value = BinanceApiClientFactory.class, factory = BinanceFactory.class)
    public BinanceApiClientFactory binanceApiClientFactory(
            @Value("${test.send_binance_events:false}") boolean sendBinanceEvent,
            @Value("${test.send_exception:false}") boolean sendException,
            @Value("${test.send_binance_events_interval_ms:100}") int sendEventsIntervalMs,
            @Value("${test.send_exception_count:1}") int sendExceptionCount
    ) {
        BinanceApiClientFactory mock = spy(BinanceApiClientFactory.newInstance());
        if (sendBinanceEvent || sendException) {
            when(mock.newWebSocketClient()).thenReturn(new BinanceApiWebSocketClientMock(sendEventsIntervalMs, sendBinanceEvent, sendExceptionCount, sendException));
        }
        else {
            BinanceApiWebSocketClient clientMock = mock(BinanceApiWebSocketClient.class);
            when(clientMock.onCandlestickEvent(anyString(), any(CandlestickInterval.class), any(BinanceApiCallback.class)))
                    .thenReturn(() -> {});
            when(mock.newWebSocketClient()).thenReturn(clientMock);
        }
        return mock;
    }
}
