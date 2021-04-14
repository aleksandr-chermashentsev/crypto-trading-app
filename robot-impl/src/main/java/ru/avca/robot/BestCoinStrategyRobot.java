package ru.avca.robot;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.general.SymbolStatus;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.exception.BinanceApiException;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;
import ru.avca.robot.utils.TimeUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.avca.robot.utils.TimeUtils.getCurrentTimeUtc;

/**
 * @author a.chermashentsev
 * Date: 08.04.2021
 **/
@Context
public class BestCoinStrategyRobot {
    private static final Logger LOG = LoggerFactory.getLogger(BestCoinStrategyRobot.class);
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    @Value("${robot.minDelayBetweenOrderExecutesMs:5000}")
    private long minDelayBetweenOrderExecutesMs;
    @Inject private ApplicationEventPublisher publisher;
    @Inject private BinanceApiClientFactory binanceApiClientFactory;
    @Inject @Named(TaskExecutors.SCHEDULED) private TaskScheduler taskScheduler;

    private final AtomicLong lastTimeTryExecuteOrderCalled = new AtomicLong();
    private final ConcurrentMap<String, CandlestickEvent> candles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BigDecimal> currentOpenPositionBalance = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BigDecimal> currentOpenPositionPrice = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> stepSizes = new HashMap<>();
    private final ConcurrentMap<String, Boolean> alreadySubscribedSymbols = new ConcurrentHashMap<>();
    private volatile BigDecimal currentUsdtBalance;
    private volatile RobotEvents.RobotStartEvent startEvent;

    @EventListener
    @Async
    public void onRobotStartEvent(RobotEvents.RobotStartEvent event) {
        boolean isAlreadyStarted = isStarted.getAndSet(true);
        startEvent = event;
        LOG.info("Got event {}", event);
        if (isAlreadyStarted) {
            LOG.info("Skip event {} because robot is already in started state", event);
            return;
        }
        currentUsdtBalance = event.getInitialUsdtBalance();

        loadSymbols(event.getUsdCoin()).forEach(symbol -> alreadySubscribedSymbols.put(symbol, true));
        updateStepSizes(event);

        subscribeOnSymbols(alreadySubscribedSymbols.keySet(), event.getInterval());

        Duration initialDelay = Duration.ofMillis(event.getNextTimeOrderExecute() - getCurrentTimeUtc());
        Duration interval = Duration.ofMillis(event.getOrderExecuteInterval());
        taskScheduler.scheduleAtFixedRate(initialDelay, interval, () ->
                publisher.publishEventAsync(new RobotEvents.TryExecuteOrderEvent())
        );
    }

    private void subscribeOnSymbols(Set<String> symbols, CandlestickInterval interval) {
        String subscribeString = symbols.stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining(","));
        LOG.info("{} symbols loaded. Send subscribe event with subscribeString {}", symbols.size(), subscribeString);

