package org.example

fun main() {
    val data1 = Data("important string")
    val data2 = Data("other important string")

    val user1 = User("Ivan", 20, false, 1.333, listOf("Tom", "Sonya"), null, data1)
    val user2 = User("Dima", 22, true, 2.444, listOf("Ivan", "Alex"), user1, data2)
    val user3 = User("Alex", 24, true, 3.555, listOf("Egor", "Rus"), null, data1)

    val toml = TomlSerializer.serialize(user2)

    println(toml)

    val restored = TomlSerializer.deserialize<User>(toml)
    println("\nRestored: $restored")

    println("Equal? ${user2 == restored}")
}