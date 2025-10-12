package org.example.Models

import org.example.Exceptions.NoSuchCurrencyException

enum class Currency (val symbol: String) {
    EUR("EUR"),
    USD("USD"),
    RUB("RUB"),
    GBP("GBP"),
    JPY("JPY");

    companion object {
        fun fromSymbol(symbol: String): Currency = Currency.entries.find { it.symbol == symbol } ?: throw NoSuchCurrencyException("No currency type $symbol.")
    }
}