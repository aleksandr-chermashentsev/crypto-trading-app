package ru.avca.grpc

import io.grpc.ManagedChannel
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import ru.avca.grpcservices.RobotStateManagerGrpc

/**
 *
 * @author a.chermashentsev
 * Date: 18.05.2021
 **/
@Factory
open class GrpcFactory {

    @Bean
    open fun robotImpl(
        @GrpcChannel("robot-impl") channel: ManagedChannel
    ): RobotStateManagerGrpc.RobotStateManagerBlockingStub {
        return RobotStateManagerGrpc.newBlockingStub(channel);
    }
}