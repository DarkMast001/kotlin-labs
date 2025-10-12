package org.example.Interfaces

import org.example.Models.Currency
import java.math.BigDecimal

interface UserProvider {
    fun decreaseCurrency(currency: Currency, quantity: BigDecimal)

    fun increaseCurrency(currency: Currency, quantity: BigDecimal)
}