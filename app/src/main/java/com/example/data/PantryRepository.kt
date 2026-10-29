package com.example.data

import com.example.data.local.PantryDao
import com.example.data.local.PantryItemEntity
import com.example.model.PantryItem
import com.example.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PantryRepository(private val pantryDao: PantryDao) {

    val pantryItemsFlow: Flow<List<PantryItem>> = pantryDao.getAllPantryItems()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun addProductToPantry(product: Product, expirationDate: Long = System.currentTimeMillis() + (1000L * 60L * 60L * 24L * 14L)) = withContext(Dispatchers.IO) {
        val existing = pantryDao.getByBarcodeSync(product.barcode)
        val entity = PantryItemEntity(
            barcode = product.barcode,
            name = product.name,
            brand = product.brand,
            imageUrl = product.imageUrl,
            quantity = product.quantity,
            addedAt = existing?.addedAt ?: System.currentTimeMillis(),
            expirationDate = existing?.expirationDate ?: expirationDate,
            isConsumed = existing?.isConsumed ?: false
        )
        pantryDao.insertOrReplace(entity)
    }

    suspend fun updateConsumed(barcode: String, isConsumed: Boolean) = withContext(Dispatchers.IO) {
        pantryDao.updateConsumed(barcode, isConsumed)
    }

    suspend fun updateExpiration(barcode: String, expirationDate: Long) = withContext(Dispatchers.IO) {
        pantryDao.updateExpiration(barcode, expirationDate)
    }

    suspend fun deleteItem(barcode: String) = withContext(Dispatchers.IO) {
        pantryDao.deleteByBarcode(barcode)
    }

    suspend fun clearPantry() = withContext(Dispatchers.IO) {
        pantryDao.clearPantry()
    }

    suspend fun seedSamplePantry() = withContext(Dispatchers.IO) {
        val samples = DemoCatalog.samples
        val now = System.currentTimeMillis()
        val day = 1000L * 60L * 60L * 24L

        samples.forEachIndexed { index, sample ->
            val product = DemoCatalog.getSample(sample.barcode)
            if (product != null) {
                // Stagger expiration dates: some expired, some expiring soon, some fresh
                val expOffset = when (index) {
                    0 -> -day * 2 // Expired 2 days ago
                    1 -> day * 1  // Expiring tomorrow
                    2 -> day * 3  // Expiring in 3 days
                    else -> day * 20 // Fresh
                }
                val entity = PantryItemEntity(
                    barcode = product.barcode,
                    name = product.name,
                    brand = product.brand,
                    imageUrl = product.imageUrl,
                    quantity = product.quantity,
                    addedAt = now - (index * day * 2),
                    expirationDate = now + expOffset,
                    isConsumed = index == 5 // Last one consumed
                )
                pantryDao.insertOrReplace(entity)
            }
        }
    }

    private fun PantryItemEntity.toDomain(): PantryItem {
        return PantryItem(
            barcode = barcode,
            name = name,
            brand = brand,
            imageUrl = imageUrl,
            quantity = quantity,
            addedAt = addedAt,
            expirationDate = expirationDate,
            isConsumed = isConsumed
        )
    }
}
