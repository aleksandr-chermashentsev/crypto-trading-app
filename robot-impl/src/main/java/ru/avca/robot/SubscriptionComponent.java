package ru.avca.robot;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.general.SymbolStatus;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.config.SubscriptionConfig;
import ru.avca.robot.event.CandlestickEvents;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author a.chermashentsev
 * Date: 19.10.2021
 **/
@Context
public class SubscriptionComponent {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionComponent.class);
    @Inject private SubscriptionConfig subscriptionConfig;
    @Inject private BinanceApiClientFactory binanceApiClientFactory;
    @Inject private ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> subscribedSymbols = new HashSet<>();
    private final List<CandlestickEvents.ListenerKey> listenerKeys = new ArrayList<>();

    @EventListener
    @Async
    public void onStart(StartupEvent event) {
        String subscribeString = getSymbolsStream()
                .map(String::toLowerCase)
                .peek(subscribedSymbols::add)
                .collect(Collectors.joining(","));
        LOG.info("Send subscribe string {}", subscribeString);

        subscribe(subscribeString);

        scheduledExecutorService.scheduleAtFixedRate(this::onUpdate,
                subscriptionConfig.getUpdateInterval().getSeconds(),
                subscriptionConfig.getUpdateInterval().getSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void subscribe(String subscribeString) {
        subscriptionConfig.getCandlestickIntervals().forEach(interval -> {
            CandlestickEvents.ListenerKey key = new CandlestickEvents.ListenerKey(subscribeString, interval);
            listenerKeys.add(key);
            eventPublisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(key));
        });
    }

    private void onUpdate() {
        Set<String> newSymbols = getSymbolsStream().collect(Collectors.toSet());
        if (!subscribedSymbols.equals(newSymbols)) {
            listenerKeys.forEach(listenerKey -> eventPublisher.publishEvent(new CandlestickEvents.StopListenCandlesticksEvent(listenerKey)));
            listenerKeys.clear();
            String subscribeString = String.join(",", newSymbols);
            subscribe(subscribeString);
        }
    }

    @NotNull
    private Stream<String> getSymbolsStream() {
        return binanceApiClientFactory.newRestClient().getExchangeInfo().getSymbols().stream()
                .filter(symbolInfo -> subscriptionConfig.getQuoteFilter().equals(symbolInfo.getQuoteAsset()))
                .filter(symbolInfo -> subscriptionConfig.getBaseFilter().isEmpty() || subscriptionConfig.getBaseFilter().contains(symbolInfo.getBaseAsset().toUpperCase()))
                .filter(symbolInfo -> symbolInfo.getStatus() == SymbolStatus.TRADING)
                .map(SymbolInfo::getSymbol);
    }
}
