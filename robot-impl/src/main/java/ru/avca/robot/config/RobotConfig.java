package ru.avca.robot.config;

import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author a.chermashentsev
 * Date: 08.04.2021
 **/
@ConfigurationProperties("robot")
@Setter
@Getter
@ToString
public class RobotConfig {
    private String usdCoin;
    private CandlestickInterval interval;
    private double stopLoss;
    private double takeProfit;
    private int coinsCount;
}
