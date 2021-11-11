package ru.avca.robot.athdevergence;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.event.CandlestickEvent;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.config.AthDivergenceSignalConfig;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;
import ru.avca.robot.utils.RobotUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author a.chermashentsev
 * Date: 19.10.2021
 **/
@Context
public class ATHValuesSignalProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ATHValuesSignalProcessor.class);

    @Inject private AthDivergenceSignalConfig config;
    @Inject private RobotUtils robotUtils;
    @Inject private ApplicationEventPublisher eventPublisher;
    private final ConcurrentMap<String, CircularFifoQueue<BigDecimal>> candlesticksHighPrices = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BigDecimal> candlesticksCurrentAthValues = new ConcurrentHashMap<>();
    private volatile long lastSendTime;

    @EventListener
    @Async
    public void onCandlestickEvent(CandlestickEvents.BinanceCandlestickEvent event) {
        CandlestickEvent binanceEvent = event.getBinanceEvent();
        String symbol = binanceEvent.getSymbol();
        BigDecimal lastEventHighPrice = new BigDecimal(binanceEvent.getHigh());
        if (!candlesticksHighPrices.containsKey(symbol)) {
            Instant relativeTime = Instant.now().minus(config.getPeriodLength() * (robotUtils.timeInMillis(event.getKey().getInterval())), ChronoUnit.MILLIS);
            CircularFifoQueue<BigDecimal> historyValues = robotUtils.loadHistory(symbol, event.getKey().getInterval(), relativeTime.toEpochMilli(), Instant.now().toEpochMilli())
                    .collect(
                            () -> new CircularFifoQueue<>(config.getPeriodLength()),
                            (queue, candle) -> queue.add(new BigDecimal(candle.getHigh())),
                            AbstractCollection::addAll
                    );
            candlesticksHighPrices.put(symbol, historyValues);
            LOG.info("History values for {} was loaded. Size {}", symbol, historyValues.size());
            if (historyValues.size() >= config.getPeriodLength()) {
                BigDecimal athValue = calculateHighPrice(symbol, historyValues);
                candlesticksCurrentAthValues.put(symbol, athValue);
            }
        }
        BigDecimal currentAthValue = candlesticksCurrentAthValues.get(symbol);
        if (binanceEvent.getBarFinal()) {
            BigDecimal athValue = currentAthValue;
            CircularFifoQueue<BigDecimal> highPrices = candlesticksHighPrices.get(symbol);
            if (highPrices.size() >= config.getPeriodLength()) {
                BigDecimal lastHighPrice = highPrices.peek();
                highPrices.add(lastEventHighPrice);
                if ((lastHighPrice != null && lastHighPrice.equals(athValue)) || currentAthValue == null) {
                    BigDecimal newHighPrice = calculateHighPrice(symbol, highPrices);
                    candlesticksCurrentAthValues.put(symbol, newHighPrice);
                }
                else {
                    BigDecimal prevAthValue = currentAthValue;
                    if (prevAthValue.doubleValue() < lastEventHighPrice.doubleValue()) {
                        candlesticksCurrentAthValues.put(symbol, lastEventHighPrice);
                    }
                }
            }
            else {
                highPrices.add(lastEventHighPrice);
            }
        }
        else if (currentAthValue != null) {
            BigDecimal closePrice = new BigDecimal(binanceEvent.getClose());

            double athDivergencePrice = currentAthValue.doubleValue() * config.getDivergenceForBuy();
            if (closePrice.doubleValue() <= athDivergencePrice) {
                eventPublisher.publishEventAsync(new CandlestickEvents.SignalEvent(symbol, OrderSide.BUY, athDivergencePrice));
            }
            eventPublisher.publishEventAsync(new RobotEvents.CloseToAthValues(
                    symbol,
                    closePrice.doubleValue() / currentAthValue.doubleValue()
            ));
        }
    }

    private BigDecimal calculateHighPrice(String symbol, CircularFifoQueue<BigDecimal> highPrices) {
        return highPrices.stream().max(Comparator.comparingDouble(BigDecimal::doubleValue)).orElseThrow(() -> new RuntimeException("Unexpected empty queue for symbol " + symbol));
    }
}
