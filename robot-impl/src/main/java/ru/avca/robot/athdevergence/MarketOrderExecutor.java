package ru.avca.robot.athdevergence;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.exception.BinanceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avca.robot.OpenPositionInfo;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author a.chermashentsev
 * Date: 21.10.2021
 **/
@Singleton
public class MarketOrderExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(MarketOrderExecutor.class);
    @Inject private BinanceApiClientFactory binanceApiClientFactory;

    public RobotEvents.SellEvent closePosition(CandlestickEvents.MarketOrderEvent event) {
        String symbol = event.getSymbol();
        try {
            NewOrder newOrder = new NewOrder(
                    symbol,
                    OrderSide.SELL,
                    OrderType.MARKET,
                    null,
                    event.getQuantity().stripTrailingZeros().toPlainString()
            );
            NewOrderResponse response = binanceApiClientFactory.newRestClient().newOrder(newOrder);
            LOG.error("Close position response {}", response);
            return new RobotEvents.SellEvent(symbol, new BigDecimal(response.getCummulativeQuoteQty()));
        } catch (Exception e) {
            LOG.error("Exception on close position", e);
            throw e;
        }
    }

    public RobotEvents.BuyEvent openPosition(CandlestickEvents.MarketOrderEvent event) {
        if (event.getQuantity().doubleValue() < 20) {
            return null;
        }
        String symbol = event.getSymbol();
        NewOrder order = new NewOrder(
                symbol,
                OrderSide.BUY,
                OrderType.MARKET,
                null,
                null
        );
        order.quoteOrderQty(event.getQuantity().stripTrailingZeros().toPlainString());
        try {
            NewOrderResponse newOrderResponse = binanceApiClientFactory.newRestClient().newOrder(order);
            LOG.info("Open position response {}", newOrderResponse);

            BigDecimal executedQty = new BigDecimal(newOrderResponse.getExecutedQty());
            BigDecimal quoteQty = new BigDecimal(newOrderResponse.getCummulativeQuoteQty());
            BigDecimal executedPrice = quoteQty.divide(executedQty, 15, RoundingMode.CEILING);
            OpenPositionInfo info = new OpenPositionInfo(symbol, executedQty, executedPrice, 0);
            LOG.info("Executed price for {} is {}", symbol, executedPrice);

            LOG.info("Open position {} {}", symbol, info);
            return new RobotEvents.BuyEvent(
                    executedQty,
                    quoteQty,
                    executedPrice,
                    symbol
            );

        } catch (Exception e) {
            if (e instanceof BinanceApiException) {
                LOG.error("Code error {}", ((BinanceApiException) e).getError().getCode());
            }
            LOG.error("Can't open position. Rejected order {}", order, e);
        }
        return null;
    }
}
