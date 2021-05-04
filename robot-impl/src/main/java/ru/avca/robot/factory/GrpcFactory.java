package ru.avca.robot.factory;

import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import ru.avca.grpcservices.RobotStateServiceGrpc;
import ru.avca.grpcservices.TradeNotifierGrpc;

import javax.inject.Named;

/**
 * @author a.chermashentsev
 * Date: 19.04.2021
 **/
@Factory
public class GrpcFactory {
    @Named("tg-bot-notifier")
    @Bean
    TradeNotifierGrpc.TradeNotifierFutureStub tgBotFutureStub(
            @GrpcChannel("tg-bot-notifier") ManagedChannel channel
    ) {
        return TradeNotifierGrpc.newFutureStub(channel);
    }

    @Named("database-persist")
    @Bean
    TradeNotifierGrpc.TradeNotifierFutureStub dbPersistFutureStub(
            @GrpcChannel("database-persist") ManagedChannel channel
    ) {
        return TradeNotifierGrpc.newFutureStub(channel);
    }

    @Bean RobotStateServiceGrpc.RobotStateServiceBlockingStub robotStateServiceBlockingStub(
            @GrpcChannel("database-persist") ManagedChannel channel
    ) {
        return RobotStateServiceGrpc.newBlockingStub(channel);
    }
}
