package ru.avca.robot;

import com.binance.api.client.BinanceApiClientFactory;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
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
    public BinanceApiClientFactory binanceApiClientFactory() {
        BinanceApiClientFactory mock = spy(BinanceApiClientFactory.newInstance());
        when(mock.newWebSocketClient()).thenReturn(new CandlestickListenerTest.BinanceApiWebSocketClientMock());
        return mock;
    }
}
