package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val brand: String,
    val imageUrl: String?,
    val quantity: String?,
    val addedAt: Long = System.currentTimeMillis(),
    val expirationDate: Long = System.currentTimeMillis() + (1000L * 60L * 60L * 24L * 14L), // default 14 days from now
    val isConsumed: Boolean = false
)
