package ru.avca.robot.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.grpcservices.*;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ExecutionException;

/**
 * @author a.chermashentsev
 * Date: 19.04.2021
 **/
@Context
public class EventsNotifierService {
    private static final Logger LOG = LoggerFactory.getLogger(EventsNotifierService.class);
    @Inject @Named("tg-bot-notifier") private TradeNotifierGrpc.TradeNotifierFutureStub tgBotNotifier;
    @Inject @Named("database-persist") private TradeNotifierGrpc.TradeNotifierFutureStub databasePersist;

    @EventListener
    @Async
    public void sendBuyEvent(RobotEvents.BuyEvent buyEvent) {

        RobotTradeEvent tradeEvent = RobotTradeEvent.newBuilder()
                .setSymbol(buyEvent.getSymbol())
                .setBaseQty(buyEvent.getBaseQty().doubleValue())
                .setQuoteQty(buyEvent.getQuoteQty().doubleValue())
                .setSide(RobotTradeEvent.TradeSide.BUY)
                .setExpectedPrice(buyEvent.getExpectedPrice().doubleValue())
                .build();
        ListenableFuture<EventResponse> tgResponseFuture = tgBotNotifier.trade(tradeEvent);
        ListenableFuture<EventResponse> dbResponseFuture = databasePersist.trade(tradeEvent);

        try {
            LOG.info("Send BuyEvent to telegram. Got Response {}", tgResponseFuture.get());
            LOG.info("Send BuyEvent to database. Got Response {}", dbResponseFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Got error during buy event", e);
        }
    }

    @EventListener
    @Async
    public void sendRestartEvent(CandlestickEvents.RestartListenCandlesticksEvent event) {
        tgBotNotifier.restart(RobotRestartEvent.getDefaultInstance());
    }

    @EventListener
    @Async
    public void sendStartEvent(CandlestickEvents.StartListenCandlesticksEvent startListenCandlesticksEvent) {
        tgBotNotifier.start(RobotStartEvent.getDefaultInstance());
    }
}
