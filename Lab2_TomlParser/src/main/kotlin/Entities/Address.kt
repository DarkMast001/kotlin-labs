package org.example.Entities

import kotlinx.serialization.*

@Serializable
data class Address (
    val street: String,
    val city: String,
    val country: String
)