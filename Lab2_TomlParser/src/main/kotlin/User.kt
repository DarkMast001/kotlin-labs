package org.example

data class User (
    val name: String,
    val age: Int,
    val isAdmin: Boolean,
    val spending: Double,
    val colleagues: List<String>,
    val otherUser: User?,
    val data: Data
)