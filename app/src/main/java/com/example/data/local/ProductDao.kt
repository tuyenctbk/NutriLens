package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM scanned_products ORDER BY scannedAt DESC")
    fun getAllScanned(): Flow<List<ScannedProductEntity>>

    @Query("SELECT * FROM scanned_products ORDER BY scannedAt DESC LIMIT :limit")
    fun getRecentScanned(limit: Int = 20): Flow<List<ScannedProductEntity>>

    @Query("SELECT * FROM scanned_products WHERE isFavorite = 1 ORDER BY scannedAt DESC")
    fun getFavorites(): Flow<List<ScannedProductEntity>>

    @Query("SELECT * FROM scanned_products WHERE barcode = :barcode LIMIT 1")
    fun getByBarcode(barcode: String): Flow<ScannedProductEntity?>

    @Query("SELECT * FROM scanned_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcodeSync(barcode: String): ScannedProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: ScannedProductEntity)

    @Query("UPDATE scanned_products SET isFavorite = :isFavorite WHERE barcode = :barcode")
    suspend fun updateFavorite(barcode: String, isFavorite: Boolean)

    @Query("DELETE FROM scanned_products WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM scanned_products")
    suspend fun clearAll()
}
