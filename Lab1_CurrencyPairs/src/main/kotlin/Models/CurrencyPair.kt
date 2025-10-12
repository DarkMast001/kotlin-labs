package org.example.Models

import org.example.Exceptions.UnsupportedCurrencyPairException

data class CurrencyPair private constructor(val base: Currency, val quote: Currency){
    override fun toString(): String = "${base.symbol}/${quote.symbol}"

    companion object {
        fun create(baseSymbol: String, quoteSymbol: String): CurrencyPair {
            if (baseSymbol.uppercase() == quoteSymbol.uppercase()) throw UnsupportedCurrencyPairException("Wrong symbols")

            val base = Currency.fromSymbol(baseSymbol.uppercase())
            val quote = Currency.fromSymbol(quoteSymbol.uppercase())

            return create(base, quote)
        }

        fun create(baseSymbol: Currency, quoteSymbol: Currency): CurrencyPair {
            return CurrencyPair(baseSymbol, quoteSymbol)
        }
    }
}