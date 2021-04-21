package ru.avca.robot.factory;

import event_hub.EventHubGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;

/**
 * @author a.chermashentsev
 * Date: 19.04.2021
 **/
@Factory
public class GrpcFactory {
    @Bean
    EventHubGrpc.EventHubFutureStub futureStub(
            @GrpcChannel("http://localhost:50051") ManagedChannel channel
    ) {
        return EventHubGrpc.newFutureStub(channel);
    }
}
