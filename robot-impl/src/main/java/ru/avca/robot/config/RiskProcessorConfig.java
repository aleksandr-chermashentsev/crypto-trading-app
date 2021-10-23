package ru.avca.robot.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * @author a.chermashentsev
 * Date: 20.10.2021
 **/
@ConfigurationProperties("riskProcessor")
public class RiskProcessorConfig {
    private int maxNumberOfOpenPositions;
    private int maxNumberOfBuyMore;
    private double buyMorePercentOfOpenPosition;
    private double buyMoreThreshold;
    private double stopLoss;
    private double takeProfit;
    private double firstBuyPercent;

    public int getMaxNumberOfOpenPositions() {
        return maxNumberOfOpenPositions;
    }

    public void setMaxNumberOfOpenPositions(int maxNumberOfOpenPositions) {
        this.maxNumberOfOpenPositions = maxNumberOfOpenPositions;
    }

    public int getMaxNumberOfBuyMore() {
        return maxNumberOfBuyMore;
    }

    public void setMaxNumberOfBuyMore(int maxNumberOfBuyMore) {
        this.maxNumberOfBuyMore = maxNumberOfBuyMore;
    }

    public double getBuyMorePercentOfOpenPosition() {
        return buyMorePercentOfOpenPosition;
    }

    public void setBuyMorePercentOfOpenPosition(double buyMorePercentOfOpenPosition) {
        this.buyMorePercentOfOpenPosition = buyMorePercentOfOpenPosition;
    }

    public double getBuyMoreThreshold() {
        return buyMoreThreshold;
    }

    public void setBuyMoreThreshold(double buyMoreThreshold) {
        this.buyMoreThreshold = buyMoreThreshold;
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

    public double getFirstBuyPercent() {
        return firstBuyPercent;
    }

    public void setFirstBuyPercent(double firstBuyPercent) {
        this.firstBuyPercent = firstBuyPercent;
    }
}
