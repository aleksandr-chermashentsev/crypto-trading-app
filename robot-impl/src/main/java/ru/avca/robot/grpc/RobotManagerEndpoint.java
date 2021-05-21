package ru.avca.robot.grpc;

import io.grpc.stub.StreamObserver;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.grpcservices.*;
import ru.avca.robot.BestCoinStrategyRobot;

import javax.inject.Inject;
import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author a.chermashentsev
 * Date: 18.05.2021
 **/
@Context
public class RobotManagerEndpoint extends RobotStateManagerGrpc.RobotStateManagerImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(RobotManagerEndpoint.class);

    @Inject private ApplicationEventPublisher eventPublisher;
    @Inject private BestCoinStrategyRobot robot;

    @Override
    public void closePositions(ClosePositionsMsg request, StreamObserver<ClosePositionsResponse> responseObserver) {
        try {
            robot.closePosition();
            responseObserver.onNext(
                    ClosePositionsResponse.newBuilder()
                            .setSuccess(true)
                            .build()
            );
        } catch (Exception e) {
            LOG.error("Can't close position ", e);
            responseObserver.onNext(
                    ClosePositionsResponse.newBuilder()
                            .setSuccess(false)
                            .build()
            );
        }
        responseObserver.onCompleted();

    }

    @Override
    public void setUsdBalance(SetUsdBalanceMsg request, StreamObserver<SetUsdBalanceResponse> responseObserver) {
        try {
            BigDecimal newBalance = new BigDecimal(request.getBalance());
            checkState(newBalance.doubleValue() > 0, "Can't set new balance {} less then 0.", newBalance);
            robot.setUsdBalance(newBalance);
            responseObserver.onNext(SetUsdBalanceResponse.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            responseObserver.onNext(SetUsdBalanceResponse.newBuilder().setSuccess(false).build());
        }
        responseObserver.onCompleted();
    }
}
