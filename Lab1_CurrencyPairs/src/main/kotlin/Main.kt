package org.example

import org.example.Models.Currency
import java.math.BigDecimal
import java.math.RoundingMode

fun showMenu() {
    println("1. Show user's balance.")
    println("2. Change currency.")
    println("3. Show current currency rate.")
    println("4. Show that menu again.")
    println("0. Quiet.")
}

fun makeChange(exchanger: Exchanger, user: User) {
    print("Enter the currency code (ex. USD) you want to buy: ")
    val baseCurrencyCode = readLine()
    baseCurrencyCode ?: return

    print("Enter the quantity: ")
    val baseQuantityStr = readLine()
    baseQuantityStr ?: return

    print("Enter the currency code (ex. RUB) for which you want to buy: ")
    val quotedCurrencyCode = readLine()
    quotedCurrencyCode ?: return

    try {
        val base = Currency.fromSymbol(baseCurrencyCode.uppercase())
        val baseQuantity = BigDecimal(baseQuantityStr.toDouble()).setScale(3, RoundingMode.HALF_UP)
        val quoted = Currency.fromSymbol(quotedCurrencyCode.uppercase())

        exchanger.makeChange(user, base, baseQuantity, quoted)
        println("Success!")
    }
    catch (exception: Exception) {
        println("Error!")
        println("${exception.message}")
    }
}

fun main() {
    val user = User("RUB", "10000")
    val exchanger = Exchanger()

    showMenu()
    while (true) {
        print("> ")
        val str = readLine()
        str ?: break
        var point = 0
        try {
            point = str.toInt()
        }
        catch (e: NumberFormatException) {
            println("Enter number of point.")
            continue
        }

        when(point) {
            1 -> {
                for(currency in user.getCurrencies()) {
                    println("${currency.key} - ${currency.value}");
                }
            }
            2 -> makeChange(exchanger, user)
            3 -> {
                for(currencyPair in exchanger.getExchangeRate()) {
                    println("${currencyPair.key} - ${currencyPair.value}")
                }
            }
            4 -> showMenu()
            0 -> break
            else -> println("No such point.")
        }
    }
}