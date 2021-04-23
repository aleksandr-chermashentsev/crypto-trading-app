package ru.avca.robot.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import event_hub.EventHubGrpc;
import event_hub.EventRequest;
import event_hub.EventResponse;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

/**
 * @author a.chermashentsev
 * Date: 19.04.2021
 **/
@Context
public class EventHubClient {
    private static final Logger LOG = LoggerFactory.getLogger(EventHubClient.class);
    @Inject private EventHubGrpc.EventHubFutureStub stub;

    @EventListener
    @Async
    public void subscribeOnStart(StartupEvent event) throws ExecutionException, InterruptedException {
        ListenableFuture<EventResponse> register = stub.register(EventRequest.newBuilder()
                .setFrom("robotImpl")
                .build()
        );
        EventResponse response = register.get();
        LOG.info("Got response {}", response);
    }

}
