package ru.avca.robot.factory;

import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import ru.avca.grpcservices.TradeNotifierGrpc;

/**
 * @author a.chermashentsev
 * Date: 19.04.2021
 **/
@Factory
public class GrpcFactory {
    @Bean
    TradeNotifierGrpc.TradeNotifierFutureStub futureStub(
            @GrpcChannel("tg-bot-notifier") ManagedChannel channel
    ) {
        return TradeNotifierGrpc.newFutureStub(channel);
    }
}
