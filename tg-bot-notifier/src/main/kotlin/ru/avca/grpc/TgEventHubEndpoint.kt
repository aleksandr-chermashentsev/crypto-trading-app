package ru.avca.grpc

import io.grpc.stub.StreamObserver
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventPublisher
import tradenotifier.EventResponse
import tradenotifier.RobotTradeEvent
import tradenotifier.TradeNotifierGrpc
import javax.inject.Inject

/**
 *
 * @author a.chermashentsev
 * Date: 26.04.2021
 **/
@Context
class TgEventHubEndpoint(
    @Inject private val eventPublisher: ApplicationEventPublisher
) : TradeNotifierGrpc.TradeNotifierImplBase() {

    override fun trade(request: RobotTradeEvent?, responseObserver: StreamObserver<EventResponse>?) {
        request!!
        eventPublisher.publishEvent(request)
        responseObserver?.onNext(EventResponse.newBuilder()
            .setIsHandled(true)
            .build()
        )
        responseObserver?.onCompleted()
    }
}