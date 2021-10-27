package ru.avca.robot.athdevergence;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.request.OrderRequest;
import org.jetbrains.annotations.NotNull;
import ru.avca.robot.OpenPositionInfo;
import ru.avca.robot.config.AthDivergenceRobotConfig;
import ru.avca.robot.grpc.RobotStateService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * @author a.chermashentsev
 * Date: 06.10.2021
 **/
@Singleton
public class AthDivergenceRobotStateLoader {
    @Inject private RobotStateService robotStateService;
    @Inject private BinanceApiClientFactory binanceApiClientFactory;
    @Inject private AthDivergenceRobotConfig robotConfig;

    public ATHDivergenceState loadState() {
        BinanceApiRestClient binanceApiRestClient = binanceApiClientFactory.newRestClient();
        Map<String, BigDecimal> balancesFromBinance = binanceApiRestClient.getAccount().getBalances()
                .stream()
                .filter(assetBalance -> new BigDecimal(assetBalance.getFree()).doubleValue() > 0.0000001D)
                .collect(toMap(AssetBalance::getAsset, balance -> new BigDecimal(balance.getFree())));
        Map<String, OpenPositionInfo> balances = robotStateService.loadAllOpenPositionInfos(robotConfig.getRobotName())
                .values().stream()
                .filter(openPositionInfo -> balancesFromBinance.containsKey(getBaseSymbol(openPositionInfo.getSymbol())))
                .collect(toMap(OpenPositionInfo::getSymbol, Function.identity()));

        robotStateService.getUsdtBalance(robotConfig.getUsdCoin())
                .map(savedBalance -> getClampedBalance(balancesFromBinance, savedBalance))
                .orElseGet(() -> getClampedBalance(balancesFromBinance, new BigDecimal(robotConfig.getDefaultUsdBalance())));


        //update current balances
        balancesFromBinance.forEach((symbol, qty) -> {
            OpenPositionInfo dbBalance = balances.get(symbol + robotConfig.getUsdCoin());
            if (dbBalance != null) {
                balances.put(symbol + robotConfig.getUsdCoin(),
                        new OpenPositionInfo(
                                dbBalance.getSymbol(),
                                qty,
                                dbBalance.getPrice(),
                                dbBalance.getRebuyCount())
                );
            }
        });
        Map<String, ATHDivergenceState.AthDivergenceOrder> orders = binanceApiRestClient
                .getOpenOrders(new OrderRequest(null))
                .stream()
                .map(order -> new ATHDivergenceState.AthDivergenceOrder(
                        order.getSymbol(),
                        new BigDecimal(order.getOrigQty()),
                        order.getSide()
                ))
                .collect(toMap(ATHDivergenceState.AthDivergenceOrder::getSymbol, Function.identity()));

        return new ATHDivergenceState(
                robotConfig.getUsdCoin(),
                balancesFromBinance.get(robotConfig.getUsdCoin()),
                orders,
                balances
        );
    }

    @NotNull
    private String getBaseSymbol(String symbol) {
        return symbol.replace(robotConfig.getUsdCoin(), "");
    }

    private BigDecimal getClampedBalance(Map<String, BigDecimal> balancesFromBinance, BigDecimal savedBalance) {
        return Optional.ofNullable(balancesFromBinance.get(robotConfig.getUsdCoin()))
                .map(binanceBalance -> {
                    if (binanceBalance.doubleValue() < savedBalance.doubleValue()) {
                        return binanceBalance;
                    }
                    return savedBalance;
                })
                .orElse(savedBalance);
    }
}
