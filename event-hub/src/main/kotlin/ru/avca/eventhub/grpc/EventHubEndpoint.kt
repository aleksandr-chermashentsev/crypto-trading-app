package ru.avca.eventhub.grpc

import event_hub.EventHubGrpc
import event_hub.RegisterRequest
import event_hub.RegisterResponse
import io.grpc.stub.StreamObserver
import javax.inject.Singleton

/**
 *
 * @author a.chermashentsev
 * Date: 19.04.2021
 **/
@Singleton
class EventHubEndpoint : EventHubGrpc.EventHubImplBase() {

    override fun register(request: RegisterRequest?, responseObserver: StreamObserver<RegisterResponse>?) {
        val typesList = request?.typesList
        typesList?.forEach { type -> println(type) }
        responseObserver?.onNext(RegisterResponse.newBuilder().setIsRegistered(true).build())
        responseObserver?.onCompleted()
    }
}