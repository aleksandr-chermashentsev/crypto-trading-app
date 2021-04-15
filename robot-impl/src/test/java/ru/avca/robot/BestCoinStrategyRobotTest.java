package ru.avca.robot;

import com.binance.api.client.domain.event.CandlestickEvent;
import com.binance.api.client.domain.market.CandlestickInterval;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.avca.robot.event.CandlestickEvents;
import ru.avca.robot.event.RobotEvents;
import ru.avca.robot.utils.TimeUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author a.chermashentsev
 * Date: 12.04.2021
 **/
@MicronautTest
@Disabled
@Timeout(3)
class BestCoinStrategyRobotTest {

    @Test
    public void shouldSubscribeOnAllSymbolsWhenGotStartEvent() throws InterruptedException, ExecutionException {

        Map<String, Object> values = new HashMap<>();
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,DOGE-BTC,TBD-USDT");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT", new BigDecimal(0), new BigDecimal(2), 1, CandlestickInterval.TWELVE_HOURLY, TimeUtils.getCurrentTimeUtc() + 10000, 1000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> future = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);

        assertEquals("btc-usdt,eth-usdt,xrp-usdt,tbd-usdt", future.get().getKey().getSymbols());
    }
    @Disabled
    @Test
    public void shouldSaveCandlesDataOnlyForHisSubscriptions() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal(0),new BigDecimal(2), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 500, 1000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent event = createCandle(currentTimeUtc, "test", "100", "10", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(event, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWO_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "TryExecuteOrderEvent wasn't published");

        Future<RobotEvents.BuyEvent> buyEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.BuyEvent.class);
        try {
            RobotEvents.BuyEvent buyEvent = buyEventFuture.get(1, TimeUnit.SECONDS);
            fail("Should be no buy event, but got one " + buyEvent);
        } catch (TimeoutException e) {

        }

    }

    @Test
    public void shouldSendBuyEventWhenBuyExecuted() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal(0),new BigDecimal(2), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 500, 1000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent event = createCandle(currentTimeUtc, "BTC-USDT", "100", "10", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(event, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "TryExecuteOrderEvent wasn't published");

        Future<RobotEvents.BuyEvent> buyEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.BuyEvent.class);
        assertNotNull(buyEventFuture.get(), "BuyEvent wasn't published");
    }

    @Test
    public void shouldOpenPositionInBiggestCandle() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal(0),new BigDecimal(2), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 1000, 1000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent smallCandle = createCandle(currentTimeUtc, "BTC-USDT", "100", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(smallCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        CandlestickEvent bigCandle = createCandle(currentTimeUtc, "ETH-USDT", "100", "10", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(bigCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "TryExecuteOrderEvent wasn't published");

        Future<RobotEvents.BuyEvent> buyEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.BuyEvent.class);
        RobotEvents.BuyEvent buyEvent = buyEventFuture.get();
        assertNotNull(buyEvent, "BuyEvent wasn't published");
        assertEquals(bigCandle.getSymbol(), buyEvent.getSymbol(), "Should buy biggest candle");
    }

    @Test
    public void shouldUpdateCandleWithLatestValue() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal(0),new BigDecimal(1), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 1000, 1000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent smallCandle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "100", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(smallCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        CandlestickEvent bigCandle = createCandle(TimeUtils.getCurrentTimeUtc(), "ETH-USDT", "100", "10", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(bigCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        CandlestickEvent biggestCandle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "90000", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(biggestCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "TryExecuteOrderEvent wasn't published");

        Future<RobotEvents.BuyEvent> buyEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.BuyEvent.class);
        RobotEvents.BuyEvent buyEvent = buyEventFuture.get();
        assertNotNull(buyEvent, "BuyEvent wasn't published");
        assertEquals(biggestCandle.getSymbol(), buyEvent.getSymbol(), "Should buy biggest candle");
    }


    @Test
    public void shouldNotBuyIfNoPositiveCandles() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal(0),new BigDecimal(1), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 1000, 1000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent smallCandle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "100", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(smallCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        CandlestickEvent bigCandle = createCandle(TimeUtils.getCurrentTimeUtc(), "ETH-USDT", "100", "110", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(bigCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        CandlestickEvent biggestCandle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "10", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(biggestCandle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "TryExecuteOrderEvent wasn't published");

        Future<RobotEvents.BuyEvent> buyEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.BuyEvent.class);
        try {
            RobotEvents.BuyEvent buyEvent = buyEventFuture.get(1, TimeUnit.SECONDS);
            fail("Should be no buy event, but got one " + buyEvent);
        } catch (TimeoutException e) {

        }
    }

    @Test
    public void shouldSellWhenGotTwoTryExecuteOrders() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("robot.minDelayBetweenOrderExecutesMs", 0);
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        values.put("test.usd_balance", "50");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal(0),new BigDecimal(1), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 1000, 300, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent candle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "100", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(candle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "First TryExecuteOrderEvent wasn't published");
        tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "Second TryExecuteOrderEvent wasn't published");

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.SellEvent> sellEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.SellEvent.class);
        RobotEvents.SellEvent sellEvent = sellEventFuture.get();
        assertEquals(candle.getSymbol(), sellEvent.getSymbol());

        Future<RobotEvents.BuyEvent> buyEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.BuyEvent.class);
        try {
            RobotEvents.BuyEvent buyEvent = buyEventFuture.get(1, TimeUnit.SECONDS);
            fail("Should be no buy event because only one candle was sent, but got one " + buyEvent);
        } catch (TimeoutException e) {

        }
    }

    @Test
    public void shouldSellWhenHitTakeProfit() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("robot.minDelayBetweenOrderExecutesMs", 0);
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        values.put("test.usd_balance", "100");
        values.put("test.executedQty", "1");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal(0),new BigDecimal("1.2"), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 1000, 3000000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent candle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "100", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(candle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "First TryExecuteOrderEvent wasn't published");

        messageListenerTestHelper.getQueue().clear();
        candle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "130", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(candle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));
        Future<RobotEvents.SellEvent> sellEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.SellEvent.class);
        RobotEvents.SellEvent sellEvent = sellEventFuture.get();
        assertEquals(candle.getSymbol(), sellEvent.getSymbol());
    }

    @Test
    public void shouldSellWhenHitStopLoss() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("robot.minDelayBetweenOrderExecutesMs", 0);
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        values.put("test.usd_balance", "100");
        values.put("test.executedQty", "1");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal("0.8"),new BigDecimal("1.2"), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 1000, 3000000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent candle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "100", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(candle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "First TryExecuteOrderEvent wasn't published");

        messageListenerTestHelper.getQueue().clear();
        candle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "80", "90", "79");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(candle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));
        Future<RobotEvents.SellEvent> sellEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.SellEvent.class);
        RobotEvents.SellEvent sellEvent = sellEventFuture.get();
        assertEquals(candle.getSymbol(), sellEvent.getSymbol());
    }

    @Test
    public void shouldNotSellWhenDoesntHitSlOrTp() throws InterruptedException, ExecutionException {
        Map<String, Object> values = new HashMap<>();
        values.put("robot.minDelayBetweenOrderExecutesMs", 0);
        values.put("test.symbols_list", "BTC-USDT,ETH-USDT,XRP-USDT,TBD-USDT");
        values.put("test.usd_balance", "100");
        values.put("test.executedQty", "1");
        ApplicationContext context = ApplicationContext.run(ApplicationContext.class, values);
        ApplicationEventPublisher eventPublisher = context.getBean(ApplicationEventPublisher.class);
        MessageListenerTestHelper messageListenerTestHelper = context.getBean(MessageListenerTestHelper.class);

        long currentTimeUtc = TimeUtils.getCurrentTimeUtc();
        eventPublisher.publishEvent(new RobotEvents.RobotStartEvent("USDT",new BigDecimal("0.8"),new BigDecimal("1.2"), 1, CandlestickInterval.TWELVE_HOURLY, currentTimeUtc + 1000, 3000000, new BigDecimal(30)));

        Future<CandlestickEvents.StartListenCandlesticksEvent> startListenEventFuture = messageListenerTestHelper.getEventFromQueue(CandlestickEvents.StartListenCandlesticksEvent.class);
        assertNotNull(startListenEventFuture.get(), "StartListenCandlesticksEvent wasn't published");

        CandlestickEvent candle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "100", "90", "100");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(candle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));

        messageListenerTestHelper.getQueue().clear();

        Future<RobotEvents.TryExecuteOrderEvent> tryExecuteOrderEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.TryExecuteOrderEvent.class);
        assertNotNull(tryExecuteOrderEventFuture.get(), "First TryExecuteOrderEvent wasn't published");

        messageListenerTestHelper.getQueue().clear();
        candle = createCandle(TimeUtils.getCurrentTimeUtc(), "BTC-USDT", "101", "91", "99");
        eventPublisher.publishEvent(new CandlestickEvents.BinanceCandlestickEvent(candle, new CandlestickEvents.ListenerKey("test", CandlestickInterval.TWELVE_HOURLY)));
        Future<RobotEvents.SellEvent> sellEventFuture = messageListenerTestHelper.getEventFromQueue(RobotEvents.SellEvent.class);
        try {
            RobotEvents.SellEvent sellEvent = sellEventFuture.get(1, TimeUnit.SECONDS);
            fail("Should be no sell event because only one candle was sent, but got one " + sellEvent);
        } catch (TimeoutException e) {

        }
    }

    private CandlestickEvent createCandle(long currentTimeUtc, String symbol, String close, String open, String low) {
        CandlestickEvent event = new CandlestickEvent();
        event.setSymbol(symbol);
        event.setQuoteAssetVolume("100000");
        event.setEventTime(currentTimeUtc);
        event.setCloseTime(currentTimeUtc);
        event.setClose(close);
        event.setOpen(open);
        event.setLow(low);
        return event;
    }
}