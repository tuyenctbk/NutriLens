package com.example.data

import com.example.data.local.ProductDao
import com.example.data.local.ScannedProductEntity
import com.example.model.DietStatus
import com.example.model.NutrimentsInfo
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.ProductDto
import com.example.network.NetworkClient
import com.example.network.OpenFoodFactsApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Repository interface defining the single source of truth contract
 * for food product data flow between the Open Food Facts API and local Room database.
 */
interface ProductRepository {
    /**
     * Flow of all scanned products in reverse chronological order.
     */
    val historyProducts: Flow<List<Product>>

    /**
     * Flow of user favorite products.
     */
    val favoriteProducts: Flow<List<Product>>

    /**
     * Flow of recent scanned products limited to specified count (default 20).
     */
    fun getRecentScanned(limit: Int = 20): Flow<List<Product>>

    /**
     * Reactive stream to fetch a product by its barcode.
     * Checks Room local cache first; fetches from Open Food Facts API on cache miss or forceRefresh.
     * Caches successful network results into Room database.
     */
    fun getProductFlow(barcode: String, forceRefresh: Boolean = false): Flow<Result<Product>>

    /**
     * Alias for getProductFlow to align with standard repository naming conventions.
     */
    fun getProductByBarcode(barcode: String, forceRefresh: Boolean = false): Flow<Result<Product>>

    /**
     * Suspending function to retrieve a product by barcode, checking Room cache and fetching/caching as needed.
     */
    suspend fun getProduct(barcode: String, forceRefresh: Boolean = false): Result<Product>

    /**
     * Toggle the favorite status of a product by barcode.
     */
    suspend fun toggleFavorite(barcode: String, isFavorite: Boolean)

    /**
     * Delete a scanned product record from the Room database.
     */
    suspend fun deleteScan(barcode: String)

    /**
     * Clear all scan history from the Room database.
     */
    suspend fun clearHistory()

    /**
     * Seed initial demo products into the Room cache for offline and preview experience.
     */
    suspend fun seedSampleScans()

    companion object {
        /**
         * Factory function allowing direct instantiation: ProductRepository(dao, apiService)
         */
        operator fun invoke(
            productDao: ProductDao,
            apiService: OpenFoodFactsApiService = NetworkClient.apiService,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ProductRepository = DefaultProductRepository(productDao, apiService, ioDispatcher)
    }
}

/**
 * Default implementation managing data flow between Open Food Facts API and Room database.
 * Follows the Offline-First Single Source of Truth architecture pattern.
 */
class DefaultProductRepository(
    private val productDao: ProductDao,
    private val apiService: OpenFoodFactsApiService = NetworkClient.apiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    override val historyProducts: Flow<List<Product>> = productDao.getAllScanned()
        .map { entities -> entities.map { it.toDomain(isOfflineCached = true) } }
        .flowOn(ioDispatcher)

    override val favoriteProducts: Flow<List<Product>> = productDao.getFavorites()
        .map { entities -> entities.map { it.toDomain(isOfflineCached = true) } }
        .flowOn(ioDispatcher)

    override fun getRecentScanned(limit: Int): Flow<List<Product>> = productDao.getRecentScanned(limit)
        .map { entities -> entities.map { it.toDomain(isOfflineCached = true) } }
        .flowOn(ioDispatcher)

    override fun getProductByBarcode(barcode: String, forceRefresh: Boolean): Flow<Result<Product>> {
        return getProductFlow(barcode, forceRefresh)
    }

    override fun getProductFlow(barcode: String, forceRefresh: Boolean): Flow<Result<Product>> = flow {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) {
            emit(Result.failure(IllegalArgumentException("Barcode cannot be blank")))
            return@flow
        }

        // 1. Check local Room database cache first
        val cachedEntity = productDao.getByBarcodeSync(cleanBarcode)
        if (cachedEntity != null && !forceRefresh) {
            emit(Result.success(cachedEntity.toDomain(isOfflineCached = true)))
            return@flow
        }

        // 2. Fetch from Open Food Facts API
        try {
            val response = apiService.getProductByBarcode(cleanBarcode)
            val body = response.body()
            if (response.isSuccessful && body?.status == 1 && body.product != null) {
                val dto = body.product
                val product = mapDtoToProduct(cleanBarcode, dto, cachedEntity?.isFavorite ?: false)

                // 3. Cache freshly fetched product into Room database
                productDao.insertOrReplace(product.toEntity())
                emit(Result.success(product.copy(isOfflineCached = false)))
            } else {
                // If network succeeded but product not found in Open Food Facts database
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
            // 4. Offline fallback on network exception
            if (cachedEntity != null) {
                emit(Result.success(cachedEntity.toDomain(isOfflineCached = true)))
            } else {
                val sample = DemoCatalog.getSample(cleanBarcode)
                if (sample != null) {
                    productDao.insertOrReplace(sample.toEntity())
                    emit(Result.success(sample))
                } else {
                    emit(Result.failure(Exception("Offline & not cached. Check internet connection. (${e.localizedMessage ?: "Network error"})")))
                }
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getProduct(barcode: String, forceRefresh: Boolean): Result<Product> = withContext(ioDispatcher) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Barcode cannot be blank"))
        }

        // 1. Check local Room database cache first
        val cachedEntity = productDao.getByBarcodeSync(cleanBarcode)
        if (cachedEntity != null && !forceRefresh) {
            return@withContext Result.success(cachedEntity.toDomain(isOfflineCached = true))
        }

        // 2. Fetch from Open Food Facts API
        try {
            val response = apiService.getProductByBarcode(cleanBarcode)
            val body = response.body()
            if (response.isSuccessful && body?.status == 1 && body.product != null) {
                val dto = body.product
                val product = mapDtoToProduct(cleanBarcode, dto, cachedEntity?.isFavorite ?: false)

                // 3. Cache freshly fetched product into Room database
                productDao.insertOrReplace(product.toEntity())
                Result.success(product.copy(isOfflineCached = false))
            } else {
                if (cachedEntity != null) {
                    Result.success(cachedEntity.toDomain(isOfflineCached = true))
                } else {
                    val sample = DemoCatalog.getSample(cleanBarcode)
                    if (sample != null) {
                        productDao.insertOrReplace(sample.toEntity())
                        Result.success(sample)
                    } else {
                        Result.failure(Exception("Product not found in Open Food Facts database (Barcode: $cleanBarcode)"))
                    }
                }
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                Result.success(cachedEntity.toDomain(isOfflineCached = true))
            } else {
                val sample = DemoCatalog.getSample(cleanBarcode)
                if (sample != null) {
                    productDao.insertOrReplace(sample.toEntity())
                    Result.success(sample)
                } else {
                    Result.failure(Exception("Offline & not cached. Check internet connection. (${e.localizedMessage ?: "Network error"})"))
                }
            }
        }
    }

    override suspend fun toggleFavorite(barcode: String, isFavorite: Boolean) = withContext(ioDispatcher) {
        productDao.updateFavorite(barcode, isFavorite)
    }

    override suspend fun deleteScan(barcode: String) = withContext(ioDispatcher) {
        productDao.deleteByBarcode(barcode)
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        productDao.clearAll()
    }

    override suspend fun seedSampleScans() = withContext(ioDispatcher) {
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

        // Nutriscore & Ecoscore
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
