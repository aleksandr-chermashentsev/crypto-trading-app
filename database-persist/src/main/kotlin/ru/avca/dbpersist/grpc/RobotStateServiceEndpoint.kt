package ru.avca.dbpersist.grpc

import io.grpc.stub.StreamObserver
import io.micronaut.context.annotation.Context
import ru.avca.dbpersist.domain.CurrencyBalanceDomain
import ru.avca.dbpersist.domain.OpenPositionDomain
import ru.avca.dbpersist.repository.CurrencyBalanceRepository
import ru.avca.dbpersist.repository.OpenPositionRepository
import ru.avca.grpcservices.*
import java.util.stream.Collectors.toList
import javax.inject.Inject

/**
 *
 * @author a.chermashentsev
 * Date: 03.05.2021
 **/
@Context
class RobotStateServiceEndpoint(
    @Inject private val currencyBalanceRepository: CurrencyBalanceRepository,
    @Inject private val openPositionRepository: OpenPositionRepository
) : RobotStateServiceGrpc.RobotStateServiceImplBase() {
    override fun updateOpenPositions(request: OpenPositionsMsg?, responseObserver: StreamObserver<Empty>?) {
        request!!
        val newPositions = request.openPositionsList.stream()
            .map { OpenPositionDomain(it.symbol, it.price, it.balance, it.robotName) }
            .collect(toList())
        openPositionRepository.updateOpenPositions(newPositions)

        responseObserver?.onNext(Empty.getDefaultInstance())
        responseObserver?.onCompleted()
    }

    override fun getAllOpenPositions(request: RobotName?, responseObserver: StreamObserver<OpenPositionsMsg>?) {
        val positions = openPositionRepository.getAllOpenPositions(request!!.robotName)
            .map {
                OpenPositionMsg.newBuilder()
                    .setBalance(it.balance)
                    .setPrice(it.price)
                    .setSymbol(it.symbol)
                    .build()
            }
            .collect(toList())

        responseObserver?.onNext(
            OpenPositionsMsg.newBuilder()
                .addAllOpenPositions(positions)
                .build()
        )

        responseObserver?.onCompleted()
    }

    override fun saveCurrencyBalance(request: CurrencyBalanceMsg?, responseObserver: StreamObserver<Empty>?) {
        request!!
        currencyBalanceRepository.saveCurrencyBalance(CurrencyBalanceDomain(request.symbol, request.balance))

        responseObserver?.onNext(Empty.getDefaultInstance())
        responseObserver?.onCompleted()
    }

    override fun getAllCurrencyBalance(request: Empty?, responseObserver: StreamObserver<CurrencyBalancesMsg>?) {
        val balances = currencyBalanceRepository.getAllCurrencyBalances()
            .map {
                CurrencyBalanceMsg.newBuilder()
                    .setBalance(it.balance)
                    .setSymbol(it.symbol)
                    .build()
            }
            .collect(toList())

        responseObserver?.onNext(
            CurrencyBalancesMsg.newBuilder()
                .addAllCurrencyBalances(balances)
                .build()
        )

        responseObserver?.onCompleted()
    }
}