package ru.avca.robot.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * @author a.chermashentsev
 * Date: 19.10.2021
 **/
@ConfigurationProperties("athDivergenceSignalConfig")
public class AthDivergenceSignalConfig {
    private int periodLength;
    private double divergenceForBuy;

    public int getPeriodLength() {
        return periodLength;
    }

    public void setPeriodLength(int periodLength) {
        this.periodLength = periodLength;
    }

    public double getDivergenceForBuy() {
        return divergenceForBuy;
    }

    public void setDivergenceForBuy(double divergenceForBuy) {
        this.divergenceForBuy = divergenceForBuy;
    }
}
