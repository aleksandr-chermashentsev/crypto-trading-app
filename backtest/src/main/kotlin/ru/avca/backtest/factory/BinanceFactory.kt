package ru.avca.backtest.factory

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiRestClient
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import javax.inject.Singleton

@Factory
class BinanceFactory {
    @Singleton
    fun binanceApiClientFactory(
        @Value("\${exchange.binance.api.key}") apiKey: String,
        @Value("\${exchange.binance.api.secret}") apiSecret: String
    ): BinanceApiClientFactory {
        return BinanceApiClientFactory.newInstance(apiKey.trim { it <= ' ' }, apiSecret.trim { it <= ' ' })
    }

    @Singleton
    fun binanceAsyncApi(binanceApiClientFactory: BinanceApiClientFactory): BinanceApiAsyncRestClient {
        return binanceApiClientFactory.newAsyncRestClient()
    }

    @Singleton
    fun binanceSyncApi(binanceApiClientFactory: BinanceApiClientFactory): BinanceApiRestClient {
        return binanceApiClientFactory.newRestClient();
    }
}