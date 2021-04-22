package ru.avca.robot.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import event_hub.EventHubGrpc;
import event_hub.EventType;
import event_hub.RegisterRequest;
import event_hub.RegisterResponse;
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
        ListenableFuture<RegisterResponse> register = stub.register(RegisterRequest.newBuilder()
                .addTypes(EventType.ROBOT_START)
                .build()
        );
        RegisterResponse response = register.get();
        LOG.info("Got response {}", response);
    }

}
