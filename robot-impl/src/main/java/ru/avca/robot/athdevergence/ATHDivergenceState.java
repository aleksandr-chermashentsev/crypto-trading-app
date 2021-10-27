package ru.avca.robot.athdevergence;

import com.binance.api.client.domain.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import ru.avca.robot.OpenPositionInfo;
import ru.avca.robot.event.RobotEvents;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author a.chermashentsev
 * Date: 06.10.2021
 **/
@AllArgsConstructor
@ToString
public class ATHDivergenceState {
    @Getter private final String usdCoin;
    @Getter private volatile BigDecimal usdQuantity;
    private Map<String, AthDivergenceOrder> orders;
    private Map<String, OpenPositionInfo> balances;


    public Stream<AthDivergenceOrder> orders() {
        return orders.values().stream();
    }

    public Stream<OpenPositionInfo> openPositions() {
        return balances.values().stream();
    }

    public int openPositionsCount() {
        return Math.max(0, balances.size());
    }

    public Optional<OpenPositionInfo> getOpenPosition(String symbol) {
        return Optional.ofNullable(balances.get(symbol));
    }

    public void addOpenPosition(OpenPositionInfo response) {
        balances.put(response.getSymbol(), response);
    }

    public synchronized void addBuy(RobotEvents.BuyEvent buyEvent) {
        OpenPositionInfo oldBalance = balances.get(buyEvent.getSymbol());
        if (oldBalance != null) {
            balances.put(buyEvent.getSymbol(), new OpenPositionInfo(
                    buyEvent.getSymbol(),
                    oldBalance.getBalance().add(buyEvent.getBaseQty()),
                    oldBalance.getBalance().multiply(oldBalance.getPrice()).add(buyEvent.getQuoteQty())
                            .divide(oldBalance.getBalance().add(buyEvent.getBaseQty()), 15, RoundingMode.CEILING),
                    oldBalance.getRebuyCount() + 1
            ));
        }
        else {
            balances.put(buyEvent.getSymbol(), new OpenPositionInfo(
                    buyEvent.getSymbol(),
                    buyEvent.getBaseQty(),
                    buyEvent.getRealPrice(),
                    0
            ));
        }
        usdQuantity = usdQuantity.add(buyEvent.getQuoteQty().negate());
    }

    public synchronized void addSell(RobotEvents.SellEvent sellEvent) {
        balances.remove(sellEvent.getSymbol());
        usdQuantity = usdQuantity.add(sellEvent.getUsdBalance());
    }

    @Value
    public static class AthDivergenceOrder {
        String symbol;
        BigDecimal quantity;
        OrderSide side;
    }
}