        publisher.publishEventAsync(new CandlestickEvents.StartListenCandlesticksEvent(
                new CandlestickEvents.ListenerKey(
                        subscribeString, interval
                )
        ));
    }

    private void updateStepSizes(RobotEvents.RobotStartEvent event) {
        binanceApiClientFactory.newRestClient().getExchangeInfo().getSymbols().stream()
                .filter(symbolInfo -> event.getUsdCoin().equals(symbolInfo.getQuoteAsset()))
                .filter(symbolInfo -> symbolInfo.getStatus() == SymbolStatus.TRADING)
                .forEach(symbolInfo -> symbolInfo.getFilters().stream()
                        .filter(a -> a.getFilterType() == FilterType.LOT_SIZE)
                        .forEach(symbolFilter -> stepSizes.put(symbolInfo.getSymbol(), new BigDecimal(symbolFilter.getStepSize())))
                );
    }

    @EventListener
    @Async
    public void onBinanceEvent(CandlestickEvents.BinanceCandlestickEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Robot got binance candlestick event {}", event);
        }
        if (startEvent == null) {
            return;
        }
        if (event.getKey().getInterval() == startEvent.getInterval() && alreadySubscribedSymbols.containsKey(event.getBinanceEvent().getSymbol())) {
            candles.put(event.getBinanceEvent().getSymbol(), event.getBinanceEvent());
            if (!currentOpenPositionPrice.isEmpty()) {
                BigDecimal price = currentOpenPositionPrice.get(event.getBinanceEvent().getSymbol());
                if (price != null) {
                    checkTpAndSl(event, price);
                }
            }
        }
    }

    private void checkTpAndSl(CandlestickEvents.BinanceCandlestickEvent event, BigDecimal price) {
        CandlestickEvent binanceEvent = event.getBinanceEvent();
        double slValue = price.multiply(startEvent.getStopLoss()).doubleValue();

        if (slValue > Double.parseDouble(binanceEvent.getLow())) {
            LOG.info("STOP LOSS!!!!!!!!!!!!!!!!!!!!!!!! candlestick.low {}, stoplossValue {}", binanceEvent.getLow(), slValue);
            closePosition();
        }
        else {
            double tpValue = price.multiply(startEvent.getTakeProfit()).doubleValue();
            if (tpValue < Double.parseDouble(binanceEvent.getClose())) {
                LOG.info("Take profit! candlestick.close {}, tpValue {}", binanceEvent.getClose(), tpValue);
                closePosition();
            }
        }
    }

    @EventListener
    @Async
    public void onTryExecuteOrder(RobotEvents.TryExecuteOrderEvent event) {
        LOG.info("Robot got event {}", event);

        BigDecimal currentUsdtBalanceLocal = currentUsdtBalance;
        RobotEvents.RobotStartEvent startEvent = this.startEvent;
        long lastTimeCalled = lastTimeTryExecuteOrderCalled.get();
        long currentTimeUtc = getCurrentTimeUtc();
        if (lastTimeCalled + minDelayBetweenOrderExecutesMs >= currentTimeUtc || !lastTimeTryExecuteOrderCalled.compareAndSet(lastTimeCalled, currentTimeUtc)) {
            LOG.info("Concurrent calls for TryExecuteOrder lastTimeCalled {}, currentUtc {} ", lastTimeCalled, currentTimeUtc);
            return;
        }

        if (!currentOpenPositionBalance.isEmpty()) {
            closePosition();
        }

        long currentTimeMillis = TimeUtils.getCurrentTimeUtc();
        List<Map.Entry<String, CandlestickEvent>> bestCoinsByVolume = candles.entrySet().stream()
                .filter(pair -> Math.abs(currentTimeMillis - pair.getValue().getEventTime()) < 60_000) //use only candle updated last 60 sec
                .filter(pair -> new BigDecimal(pair.getValue().getQuoteAssetVolume()).doubleValue() > currentUsdtBalanceLocal.doubleValue() * 100) //use candle only if current balance is 1% of the volume of this candle
                .collect(Collectors.toList());

        LOG.info("Try find best coins in {} filtered coins", bestCoinsByVolume.size());
        List<Map.Entry<String, CandlestickEvent>> bestCoins = bestCoinsByVolume.stream()
                .sorted(Collections.reverseOrder(Comparator.comparing(pair -> new BigDecimal(pair.getValue().getClose()).divide(new BigDecimal(pair.getValue().getOpen()), 10, RoundingMode.CEILING))))
                .filter(pair -> new BigDecimal(pair.getValue().getClose()).compareTo(new BigDecimal(pair.getValue().getOpen())) > 0)
                .limit(startEvent.getCoinsCount())
                .collect(Collectors.toList());

        if (bestCoins.size() < startEvent.getCoinsCount()) {

            LOG.info("Not enough best coins {}", bestCoins.size());
            return;
        }

        bestCoins.forEach(bestCoin -> openPosition(bestCoin, currentUsdtBalance.divide(new BigDecimal(startEvent.getCoinsCount()), 15, RoundingMode.CEILING)));

        Set<String> newSymbols = loadSymbols(startEvent.getUsdCoin())
                .filter(symbol -> !alreadySubscribedSymbols.containsKey(symbol))
                .peek(symbol -> alreadySubscribedSymbols.put(symbol, true))
                .collect(Collectors.toSet());
        if (newSymbols.size() > 0) {
            LOG.info("Subscribe on new symbols {}", newSymbols);
            subscribeOnSymbols(newSymbols, startEvent.getInterval());
        }
    }

    private void closePosition() {
        LOG.info("Close position in {}", currentOpenPositionBalance.keySet());

        Map<String, BigDecimal> expectedQuantities = currentOpenPositionBalance.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().remainder(stepSizes.get(entry.getKey())).negate().add(entry.getValue()))
                );
        LOG.info("Try to close {}", expectedQuantities);
        expectedQuantities.forEach((symbol, expectedQty) -> {
            NewOrder newOrder = new NewOrder(
                    symbol,
                    OrderSide.SELL,
                    OrderType.MARKET,
                    null,
                    expectedQty.stripTrailingZeros().toPlainString()
            );
            currentOpenPositionBalance.remove(symbol);
            NewOrderResponse response = binanceApiClientFactory.newRestClient().newOrder(newOrder);
            LOG.info("Close position response {}", response);
            currentUsdtBalance = new BigDecimal(response.getCummulativeQuoteQty());
            publisher.publishEvent(new RobotEvents.SellEvent(symbol, currentUsdtBalance));
            LOG.info("Current usdt balance {}", currentUsdtBalance);
        });

        currentOpenPositionBalance.clear();
    }

    private void openPosition(Map.Entry<String, CandlestickEvent> candleBySymbol, BigDecimal usdtBalance) {
        String symbol = candleBySymbol.getKey();
        NewOrder order = new NewOrder(
                symbol,
                OrderSide.BUY,
                OrderType.MARKET,
                null,
                null
        );
        order.quoteOrderQty(usdtBalance.stripTrailingZeros().toPlainString());
        try {
            NewOrderResponse newOrderResponse = binanceApiClientFactory.newRestClient().newOrder(order);
            LOG.info("Open position response {}", newOrderResponse);

            BigDecimal executedQty = new BigDecimal(newOrderResponse.getExecutedQty());
            currentOpenPositionBalance.put(symbol, executedQty);
            BigDecimal executedPrice = new BigDecimal(newOrderResponse.getCummulativeQuoteQty()).divide(executedQty, 15, RoundingMode.CEILING);
            LOG.info("Executed price for {} is {}", symbol, executedPrice);
            currentOpenPositionPrice.put(symbol, executedPrice);

            publisher.publishEventAsync(new RobotEvents.BuyEvent(symbol));
            LOG.info("Open position {} {}", symbol, currentOpenPositionBalance);
            candles.clear();

        } catch (Exception e) {
            if (e instanceof BinanceApiException) {
                LOG.error("Code error {}", ((BinanceApiException)e).getError().getCode());
            }
            LOG.error("Can't open position. Rejected order {}", order, e);
        }
    }

    private Stream<String> loadSymbols(String usdCoin) {
        return binanceApiClientFactory.newRestClient().getExchangeInfo().getSymbols().stream()
                .filter(symbolInfo -> usdCoin.equals(symbolInfo.getQuoteAsset()))
                .filter(symbolInfo -> symbolInfo.getStatus() == SymbolStatus.TRADING)
                .map(SymbolInfo::getSymbol);
    }

}
