package ru.avca.robot.grpc;

import ru.avca.grpcservices.*;
import ru.avca.robot.OpenPositionInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author a.chermashentsev
 * Date: 03.05.2021
 **/
@Singleton
public class RobotStateService {

    @Inject private RobotStateServiceGrpc.RobotStateServiceBlockingStub robotStateServiceBlockingStub;

    public Map<String, OpenPositionInfo> loadAllOpenPositionInfos(String robotName) {
        OpenPositionsMsg allOpenPositions = robotStateServiceBlockingStub.getAllOpenPositions(
                RobotName.newBuilder()
                        .setRobotName(robotName)
                        .build()
        );
        return allOpenPositions.getOpenPositionsList().stream()
                .collect(Collectors.toMap(
                        OpenPositionMsg::getSymbol,
                        value -> new OpenPositionInfo(
                                value.getSymbol(),
                                new BigDecimal(value.getBalance()),
                                new BigDecimal(value.getPrice()),
                                value.getRebuyCount()
                        )
                ));
    }

    public Optional<BigDecimal> getUsdtBalance(String usdCoin) {
        try {
            return robotStateServiceBlockingStub.getAllCurrencyBalance(Empty.getDefaultInstance())
                    .getCurrencyBalancesList().stream()
                    .filter(msg -> usdCoin.equals(msg.getSymbol().toUpperCase(Locale.ROOT)))
                    .map(msg -> new BigDecimal(msg.getBalance()))
                    .findAny();
        } catch (Exception e) {
            System.exit(1);
            return Optional.empty();
        }
    }

    public void saveOpenPositions(String robotName, Stream<OpenPositionInfo> openPositionInfos) {
        OpenPositionsMsg msg = OpenPositionsMsg.newBuilder().addAllOpenPositions(() -> openPositionInfos
                .map(openPositionInfo ->
                        OpenPositionMsg.newBuilder()
                                .setBalance(openPositionInfo.getBalance().toPlainString())
                                .setSymbol(openPositionInfo.getSymbol())
                                .setPrice(openPositionInfo.getPrice().toPlainString())
                                .setRobotName(robotName)
                                .build()
                ).iterator()
        ).build();

        robotStateServiceBlockingStub.updateOpenPositions(msg);
    }

    public void saveCurrencyBalance(String symbol, BigDecimal balance) {
        robotStateServiceBlockingStub.saveCurrencyBalance(
                CurrencyBalanceMsg.newBuilder()
                        .setSymbol(symbol)
                        .setBalance(balance.toPlainString())
                        .build()
        );
    }

    public void turnOffSymbol(String symbol) {
        robotStateServiceBlockingStub.turnOffSymbol(Symbol.newBuilder()
                .setSymbol(symbol)
                .build()
        );
    }

    public void turnOnSymbol(String symbol) {
        robotStateServiceBlockingStub.turnOnSymbol(Symbol.newBuilder()
                        .setSymbol(symbol)
                        .build());
    }

    public Stream<String> getAllTurnedOffSymbols() {
        return robotStateServiceBlockingStub.getAllTurnOffSymbols(Empty.getDefaultInstance()).getSymbolList().stream();
    }
}
