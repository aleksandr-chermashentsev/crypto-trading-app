package ru.avca.robot.config;

import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * @author a.chermashentsev
 * Date: 08.04.2021
 **/
@ConfigurationProperties("robot")
public class RobotConfig {
    private String usdCoin;
    private CandlestickInterval interval;
    private double stopLoss;
    private double takeProfit;
    private int coinsCount;

    private int initialBalance;

    public String getUsdCoin() {
        return usdCoin;
    }

    public void setUsdCoin(String usdCoin) {
        this.usdCoin = usdCoin;
    }

    public CandlestickInterval getInterval() {
        return interval;
    }

    public void setInterval(CandlestickInterval interval) {
        this.interval = interval;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public double getTakeProfit() {
        return takeProfit;
    }

    public void setTakeProfit(double takeProfit) {
        this.takeProfit = takeProfit;
    }

    public int getCoinsCount() {
        return coinsCount;
    }

    public void setCoinsCount(int coinsCount) {
        this.coinsCount = coinsCount;
    }

    public int getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(int initialBalance) {
        this.initialBalance = initialBalance;
    }
    @Override
    public String toString() {
        return "RobotConfig{" +
                "usdCoin='" + usdCoin + '\'' +
                ", interval=" + interval +
                ", stopLoss=" + stopLoss +
                ", takeProfit=" + takeProfit +
                ", coinsCount=" + coinsCount +
                ", initialBalance=" + initialBalance +
                '}';
    }
}
