package ru.avca.robot;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.config.RobotConfig;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;
import ru.avca.robot.utils.TimeUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;
import static java.util.concurrent.TimeUnit.DAYS;

/**
 * @author a.chermashentsev
 * Date: 30.03.2021
 **/
@Singleton
@Requires(notEnv = Environment.TEST)
public class Runner {
    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    @Inject private ApplicationEventPublisher eventPublisher;
    @Inject RobotConfig robotConfig;
    @Inject BinanceApiClientFactory clientFactory;

    @EventListener
    @Async
    public void startRobot(StartupEvent event) {
        LOG.info("Start robot with config {}", robotConfig);
        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        long intervalMs = getIntervalMs(robotConfig.getInterval());
        List<Candlestick> btcusdt = clientFactory.newRestClient().getCandlestickBars("BTCUSDT", robotConfig.getInterval(), 1, currentTimeUtc - intervalMs, null);
        if (btcusdt.isEmpty()) {
            LOG.error("Couldn't start because btcUsdt is empty");
            return;
        }
        Candlestick candlestick = btcusdt.get(0);

        eventPublisher.publishEventAsync(new RobotEvents.RobotStartEvent(
                robotConfig.getUsdCoin(),
                BigDecimal.valueOf(robotConfig.getStopLoss()),
                BigDecimal.valueOf(robotConfig.getTakeProfit()),
                robotConfig.getCoinsCount(),
                robotConfig.getInterval(),
                candlestick.getCloseTime(),
                intervalMs,
                new BigDecimal(25) //todo load it from properties or database
        ));

    }

    private long getIntervalMs(CandlestickInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> MINUTES.toMillis(1);
            case THREE_MINUTES -> MINUTES.toMillis(3);
            case FIVE_MINUTES -> MINUTES.toMillis(5);
            case FIFTEEN_MINUTES -> MINUTES.toMillis(15);
            case HALF_HOURLY -> MINUTES.toMillis(30);
            case HOURLY -> HOURS.toMillis(1);
            case TWO_HOURLY -> HOURS.toMillis(2);
            case FOUR_HOURLY -> HOURS.toMillis(4);
            case SIX_HOURLY -> HOURS.toMillis(6);
            case EIGHT_HOURLY -> HOURS.toMillis(8);
            case TWELVE_HOURLY -> HOURS.toMillis(12);
            case DAILY -> DAYS.toMillis(1);
            case THREE_DAILY -> DAYS.toMillis(3);
            case WEEKLY -> DAYS.toMillis(7);
            case MONTHLY -> DAYS.toMillis(30);
            default -> throw new RuntimeException("Unknown interval " + interval);
        };
    }
}
