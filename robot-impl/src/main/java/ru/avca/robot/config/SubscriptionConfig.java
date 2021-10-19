package ru.avca.robot.config;

import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * @author a.chermashentsev
 * Date: 19.10.2021
 **/
@ConfigurationProperties("subscription")
public class SubscriptionConfig {
    private String quoteFilter;
    private List<CandlestickInterval> candlestickIntervals;
    private Duration updateInterval;

    public String getQuoteFilter() {
        return quoteFilter;
    }

    public void setQuoteFilter(String quoteFilter) {
        this.quoteFilter = quoteFilter;
    }

    public List<CandlestickInterval> getCandlestickIntervals() {
        return candlestickIntervals;
    }

    public void setCandlestickIntervals(List<CandlestickInterval> candlestickIntervals) {
        this.candlestickIntervals = candlestickIntervals;
    }

    public Duration getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateIntervalSec(int updateInterval) {
        this.updateInterval = Duration.ofSeconds(updateInterval);
    }
}
