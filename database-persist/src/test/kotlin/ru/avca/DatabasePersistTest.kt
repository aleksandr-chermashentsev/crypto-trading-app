package ru.avca

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.kotest.annotation.MicronautTest
import ru.avca.dbpersist.domain.CurrencyBalanceDomain
import ru.avca.dbpersist.domain.OpenPositionDomain
import ru.avca.dbpersist.domain.TradeDomain
import ru.avca.dbpersist.repository.CurrencyBalanceRepository
import ru.avca.dbpersist.repository.OpenPositionRepository
import ru.avca.dbpersist.repository.TradeRepository
import java.util.stream.Collectors.toList

@MicronautTest
class DatabasePersistTest(
    private val application: EmbeddedApplication<*>,
    private val tradeRepository: TradeRepository,
    private val currencyBalanceRepository: CurrencyBalanceRepository,
    private val openPositionRepository: OpenPositionRepository
) : StringSpec({

    "test the server is running" {
        assert(application.isRunning)
    }

    "test a trade is saving" {
        val tradeDomain = TradeDomain(null, 1, "btcusdt", "123", "432", "BUY")
        val savedDomain = tradeRepository.save(tradeDomain)

        savedDomain.id shouldNotBe null

        val foundDomain = tradeRepository.findById(savedDomain.id!!)

        foundDomain shouldNotBe null
        foundDomain shouldBe savedDomain
    }

    "test currency should merge repeated saves" {
        val firstCurrencyBalance = CurrencyBalanceDomain("test", "123")
        val secondCurrencyBalance = CurrencyBalanceDomain("test", "456")

        currencyBalanceRepository.saveCurrencyBalance(firstCurrencyBalance)

        var savedBalances = currencyBalanceRepository.getAllCurrencyBalances().collect(toList())

        savedBalances.size shouldBe 1
        savedBalances[0] shouldBe firstCurrencyBalance

        currencyBalanceRepository.saveCurrencyBalance(secondCurrencyBalance)

        savedBalances = currencyBalanceRepository.getAllCurrencyBalances().collect(toList())

        savedBalances.size shouldBe 1
        savedBalances[0] shouldBe secondCurrencyBalance
    }

    "test openPositionRepository should remove all prev positions before save new one" {
        val openPositionDomains1 = listOf(
            OpenPositionDomain("test1", "123", "456", "robot", 0),
            OpenPositionDomain("test2", "123", "567", "robot", 0)
        )

        val openPositionDomains2 = listOf(
            OpenPositionDomain("test3", "4848", "9499", "robot", 0),
            OpenPositionDomain("test4", "4020", "1038", "robot", 0)
        )

        openPositionRepository.updateOpenPositions(openPositionDomains1)
        var savedDomains = openPositionRepository.getAllOpenPositions("robot")
            .sorted(compareBy {it.symbol})
            .collect(toList())

        savedDomains.size shouldBe 2
        savedDomains shouldBe openPositionDomains1

        openPositionRepository.updateOpenPositions(openPositionDomains2)
        savedDomains = openPositionRepository.getAllOpenPositions("robot")
            .sorted(compareBy {it.symbol})
            .collect(toList())

        savedDomains.size shouldBe 2
        savedDomains shouldBe openPositionDomains2
    }
})
