package org.example

import org.example.Exceptions.InsufficientFundsException
import org.example.Exceptions.NoSuchCurrencyException
import org.example.Interfaces.UserProvider
import org.example.Models.Currency
import java.math.BigDecimal
import java.math.RoundingMode

class User(base: String, baseQuantity: String) : UserProvider {
    private val base: Currency = Currency.fromSymbol(base)
    private val baseQuantity: BigDecimal = BigDecimal(baseQuantity).setScale(3, RoundingMode.HALF_UP)
    private val currenciesMap: MutableMap<Currency, BigDecimal> = mutableMapOf(this.base to this.baseQuantity)

    fun getCurrencies() : Map<Currency, BigDecimal> = currenciesMap.toMap()

    override fun decreaseCurrency(currency: Currency, quantity: BigDecimal) {
        val currentBalance = currenciesMap[currency] ?: throw NoSuchCurrencyException("No currency ${currency.symbol}.")

        val newBalance = currentBalance.minus(quantity)

        if (newBalance < BigDecimal.ZERO) {
            throw InsufficientFundsException("Insufficient funds in ${currency.symbol}.")
        }

        if (newBalance == BigDecimal.ZERO) {
            currenciesMap.remove(currency)
            return
        }

        currenciesMap[currency] = newBalance
    }

    override fun increaseCurrency(currency: Currency, quantity: BigDecimal) {
        val currentBalance = currenciesMap[currency]

        if (currentBalance == null) {
            currenciesMap[currency] = quantity
            return
        }

        val newBalance = currentBalance.plus(quantity)

        currenciesMap[currency] = newBalance
    }
}
