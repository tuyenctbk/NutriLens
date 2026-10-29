package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_products")
data class ScannedProductEntity(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val brand: String,
    val imageUrl: String?,
    val nutriscoreGrade: String?,
    val novaGroup: Int?,
    val ecoscoreGrade: String?,
    val ingredientsText: String?,
    val additivesRaw: String, // Comma-separated E-codes e.g. "E322,E330"
    val allergensRaw: String, // Comma-separated e.g. "Milk,Soybeans"
    val tracesRaw: String,
    val palmOilStatus: String, // FREE, CONTAINS, MAY_CONTAIN, UNKNOWN
    val veganStatus: String,
    val vegetarianStatus: String,
    val energyKcal: Double?,
    val fat: Double?,
    val saturatedFat: Double?,
    val carbs: Double?,
    val sugars: Double?,
    val fiber: Double?,
    val proteins: Double?,
    val salt: Double?,
    val quantity: String?,
    val servingSize: String?,
    val scannedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
