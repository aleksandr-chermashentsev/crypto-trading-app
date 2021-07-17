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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;
import ru.avca.robot.grpc.RobotStateService;
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
    @Inject private RobotStateService robotStateService;

    private final AtomicLong lastTimeTryExecuteOrderCalled = new AtomicLong();
    private final ConcurrentMap<String, CandlestickEvent> candles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, OpenPositionInfo> openPositionInfosBySymbol = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> stepSizes = new HashMap<>();
    private final ConcurrentMap<String, Boolean> alreadySubscribedSymbols = new ConcurrentHashMap<>();
    private volatile BigDecimal currentUsdtBalance;
    private volatile BigDecimal newUsdtBalance;
    private volatile RobotEvents.RobotStartEvent startEvent;

    @EventListener
    @Async
    public void onRobotStartEvent(RobotEvents.RobotStartEvent event) {
        boolean isAlreadyStarted = isStarted.getAndSet(true);
        if (isAlreadyStarted) {
            LOG.info("Skip event {} because robot is already in started state", event);
            return;
        }
        startEvent = event;
        LOG.info("Got event {}", event);
        currentUsdtBalance = event.getInitialUsdtBalance();
        event.getOpenPositionInfosBySymbol().forEach(openPositionInfosBySymbol::put);

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
            if (!openPositionInfosBySymbol.isEmpty()) {
                OpenPositionInfo info = openPositionInfosBySymbol.get(event.getBinanceEvent().getSymbol());
                if (info != null) {
                    checkTpAndSl(event, info.getPrice());
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

        if (!openPositionInfosBySymbol.isEmpty()) {
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

        robotStateService.saveOpenPositions(openPositionInfosBySymbol.values().stream());
        Set<String> newSymbols = loadSymbols(startEvent.getUsdCoin())
                .filter(symbol -> !alreadySubscribedSymbols.containsKey(symbol))
                .peek(symbol -> alreadySubscribedSymbols.put(symbol, true))
                .collect(Collectors.toSet());
        if (newSymbols.size() > 0) {
            LOG.info("Subscribe on new symbols {}", newSymbols);
            subscribeOnSymbols(newSymbols, startEvent.getInterval());
            updateStepSizes(startEvent);
        }
    }

    public Map<String, Boolean> closePosition() {
        LOG.info("Close position in {}", openPositionInfosBySymbol.keySet());

        Map<String, BigDecimal> expectedQuantities = openPositionInfosBySymbol.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getBalance().remainder(stepSizes.get(entry.getKey())).negate().add(entry.getValue().getBalance()))
                );
        LOG.info("Try to close {}", expectedQuantities);
        HashMap<String, Boolean> result = new HashMap<>();
        try {
            expectedQuantities.forEach((symbol, expectedQty) -> {
                NewOrder newOrder = new NewOrder(
                        symbol,
                        OrderSide.SELL,
                        OrderType.MARKET,
                        null,
                        expectedQty.stripTrailingZeros().toPlainString()
                );
                openPositionInfosBySymbol.remove(symbol);
                NewOrderResponse response = binanceApiClientFactory.newRestClient().newOrder(newOrder);
                LOG.error("Close position response {}", response);
                currentUsdtBalance = new BigDecimal(response.getCummulativeQuoteQty());
                if (newUsdtBalance != null) {
                    currentUsdtBalance = newUsdtBalance;
                    newUsdtBalance = null;
                }
                publisher.publishEvent(new RobotEvents.SellEvent(symbol, currentUsdtBalance));
                LOG.info("Current usdt balance {}", currentUsdtBalance);
                result.put(symbol, true);
            });
        } catch (Exception e) {
            LOG.error("Exception on close position", e);
        }

        openPositionInfosBySymbol.clear();
        robotStateService.saveOpenPositions(Stream.empty());
        robotStateService.saveCurrencyBalance("USDT", currentUsdtBalance);
        return result;
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
            BigDecimal quoteQty = new BigDecimal(newOrderResponse.getCummulativeQuoteQty());
            BigDecimal executedPrice = quoteQty.divide(executedQty, 15, RoundingMode.CEILING);
            OpenPositionInfo info = new OpenPositionInfo(symbol, executedQty, executedPrice);
            openPositionInfosBySymbol.put(symbol, info);
            LOG.info("Executed price for {} is {}", symbol, executedPrice);

            publisher.publishEventAsync(new RobotEvents.BuyEvent(
                    executedQty,
                    quoteQty,
                    new BigDecimal(candleBySymbol.getValue().getClose()),
                    symbol
            ));
            LOG.info("Open position {} {}", symbol, info);
            candles.clear();

        } catch (Exception e) {
            if (e instanceof BinanceApiException) {
                LOG.error("Code error {}", ((BinanceApiException) e).getError().getCode());
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

    public void setUsdBalance(BigDecimal newBalance) {
        if (openPositionInfosBySymbol.isEmpty()) {
            currentUsdtBalance = newBalance;
        }
        else {
            newUsdtBalance = newBalance;
        }
        LOG.info("Usdt balance updated. {}", newBalance);
    }

    public Pair<BigDecimal, BigDecimal> getProfitInfo() {
        return Pair.of(
                currentUsdtBalance,
                openPositionInfosBySymbol.entrySet().stream()
                        .map(symbolToOpenPositionInfo -> {
                            CandlestickEvent currentState = candles.get(symbolToOpenPositionInfo.getKey());
                            return new BigDecimal(currentState.getClose()).multiply(symbolToOpenPositionInfo.getValue().getBalance());
                        })
                        .reduce(new BigDecimal(0), BigDecimal::add)
        );
    }
}
