package com.example.model

data class PantryItem(
    val barcode: String,
    val name: String,
    val brand: String,
    val imageUrl: String?,
    val quantity: String?,
    val addedAt: Long,
    val expirationDate: Long,
    val isConsumed: Boolean
)
