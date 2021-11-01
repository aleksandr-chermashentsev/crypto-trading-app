package ru.avca.robot.athdevergence;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.event.CandlestickEvent;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.config.AthDivergenceRobotConfig;
import ru.avca.robot.config.RiskProcessorConfig;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;
import ru.avca.robot.grpc.RobotStateService;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author a.chermashentsev
 * Date: 20.10.2021
 **/
@Context
public class RiskProcessorComponent {
    private static final Logger LOG = LoggerFactory.getLogger(RiskProcessorComponent.class);

    @Inject private AthDivergenceRobotStateLoader stateLoader;
    @Inject private RiskProcessorConfig config;
    @Inject private RobotStateService robotStateService;
    @Inject private ApplicationEventPublisher eventPublisher;
    @Inject private AthDivergenceRobotConfig robotConfig;
    @Inject private MarketOrderExecutor marketOrderExecutor;
    private volatile ATHDivergenceState state;
    private final ReentrantLock buyLock = new ReentrantLock();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> unlockFeature;

    @EventListener
    public void onStart(StartupEvent startupEvent) {
        state = stateLoader.loadState();
        LOG.info("State loaded {}", state);
    }

    @EventListener
    @Async
    public void onSignal(CandlestickEvents.SignalEvent signal) {
        lock();
        if (state != null && state.openPositionsCount() < config.getMaxNumberOfOpenPositions()) {
            updateBalance();
            BigDecimal usdBalance = state.getUsdQuantity();
            BigDecimal quantity = usdBalance
                    .divide(BigDecimal.valueOf(config.getMaxNumberOfOpenPositions()), 15, RoundingMode.CEILING)
                    .multiply(BigDecimal.valueOf(config.getFirstBuyPercent()));
            LOG.info("First buy {} on {}", signal.getSymbol(), quantity);
            onBuy(marketOrderExecutor.openPosition(new CandlestickEvents.MarketOrderEvent(
                    OrderSide.BUY,
                    signal.getSymbol(),
                    quantity
            )));
        }
    }

    private void lock() {
        buyLock.lock();
        if (unlockFeature != null && !unlockFeature.isDone()) {
            unlockFeature.cancel(true);
        }
        unlockFeature = scheduledExecutorService.schedule(buyLock::unlock, 2, TimeUnit.MINUTES);
    }

    @EventListener
    @Async
    public void onCandlestick(CandlestickEvents.BinanceCandlestickEvent candlestickEvent) {
        if (this.state == null) {
            return;
        }
        CandlestickEvent binanceEvent = candlestickEvent.getBinanceEvent();
        String symbol = binanceEvent.getSymbol();
        state.getOpenPosition(symbol)
        .ifPresent(openPosition -> {
            BigDecimal closePrice = new BigDecimal(binanceEvent.getClose());
            //check stop loss and loss buy more
            if (openPosition.getPrice().doubleValue() > closePrice.doubleValue()) {
                if (openPosition.getRebuyCount() < config.getMaxNumberOfBuyMore() && openPosition.getPrice().doubleValue() * config.getBuyMoreThreshold() >= closePrice.doubleValue()) {
                    lock();
                    LOG.info("Buy more {}", symbol);
                    onBuy(marketOrderExecutor.openPosition(new CandlestickEvents.MarketOrderEvent(
                            OrderSide.BUY,
                            symbol,
                            openPosition.getBalance().multiply(openPosition.getPrice()).divide(BigDecimal.valueOf(config.getBuyMorePercentOfOpenPosition()), 15, RoundingMode.CEILING)
                    )));
                }
                else if (openPosition.getPrice().doubleValue() * config.getStopLoss() >= closePrice.doubleValue()) {
                    lock();
                    LOG.info("Stop loss sell {}", symbol);
                    onSell(marketOrderExecutor.closePosition(new CandlestickEvents.MarketOrderEvent(
                            OrderSide.SELL,
                            symbol,
                            openPosition.getBalance()
                    )));
                }
            }
            //check take profit
            else if (openPosition.getPrice().doubleValue() * config.getTakeProfit() <= closePrice.doubleValue()){
                lock();
                LOG.info("Take profit sell {}", symbol);
                onSell(marketOrderExecutor.closePosition(new CandlestickEvents.MarketOrderEvent(
                        OrderSide.SELL,
                        symbol,
                        openPosition.getBalance()
                )));

            }
        });
    }

    private void onBuy(RobotEvents.BuyEvent buyEvent) {
        state.addBuy(buyEvent);
        robotStateService.saveOpenPositions(robotConfig.getRobotName(), state.openPositions());
        robotStateService.saveCurrencyBalance(state.getUsdCoin(), state.getUsdQuantity());
        unlockFeature.cancel(true);
        buyLock.unlock();
        eventPublisher.publishEventAsync(buyEvent);
    }

    private void onSell(RobotEvents.SellEvent sellEvent) {
        updateBalance();
        state.addSell(sellEvent);
        robotStateService.saveOpenPositions(robotConfig.getRobotName(), state.openPositions());
        robotStateService.saveCurrencyBalance(state.getUsdCoin(), state.getUsdQuantity());
        unlockFeature.cancel(true);
        buyLock.unlock();
        eventPublisher.publishEventAsync(sellEvent);
    }

    private void updateBalance() {
        robotStateService.getUsdtBalance(robotConfig.getUsdCoin())
                        .ifPresent(usdQuantity -> {
                            state.setUsdQuantity(usdQuantity);
                            LOG.info("Usd quantity was set to {}", usdQuantity);
                        });
    }
}
