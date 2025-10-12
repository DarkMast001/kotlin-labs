package org.example

import org.example.Interfaces.ExchangerProvider
import org.example.Interfaces.UserProvider
import org.example.Models.Currency
import org.example.Models.CurrencyPair
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

class Exchanger : ExchangerProvider {
    private val currencyPairs = mutableMapOf(
        CurrencyPair.create("USD", "RUB") to BigDecimal("1.0"),
        CurrencyPair.create("EUR", "RUB") to BigDecimal("1.0"),
        CurrencyPair.create("EUR", "USD") to BigDecimal("1.0")
    )
    private val random = Random.Default

    init {
        changeValues()
    }

    override fun getExchangeRate() : Map<CurrencyPair, BigDecimal> = currencyPairs.toMap()

    override fun makeChange(user: UserProvider, baseCurrency: Currency, baseQuantity: BigDecimal, quotedCurrency : Currency) {
        val currencyPairAmount1 = currencyPairs[CurrencyPair.create(baseCurrency.symbol, quotedCurrency.symbol)]
        val currencyPairAmount2 = currencyPairs[CurrencyPair.create(quotedCurrency.symbol, baseCurrency.symbol)]

        if (currencyPairAmount1 != null) {
            val quotedQuantity = baseQuantity.multiply(currencyPairAmount1)

            user.decreaseCurrency(quotedCurrency, quotedQuantity)
            user.increaseCurrency(baseCurrency, baseQuantity)
        }
        else if (currencyPairAmount2 != null) {
            val quotedQuantity = baseQuantity.divide(currencyPairAmount2, 3, RoundingMode.HALF_UP)

            user.decreaseCurrency(quotedCurrency, quotedQuantity)
            user.increaseCurrency(baseCurrency, baseQuantity)
        }
        else {
            throw Exception("There is no suitable currency pair.")
        }

        changeValues()
    }

    private fun changeValues() {
        for (currencyPair in currencyPairs) {
            val rate = random.nextDouble(50.0, 150.0)
            currencyPair.setValue(BigDecimal(rate.toString()).setScale(3, RoundingMode.HALF_UP))
        }
    }
}