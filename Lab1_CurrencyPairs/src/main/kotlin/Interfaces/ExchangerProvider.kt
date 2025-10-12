package org.example.Interfaces

import org.example.Models.Currency
import org.example.Models.CurrencyPair
import java.math.BigDecimal

interface ExchangerProvider {
    fun getExchangeRate() : Map<CurrencyPair, BigDecimal>

    fun makeChange(user: UserProvider, baseCurrency: Currency, baseQuantity: BigDecimal, quotedCurrency : Currency)
}