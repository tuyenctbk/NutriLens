package com.example.data

import com.example.data.local.ProductDao
import com.example.data.local.ScannedProductEntity
import com.example.model.DietStatus
import com.example.model.NutrimentsDto
import com.example.model.NutrimentsInfo
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.ProductDto
import com.example.network.NetworkClient
import com.example.network.OpenFoodFactsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

class ProductRepository(
    private val productDao: ProductDao,
    private val apiService: OpenFoodFactsApiService = NetworkClient.apiService
) {

    val historyProducts: Flow<List<Product>> = productDao.getAllScanned()
        .map { entities -> entities.map { it.toDomain(isOfflineCached = true) } }
        .flowOn(Dispatchers.IO)

    val favoriteProducts: Flow<List<Product>> = productDao.getFavorites()
        .map { entities -> entities.map { it.toDomain(isOfflineCached = true) } }
        .flowOn(Dispatchers.IO)

    fun getProductFlow(barcode: String, forceRefresh: Boolean = false): Flow<Result<Product>> = flow {
        val cleanBarcode = barcode.trim()

        // 1. Check local Room cache first
        val cachedEntity = productDao.getByBarcodeSync(cleanBarcode)
        if (cachedEntity != null && !forceRefresh) {
            emit(Result.success(cachedEntity.toDomain(isOfflineCached = true)))
            return@flow
        }

        // 2. Fetch from Open Food Facts API
        try {
            val response = apiService.getProductByBarcode(cleanBarcode)
            if (response.isSuccessful && response.body()?.status == 1 && response.body()?.product != null) {
                val dto = response.body()!!.product!!
                val product = mapDtoToProduct(cleanBarcode, dto, cachedEntity?.isFavorite ?: false)
                
                // Save to Room cache
                productDao.insertOrReplace(product.toEntity())
                emit(Result.success(product.copy(isOfflineCached = false)))
            } else {
                // If network succeeded but product not in OFF database
                if (cachedEntity != null) {
                    emit(Result.success(cachedEntity.toDomain(isOfflineCached = true)))
                } else {
                    // Check fallback demo catalog (guarantees offline/sample demo usability)
                    val sample = DemoCatalog.getSample(cleanBarcode)
                    if (sample != null) {
                        productDao.insertOrReplace(sample.toEntity())
                        emit(Result.success(sample))
                    } else {
                        emit(Result.failure(Exception("Product not found in Open Food Facts database (Barcode: $cleanBarcode)")))
                    }
                }
            }
        } catch (e: Exception) {
            // Offline fallback
            if (cachedEntity != null) {
                emit(Result.success(cachedEntity.toDomain(isOfflineCached = true)))
            } else {
                val sample = DemoCatalog.getSample(cleanBarcode)
                if (sample != null) {
                    productDao.insertOrReplace(sample.toEntity())
                    emit(Result.success(sample))
                } else {
                    emit(Result.failure(Exception("Offline & not cached. Check internet connection. (${e.localizedMessage})")))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(barcode: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        productDao.updateFavorite(barcode, isFavorite)
    }

    suspend fun deleteScan(barcode: String) = withContext(Dispatchers.IO) {
        productDao.deleteByBarcode(barcode)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        productDao.clearAll()
    }

    suspend fun seedSampleScans() = withContext(Dispatchers.IO) {
        DemoCatalog.samples.forEachIndexed { index, sampleItem ->
            val product = DemoCatalog.getSample(sampleItem.barcode)
            if (product != null) {
                // Stagger scannedAt timestamps slightly so they have varied recent scan times
                val staggeredTime = System.currentTimeMillis() - (index * 1000L * 60L * 35L)
                productDao.insertOrReplace(product.copy(scannedAt = staggeredTime).toEntity())
            }
        }
    }

    private fun mapDtoToProduct(barcode: String, dto: ProductDto, isFavorite: Boolean): Product {
        val name = dto.productName?.takeIf { it.isNotBlank() }
            ?: dto.productNameEn?.takeIf { it.isNotBlank() }
            ?: "Unnamed Food Product"
        val brand = dto.brands?.takeIf { it.isNotBlank() } ?: "Brand Unknown"
        val imageUrl = dto.imageFrontUrl ?: dto.imageUrl ?: dto.imageFrontSmallUrl

        // Nutriscore
        val nutriscore = dto.nutriscoreGrade?.lowercase(Locale.ROOT)?.takeIf { it in listOf("a", "b", "c", "d", "e") }
        val ecoscore = dto.ecoscoreGrade?.lowercase(Locale.ROOT)?.takeIf { it in listOf("a", "b", "c", "d", "e") }

        // Additives resolution
        val rawAdditiveTags = (dto.additivesTags ?: emptyList()) + (dto.additivesOriginalTags ?: emptyList())
        val additives = rawAdditiveTags.distinct().map { AdditivesDatabase.resolve(it) }

        // Allergens
        val rawAllergens = (dto.allergensTags ?: emptyList()) + (dto.allergensHierarchy ?: emptyList())
        val allergens = rawAllergens.distinct().map { formatTag(it) }.filter { it.isNotBlank() }

        // Traces
        val traces = (dto.tracesTags ?: emptyList()).distinct().map { formatTag(it) }.filter { it.isNotBlank() }

        // Palm Oil Analysis
        val analysis = dto.ingredientsAnalysisTags ?: emptyList()
        val ingredientsText = dto.ingredientsText ?: dto.ingredientsTextEn
        val ingredientsLower = (ingredientsText ?: "").lowercase(Locale.ROOT)

        val palmOilStatus = when {
            analysis.any { it.contains("palm-oil-free") } -> PalmOilStatus.FREE
            analysis.any { it.contains("may-contain-palm-oil") } -> PalmOilStatus.MAY_CONTAIN
            analysis.any { it.contains("palm-oil") } -> PalmOilStatus.CONTAINS
            ingredientsLower.contains("palm oil") || ingredientsLower.contains("palmolein") || ingredientsLower.contains("palm kernel") -> PalmOilStatus.CONTAINS
            else -> PalmOilStatus.UNKNOWN
        }

        // Vegan & Vegetarian
        val veganStatus = when {
            analysis.any { it == "en:vegan" } -> DietStatus.SUITABLE
            analysis.any { it == "en:non-vegan" } -> DietStatus.NOT_SUITABLE
            analysis.any { it == "en:vegan-status-unknown" || it.contains("maybe-vegan") } -> DietStatus.MAYBE
            else -> DietStatus.UNKNOWN
        }

        val vegetarianStatus = when {
            analysis.any { it == "en:vegetarian" } -> DietStatus.SUITABLE
            analysis.any { it == "en:non-vegetarian" } -> DietStatus.NOT_SUITABLE
            analysis.any { it == "en:vegetarian-status-unknown" || it.contains("maybe-vegetarian") } -> DietStatus.MAYBE
            else -> DietStatus.UNKNOWN
        }

        // Nutriments
        val nutriments = NutrimentsInfo(
            energyKcal = dto.nutriments?.energyKcal100g ?: dto.nutriments?.energy100g?.let { it / 4.184 },
            fat = dto.nutriments?.fat100g,
            saturatedFat = dto.nutriments?.saturatedFat100g,
            carbs = dto.nutriments?.carbohydrates100g,
            sugars = dto.nutriments?.sugars100g,
            fiber = dto.nutriments?.fiber100g,
            proteins = dto.nutriments?.proteins100g,
            salt = dto.nutriments?.salt100g ?: dto.nutriments?.sodium100g?.let { it * 2.5 }
        )

        return Product(
            barcode = barcode,
            name = name,
            brand = brand,
            imageUrl = imageUrl,
            nutriscoreGrade = nutriscore,
            novaGroup = dto.novaGroup,
            ecoscoreGrade = ecoscore,
            ingredientsText = ingredientsText,
            additives = additives,
            allergens = allergens,
            traces = traces,
            palmOilStatus = palmOilStatus,
            veganStatus = veganStatus,
            vegetarianStatus = vegetarianStatus,
            nutriments = nutriments,
            quantity = dto.quantity,
            servingSize = dto.servingSize,
            isFavorite = isFavorite
        )
    }

    private fun formatTag(tag: String): String {
        return tag.removePrefix("en:")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun ScannedProductEntity.toDomain(isOfflineCached: Boolean): Product {
        val additiveList = additivesRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { AdditivesDatabase.resolve(it) }

        val allergenList = allergensRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val traceList = tracesRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return Product(
            barcode = barcode,
            name = name,
            brand = brand,
            imageUrl = imageUrl,
            nutriscoreGrade = nutriscoreGrade,
            novaGroup = novaGroup,
            ecoscoreGrade = ecoscoreGrade,
            ingredientsText = ingredientsText,
            additives = additiveList,
            allergens = allergenList,
            traces = traceList,
            palmOilStatus = try { PalmOilStatus.valueOf(palmOilStatus) } catch (_: Exception) { PalmOilStatus.UNKNOWN },
            veganStatus = try { DietStatus.valueOf(veganStatus) } catch (_: Exception) { DietStatus.UNKNOWN },
            vegetarianStatus = try { DietStatus.valueOf(vegetarianStatus) } catch (_: Exception) { DietStatus.UNKNOWN },
            nutriments = NutrimentsInfo(
                energyKcal = energyKcal,
                fat = fat,
                saturatedFat = saturatedFat,
                carbs = carbs,
                sugars = sugars,
                fiber = fiber,
                proteins = proteins,
                salt = salt
            ),
            quantity = quantity,
            servingSize = servingSize,
            scannedAt = scannedAt,
            isFavorite = isFavorite,
            isOfflineCached = isOfflineCached
        )
    }

    private fun Product.toEntity(): ScannedProductEntity {
        return ScannedProductEntity(
            barcode = barcode,
            name = name,
            brand = brand,
            imageUrl = imageUrl,
            nutriscoreGrade = nutriscoreGrade,
            novaGroup = novaGroup,
            ecoscoreGrade = ecoscoreGrade,
            ingredientsText = ingredientsText,
            additivesRaw = additives.joinToString(",") { it.code },
            allergensRaw = allergens.joinToString(","),
            tracesRaw = traces.joinToString(","),
            palmOilStatus = palmOilStatus.name,
            veganStatus = veganStatus.name,
            vegetarianStatus = vegetarianStatus.name,
            energyKcal = nutriments.energyKcal,
            fat = nutriments.fat,
            saturatedFat = nutriments.saturatedFat,
            carbs = nutriments.carbs,
            sugars = nutriments.sugars,
            fiber = nutriments.fiber,
            proteins = nutriments.proteins,
            salt = nutriments.salt,
            quantity = quantity,
            servingSize = servingSize,
            scannedAt = scannedAt,
            isFavorite = isFavorite
        )
    }
}
