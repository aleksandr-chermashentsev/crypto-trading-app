package ru.avca.robot;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.general.*;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.google.common.collect.Lists;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import ru.avca.robot.factory.BinanceFactory;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

/**
 * @author a.chermashentsev
 * Date: 05.04.2021
 **/
@Factory
public class TestFactory {

    @Singleton
    @Replaces(value = ManagedChannel.class)
    public ManagedChannel eventHubFutureStub() {
        ManagedChannel mock = mock(ManagedChannel.class);
        when(mock.newCall(any(MethodDescriptor.class), any(CallOptions.class))).thenReturn(mock(ClientCall.class));
        return mock;
    }
    @Singleton
    @Replaces(value = BinanceApiClientFactory.class, factory = BinanceFactory.class)
    public BinanceApiClientFactory binanceApiClientFactory(
            @Value("${test.send_binance_events:false}") boolean sendBinanceEvent,
            @Value("${test.send_exception:false}") boolean sendException,
            @Value("${test.send_binance_events_interval_ms:100}") int sendEventsIntervalMs,
            @Value("${test.send_exception_count:1}") int sendExceptionCount,
            @Value("${test.symbols_list:}") String symbols,
            @Value("${test.usd_balance:}") String usdtBalance,
            @Value("${test.executedQty:100}") String executedQty
            ) {
        BinanceApiClientFactory mock = spy(BinanceApiClientFactory.newInstance());
        BinanceApiRestClient restClientMock = mock(BinanceApiRestClient.class);
        when(mock.newRestClient()).thenReturn(restClientMock);
        List<String> symbolsList;
        if (symbols.isEmpty()) {
            symbolsList = Lists.newArrayList();
        }
        else {
            symbolsList = Arrays.stream(symbols.split(",")).collect(Collectors.toList());
        }
        if (usdtBalance.isEmpty()) {
            usdtBalance = "20";
        }
        setupRestClientMock(restClientMock, symbolsList, usdtBalance, executedQty);
        if (sendBinanceEvent || sendException) {
            when(mock.newWebSocketClient()).thenReturn(new BinanceApiWebSocketClientMock(sendEventsIntervalMs, sendBinanceEvent, sendExceptionCount, sendException));
        }
        else {
            BinanceApiWebSocketClient clientMock = mock(BinanceApiWebSocketClient.class);
            when(clientMock.onCandlestickEvent(anyString(), any(CandlestickInterval.class), any(BinanceApiCallback.class)))
                    .thenReturn(() -> {
                    });
            when(mock.newWebSocketClient()).thenReturn(clientMock);
        }
        return mock;
    }

    private void setupRestClientMock(BinanceApiRestClient restClientMock, List<String> symbolsList, String usdtBalance, String executedQty) {
        ExchangeInfo exchangeInfoMock = mock(ExchangeInfo.class);
        List<SymbolInfo> symbolInfos = symbolsList.stream()
                .map(symbol -> {
                    SymbolInfo symbolInfoMock = mock(SymbolInfo.class);
                    SymbolFilter filter = new SymbolFilter();
                    filter.setFilterType(FilterType.LOT_SIZE);
                    filter.setStepSize("0.001");
                    when(symbolInfoMock.getSymbol()).thenReturn(symbol);
                    when(symbolInfoMock.getQuoteAsset()).thenReturn(symbol.split("-")[1]);
                    when(symbolInfoMock.getStatus()).thenReturn(SymbolStatus.TRADING);
                    when(symbolInfoMock.getFilters()).thenReturn(Lists.newArrayList(filter));

                    return symbolInfoMock;
                })
                .collect(Collectors.toList());
        when(restClientMock.getExchangeInfo()).thenReturn(exchangeInfoMock);
        when(exchangeInfoMock.getSymbols()).thenReturn(symbolInfos);

        NewOrderResponse newOrderResponse = new NewOrderResponse();
        newOrderResponse.setExecutedQty(executedQty);
        newOrderResponse.setCummulativeQuoteQty(usdtBalance);
        when(restClientMock.newOrder(any(NewOrder.class)))
                .thenReturn(newOrderResponse);
    }

    @Singleton
    @Replaces
    public BinanceApiClientFactory binanceApiClientFactory() {
        return mock(BinanceApiClientFactory.class);
    }
}
