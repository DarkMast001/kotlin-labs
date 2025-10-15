package org.example.Models

import org.example.Exceptions.NoSuchCurrencyException

enum class Currency (val symbol: String, val decimalPlaces: Int) {
    EUR("EUR", 2),
    USD("USD", 2),
    RUB("RUB", 2),
    GBP("GBP", 2),
    JPY("JPY", 0);

    companion object {
        fun fromSymbol(symbol: String): Currency = Currency.entries.find { it.symbol == symbol } ?: throw NoSuchCurrencyException("No currency type $symbol.")
    }
}