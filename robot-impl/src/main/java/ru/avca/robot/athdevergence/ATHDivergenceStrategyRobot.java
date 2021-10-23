//package ru.avca.robot.athdevergence;
//
//import com.binance.api.client.BinanceApiClientFactory;
//import com.binance.api.client.domain.OrderSide;
//import com.binance.api.client.domain.OrderType;
//import com.binance.api.client.domain.TimeInForce;
//import com.binance.api.client.domain.account.NewOrder;
//import com.binance.api.client.domain.account.NewOrderResponse;
//import com.binance.api.client.domain.event.CandlestickEvent;
//import com.binance.api.client.domain.market.CandlestickInterval;
//import com.binance.api.client.exception.BinanceApiException;
//import io.micronaut.context.event.ApplicationEventPublisher;
//import io.micronaut.context.event.StartupEvent;
//import io.micronaut.runtime.event.annotation.EventListener;
//import io.micronaut.scheduling.annotation.Async;
//import org.apache.commons.collections4.queue.CircularFifoQueue;
//import org.apache.commons.lang3.tuple.Pair;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import ru.avca.robot.OpenPositionInfo;
//import ru.avca.robot.config.AthDivergenceRobotConfig;
//import ru.avca.robot.event.CandlestickEvents;
//import ru.avca.robot.event.RobotEvents;
//import ru.avca.robot.utils.RobotUtils;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.stream.Collector;
//import java.util.stream.Collectors;
//
//import static java.util.stream.Collectors.groupingBy;
//import static java.util.stream.Collectors.toMap;
//
///**
// * @author a.chermashentsev
// * Date: 05.10.2021
// **/
////@Singleton
//public class ATHDivergenceStrategyRobot {
//
//    private static final Logger LOG = LoggerFactory.getLogger(ATHDivergenceStrategyRobot.class);
//
//    @Inject private AthDivergenceRobotStateLoader robotStateLoader;
//    @Inject private RobotUtils robotUtils;
//    @Inject private AthDivergenceRobotConfig robotConfig;
//    @Inject private ApplicationEventPublisher publisher;
//    @Inject private BinanceApiClientFactory binanceApiClientFactory;
//
//    private volatile ATHDivergenceState state;
//    private volatile ConcurrentMap<String, CircularFifoQueue<BigDecimal>> candlesticksHighPrices;
//    private volatile ConcurrentMap<String, BigDecimal> candlesticksCurrentAthValues;
//
//
//    @Async
//    @EventListener
//    public void onStart(StartupEvent ignore) {
//        state = robotStateLoader.loadState();
//
//        List<String> symbols = robotUtils.loadSymbols(robotConfig.getUsdCoin())
//                .limit(10).collect(Collectors.toList());
//        candlesticksHighPrices = symbols.stream()
//                .flatMap(symbol -> {
//                            LOG.info("Load history for symbol {}", symbol);
//                            return robotUtils.loadHistory(
//                                            symbol,
//                                            robotConfig.getInterval(),
//                                            Instant.now()
//                                                    .minus(
//                                                            robotUtils.timeInMillis(robotConfig.getInterval()) * robotConfig.getPeriodLength(),
//                                                            ChronoUnit.MILLIS
//                                                    ).toEpochMilli(),
//                                            Instant.now().toEpochMilli()
//                                    )
//                                    .map(candlestick -> Pair.of(symbol, candlestick));
//                        }
//                ).collect(groupingBy(Pair::getKey, ConcurrentHashMap::new, Collector.of(
//                        () -> new CircularFifoQueue<>(robotConfig.getPeriodLength()),
//                        (list, pair) -> list.add(new BigDecimal(pair.getValue().getHigh())),
//                        (list1, list2) -> {
//                            list1.addAll(list2);
//                            return list1;
//                        }
//                )));
//
//        this.candlesticksCurrentAthValues = candlesticksHighPrices.entrySet().stream()
//                .filter(entry -> entry.getValue().size() == robotConfig.getPeriodLength())
//                .collect(toMap(
//                        Map.Entry::getKey,
//                        entry -> entry.getValue().stream().max(Comparator.comparingDouble(BigDecimal::doubleValue)).orElseThrow(() -> new IllegalStateException("Can't find max for " + entry.getKey())),
//                        (a, b) -> a,
//                        ConcurrentHashMap::new
//                ));
//
//        publisher.publishEvent(new CandlestickEvents.StartListenCandlesticksEvent(new CandlestickEvents.ListenerKey(
//                symbols.stream()
//                        .map(String::toLowerCase)
//                        .collect(Collectors.joining(",")),
//                CandlestickInterval.ONE_MINUTE
//        )));
//        LOG.info("Robot has started {}\n{}\n{}", state, candlesticksHighPrices, candlesticksCurrentAthValues);
//    }
//
//    @EventListener
//    @Async
//    public void onCandlestick(CandlestickEvents.BinanceCandlestickEvent candle) {
//        CandlestickEvent binanceEvent = candle.getBinanceEvent();
//        BigDecimal newPrice = new BigDecimal(binanceEvent.getClose());
//        String symbol = binanceEvent.getSymbol();
//        CircularFifoQueue<BigDecimal> oldValues = candlesticksHighPrices.computeIfAbsent(
//                symbol,
//                key -> new CircularFifoQueue<>(robotConfig.getPeriodLength()));
//        //todo если кендлстик последний, сохраняем в список цен
//        if (binanceEvent.getBarFinal()) {
//            if (oldValues.size() < robotConfig.getPeriodLength()) {
//                LOG.trace("{} Old values less then period, just add value", symbol);
//                oldValues.add(newPrice);
//            }
//            else {
//                BigDecimal nextRemoveValue = oldValues.peek();
//                BigDecimal prevHighValue = candlesticksCurrentAthValues.computeIfAbsent(symbol, key -> newPrice);
//                oldValues.add(newPrice);
//                if (nextRemoveValue != null && nextRemoveValue.equals(prevHighValue)) {
//                    Optional<BigDecimal> max = oldValues.stream().max(Comparator.comparingDouble(BigDecimal::doubleValue));
//                    max.ifPresent(value -> candlesticksCurrentAthValues.put(symbol, value));
//                    LOG.trace("{} prev high value was the first value in queue. New state {} \n{}", symbol, candlesticksCurrentAthValues, candlesticksHighPrices);
//                }
//                else {
//                    prevHighValue = candlesticksCurrentAthValues.get(symbol);
//                    if (prevHighValue.doubleValue() < newPrice.doubleValue()) {
//                        candlesticksCurrentAthValues.put(symbol, prevHighValue);
//                    }
//                    LOG.trace("{} Prev high value was somewhere in the queue. New state {} \n{}", symbol, candlesticksCurrentAthValues, candlesticksHighPrices);
//                }
//            }
//        }
//
//        state.getOpenPosition(symbol)
//                .ifPresentOrElse(
//                        balance -> {
//
//                        },
//                        () -> {
//                            //todo если список цен заполнен и получили отклонение на нужный процент, то создаем орден на покупку
//                            if (oldValues.size() >= robotConfig.getPeriodLength() && state.balances().count() < robotConfig.getMaxNumberOfOpenPositions()) {
//                                if (newPrice.doubleValue() <= candlesticksCurrentAthValues.get(symbol).doubleValue() * robotConfig.getBuyDivergencePercent()) {
//                                    OpenPositionInfo response = openPosition(symbol, robotConfig.getAvailableUsdtForBuy(state.getUsdQuantity()));
//                                    state.addOpenPosition(response);
//
//
//                                }
//                            }
//                        }
//                );
//
//
//        //todo если прилетела свеча для открытой позиции, смотрим не ушли ли до цены докупа
//        //todo если прилетела свеча для открытой позиции, и уже докупались, смотрим не ушли ли до цены закрытия
//        //todo если прилетела свеча для открытой позиции и прошли profit price, продаем. ИЛИ не делаем этого, если этот функционал через ordens будет сделан
//    }
//
//    private ATHDivergenceState.AthDivergenceOrder openBuyOrder(String symbol, BigDecimal volume, BigDecimal price) {
//        NewOrder newOrder = new NewOrder(symbol, OrderSide.BUY, OrderType.TAKE_PROFIT, TimeInForce.GTC, null);
//        newOrder.quoteOrderQty(volume.stripTrailingZeros().toPlainString());
//        newOrder.price(price.stripTrailingZeros().toPlainString());
//        newOrder.stopPrice(price.stripTrailingZeros().toPlainString());
//        NewOrderResponse response = binanceApiClientFactory.newRestClient().newOrder(newOrder);
//        LOG.info("Buy response {} ", response);
//
//        return new ATHDivergenceState.AthDivergenceOrder(symbol, volume, OrderSide.BUY);
//    }
//
//
//    private OpenPositionInfo openPosition(String symbol, BigDecimal usdtBalance) {
//        NewOrder order = new NewOrder(
//                symbol,
//                OrderSide.BUY,
//                OrderType.MARKET,
//                null,
//                null
//        );
//
//        order.quoteOrderQty(usdtBalance.stripTrailingZeros().toPlainString());
//        try {
//            NewOrderResponse newOrderResponse = binanceApiClientFactory.newRestClient().newOrder(order);
//            LOG.info("Open position response {}", newOrderResponse);
//
//            BigDecimal executedQty = new BigDecimal(newOrderResponse.getExecutedQty());
//            BigDecimal quoteQty = new BigDecimal(newOrderResponse.getCummulativeQuoteQty());
//            BigDecimal executedPrice = quoteQty.divide(executedQty, 15, RoundingMode.CEILING);
//            OpenPositionInfo info = new OpenPositionInfo(symbol, executedQty, executedPrice);
//            LOG.info("Executed price for {} is {}", symbol, executedPrice);
//
//            publisher.publishEventAsync(new RobotEvents.BuyEvent(
//                    executedQty,
//                    quoteQty,
//                    executedPrice,
//                    symbol
//            ));
//            LOG.info("Open position {} {}", symbol, info);
//            return info;
//        } catch (Exception e) {
//            if (e instanceof BinanceApiException) {
//                LOG.error("Code error {}", ((BinanceApiException) e).getError().getCode());
//            }
//            LOG.error("Can't open position. Rejected order {}", order, e);
//            return null;
//        }
//
//
//    }
//
//    public void onTrade() {
//        //todo если есть открытая позиция, надо её закрыть
//    }
//}
