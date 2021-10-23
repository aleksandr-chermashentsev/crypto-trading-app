package ru.avca.robot.event;

import com.binance.api.client.domain.market.CandlestickInterval;
import lombok.Value;
import ru.avca.robot.OpenPositionInfo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author a.chermashentsev
 * Date: 08.04.2021
 **/
public class RobotEvents {

    private RobotEvents() {}
    @Value
    public static class RobotStartEvent {
        String usdCoin;
        BigDecimal stopLoss;
        BigDecimal takeProfit;
        int coinsCount;
        CandlestickInterval interval;
        long nextTimeOrderExecute;
        long orderExecuteInterval;
        BigDecimal initialUsdtBalance;
        Map<String, OpenPositionInfo> openPositionInfosBySymbol;
    }

    public static class TryExecuteOrderEvent {
    }

    @Value
    public static class BuyEvent {
        BigDecimal baseQty;
        BigDecimal quoteQty;
        BigDecimal realPrice;
        String symbol;
    }

    @Value
    public static class SellEvent {
        String symbol;
        BigDecimal usdBalance;
    }
}
