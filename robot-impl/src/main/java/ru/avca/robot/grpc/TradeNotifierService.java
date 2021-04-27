package ru.avca.robot.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.micronaut.context.annotation.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.event.RobotEvents;
import tradenotifier.EventResponse;
import tradenotifier.RobotTradeEvent;
import tradenotifier.TradeNotifierGrpc;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

/**
 * @author a.chermashentsev
 * Date: 19.04.2021
 **/
@Context
public class TradeNotifierService {
    private static final Logger LOG = LoggerFactory.getLogger(TradeNotifierService.class);
    @Inject private TradeNotifierGrpc.TradeNotifierFutureStub stub;

    public void sendBuyEvent(RobotEvents.BuyEvent buyEvent) {

        ListenableFuture<EventResponse> responseFuture = stub.trade(
                RobotTradeEvent.newBuilder()
                        .setSymbol(buyEvent.getSymbol())
                        .setBaseQty(buyEvent.getBaseQty().doubleValue())
                        .setQuoteQty(buyEvent.getQuoteQty().doubleValue())
                        .setSide(RobotTradeEvent.TradeSide.BUY)
                        .setExpectedPrice(buyEvent.getExpectedPrice().doubleValue())
                        .build()

        );

        try {
            LOG.info("Send BuyEvent to eventhub. Got Response {}", responseFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Got error during buy event", e);
        }
    }
}
