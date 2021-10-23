package ru.avca.robot.factory;

import com.binance.api.client.BinanceApiClientFactory;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import ru.avca.robot.utils.RobotUtils;

import javax.inject.Inject;
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
        return BinanceApiClientFactory.newInstance(apiKey.trim(), apiSecret.trim());
    }

    @Singleton
    @Inject
    public RobotUtils robotUtils(BinanceApiClientFactory binanceApiClientFactory) {
        return new RobotUtils(binanceApiClientFactory);
    }
}
