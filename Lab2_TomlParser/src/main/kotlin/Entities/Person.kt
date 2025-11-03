package org.example.Entities

import kotlinx.serialization.*

@Serializable
data class Person (
    val id: Int,
    val name: String,
    val age: Int,
    val isActive: Boolean,
    val score: Double,
    val tags: List<String>,
    val address: Address
)