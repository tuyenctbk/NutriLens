package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY expirationDate ASC")
    fun getAllPantryItems(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcodeSync(barcode: String): PantryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: PantryItemEntity)

    @Query("UPDATE pantry_items SET isConsumed = :isConsumed WHERE barcode = :barcode")
    suspend fun updateConsumed(barcode: String, isConsumed: Boolean)

    @Query("UPDATE pantry_items SET expirationDate = :expirationDate WHERE barcode = :barcode")
    suspend fun updateExpiration(barcode: String, expirationDate: Long)

    @Query("DELETE FROM pantry_items WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM pantry_items")
    suspend fun clearPantry()
}
