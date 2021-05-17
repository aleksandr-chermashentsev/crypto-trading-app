package ru.avca.dbpersist.grpc

import io.grpc.stub.StreamObserver
import io.micronaut.context.annotation.Context
import ru.avca.dbpersist.domain.TradeDomain
import ru.avca.dbpersist.repository.TradeRepository
import ru.avca.grpcservices.EventResponse
import ru.avca.grpcservices.RobotTradeEvent
import ru.avca.grpcservices.TradeNotifierGrpc
import java.time.Instant
import javax.inject.Inject

/**
 *
 * @author a.chermashentsev
 * Date: 02.05.2021
 **/
@Context
class TradeEndpoint(
    @Inject private val repository:TradeRepository
) : TradeNotifierGrpc.TradeNotifierImplBase() {

    override fun trade(request: RobotTradeEvent?, responseObserver: StreamObserver<EventResponse>?) {
        request!!
        repository.save(
            TradeDomain(
                null,
                eventTime = Instant.now().toEpochMilli(),
                request.symbol,
                request.baseQty.toString(),
                request.quoteQty.toString(),
                request.side.toString()
            )
        )
        responseObserver?.onNext(EventResponse.getDefaultInstance())
        responseObserver?.onCompleted()
    }
}