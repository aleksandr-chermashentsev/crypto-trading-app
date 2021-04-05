package ru.avca.robot.factory;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
//import ru.avca.robot.CandlestickListener;

import javax.inject.Singleton;

/**
 * @author a.chermashentsev
 * Date: 30.03.2021
 **/
@Factory
public class BinanceFactory {

    @Singleton
    public BinanceApiClientFactory binanceApiClientFactory(
            @Value("${exchange.binance.api.key}") String apiKey,
            @Value("${exchange.binance.api.secret}") String apiSecret) {
        return BinanceApiClientFactory.newInstance(apiKey, apiSecret);
    }
}
