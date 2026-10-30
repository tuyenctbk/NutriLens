package com.example

import com.example.data.DefaultProductRepository
import com.example.data.ProductRepository
import com.example.data.local.ProductDao
import com.example.data.local.ScannedProductEntity
import com.example.model.OpenFoodFactsResponse
import com.example.model.ProductDto
import com.example.network.OpenFoodFactsApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryTest {

    private lateinit var fakeDao: FakeProductDao
    private lateinit var fakeApi: FakeOpenFoodFactsApiService
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ProductRepository

    @Before
    fun setup() {
        fakeDao = FakeProductDao()
        fakeApi = FakeOpenFoodFactsApiService()
        repository = DefaultProductRepository(
            productDao = fakeDao,
            apiService = fakeApi,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `when product is in Room cache and forceRefresh is false, returns cached product without network call`() = runTest(testDispatcher) {
        val barcode = "123456789"
        val cached = ScannedProductEntity(
            barcode = barcode,
            name = "Cached Snack",
            brand = "HealthyBrand",
            imageUrl = "http://image.url",
            nutriscoreGrade = "a",
            novaGroup = 1,
            ecoscoreGrade = "b",
            ingredientsText = "Whole oats",
            additivesRaw = "",
            allergensRaw = "Oats",
            tracesRaw = "",
            palmOilStatus = "FREE",
            veganStatus = "SUITABLE",
            vegetarianStatus = "SUITABLE",
            energyKcal = 150.0,
            fat = 2.0,
            saturatedFat = 0.5,
            carbs = 25.0,
            sugars = 1.0,
            fiber = 4.0,
            proteins = 5.0,
            salt = 0.1,
            quantity = "100g",
            servingSize = "50g",
            scannedAt = 1000L,
            isFavorite = true
        )
        fakeDao.insertOrReplace(cached)

        // API should not be called
        fakeApi.shouldThrowNetworkError = true

        val result = repository.getProduct(barcode, forceRefresh = false)
        assertTrue(result.isSuccess)
        val product = result.getOrNull()
        assertNotNull(product)
        assertEquals("Cached Snack", product?.name)
        assertTrue("isOfflineCached flag should be true for cache hit", product?.isOfflineCached == true)
        assertTrue(product?.isFavorite == true)
        assertEquals(0, fakeApi.callCount)
    }

    @Test
    fun `when product not in Room cache, fetches from Open Food Facts API and caches in Room`() = runTest(testDispatcher) {
        val barcode = "987654321"
        fakeApi.stubbedResponse = Response.success(
            OpenFoodFactsResponse(
                status = 1,
                code = barcode,
                product = ProductDto(
                    code = barcode,
                    productName = "Organic Dark Chocolate",
                    brands = "GreenChoc",
                    nutriscoreGrade = "c",
                    novaGroup = 3,
                    allergensTags = listOf("en:milk", "en:soybeans"),
                    additivesTags = listOf("en:e322"),
                    ingredientsAnalysisTags = listOf("en:palm-oil-free", "en:vegetarian")
                )
            )
        )

        val result = repository.getProduct(barcode, forceRefresh = false)
        assertTrue(result.isSuccess)
        val product = result.getOrNull()
        assertNotNull(product)
        assertEquals("Organic Dark Chocolate", product?.name)
        assertEquals("GreenChoc", product?.brand)
        assertFalse(product?.isOfflineCached ?: true)
        assertEquals(1, fakeApi.callCount)

        // Verify it was persisted to Room DAO
        val inRoom = fakeDao.getByBarcodeSync(barcode)
        assertNotNull("Product must be saved into Room database cache", inRoom)
        assertEquals("Organic Dark Chocolate", inRoom?.name)
        assertEquals("E322", inRoom?.additivesRaw)
    }

    @Test
    fun `forceRefresh queries API and updates Room cache even if already in cache`() = runTest(testDispatcher) {
        val barcode = "555666777"
        val oldCached = ScannedProductEntity(
            barcode = barcode,
            name = "Old Name",
            brand = "Old Brand",
            imageUrl = null,
            nutriscoreGrade = null,
            novaGroup = null,
            ecoscoreGrade = null,
            ingredientsText = null,
            additivesRaw = "",
            allergensRaw = "",
            tracesRaw = "",
            palmOilStatus = "UNKNOWN",
            veganStatus = "UNKNOWN",
            vegetarianStatus = "UNKNOWN",
            energyKcal = null,
            fat = null,
            saturatedFat = null,
            carbs = null,
            sugars = null,
            fiber = null,
            proteins = null,
            salt = null,
            quantity = null,
            servingSize = null,
            isFavorite = true // User marked as favorite
        )
        fakeDao.insertOrReplace(oldCached)

        // API returns updated product info
        fakeApi.stubbedResponse = Response.success(
            OpenFoodFactsResponse(
                status = 1,
                product = ProductDto(
                    productName = "Refreshed Name",
                    brands = "Updated Brand"
                )
            )
        )

        val result = repository.getProduct(barcode, forceRefresh = true)
        assertTrue(result.isSuccess)
        assertEquals("Refreshed Name", result.getOrNull()?.name)
        assertEquals(1, fakeApi.callCount)

        // Verify Room was updated while retaining favorite flag
        val updatedInRoom = fakeDao.getByBarcodeSync(barcode)
        assertEquals("Refreshed Name", updatedInRoom?.name)
        assertTrue("User's local favorite status should be retained upon refresh", updatedInRoom?.isFavorite == true)
    }

    @Test
    fun `network error falls back to cached Room entity if present`() = runTest(testDispatcher) {
        val barcode = "444333222"
        fakeDao.insertOrReplace(
            ScannedProductEntity(
                barcode = barcode,
                name = "Emergency Apple Juice",
                brand = "Orchard",
                imageUrl = null,
                nutriscoreGrade = "b",
                novaGroup = 1,
                ecoscoreGrade = null,
                ingredientsText = "Apples",
                additivesRaw = "",
                allergensRaw = "",
                tracesRaw = "",
                palmOilStatus = "FREE",
                veganStatus = "SUITABLE",
                vegetarianStatus = "SUITABLE",
                energyKcal = 45.0,
                fat = 0.0,
                saturatedFat = 0.0,
                carbs = 11.0,
                sugars = 10.0,
                fiber = 0.5,
                proteins = 0.1,
                salt = 0.01,
                quantity = "1L",
                servingSize = "200ml"
            )
        )

        // Force refresh triggers network call, which throws
        fakeApi.shouldThrowNetworkError = true

        val flowResult = repository.getProductFlow(barcode, forceRefresh = true).first()
        assertTrue("Should fallback to cache on network error", flowResult.isSuccess)
        assertEquals("Emergency Apple Juice", flowResult.getOrNull()?.name)
        assertTrue(flowResult.getOrNull()?.isOfflineCached == true)
    }

    @Test
    fun `toggle favorite updates Room DAO correctly`() = runTest(testDispatcher) {
        val barcode = "111222333"
        fakeDao.insertOrReplace(
            ScannedProductEntity(
                barcode = barcode,
                name = "Yogurt",
                brand = "Dairy",
                imageUrl = null,
                nutriscoreGrade = "a",
                novaGroup = 1,
                ecoscoreGrade = null,
                ingredientsText = null,
                additivesRaw = "",
                allergensRaw = "Milk",
                tracesRaw = "",
                palmOilStatus = "FREE",
                veganStatus = "NOT_SUITABLE",
                vegetarianStatus = "SUITABLE",
                energyKcal = null,
                fat = null,
                saturatedFat = null,
                carbs = null,
                sugars = null,
                fiber = null,
                proteins = null,
                salt = null,
                quantity = null,
                servingSize = null,
                isFavorite = false
            )
        )

        repository.toggleFavorite(barcode, isFavorite = true)
        assertTrue(fakeDao.getByBarcodeSync(barcode)?.isFavorite == true)

        repository.toggleFavorite(barcode, isFavorite = false)
        assertFalse(fakeDao.getByBarcodeSync(barcode)?.isFavorite == true)
    }

    @Test
    fun `getRecentScanned limits returned items to 20 ordered by scannedAt DESC`() = runTest(testDispatcher) {
        for (i in 1..25) {
            fakeDao.insertOrReplace(
                ScannedProductEntity(
                    barcode = "code_$i",
                    name = "Product $i",
                    brand = "Brand",
                    imageUrl = null,
                    nutriscoreGrade = null,
                    novaGroup = null,
                    ecoscoreGrade = null,
                    ingredientsText = null,
                    additivesRaw = "",
                    allergensRaw = "",
                    tracesRaw = "",
                    palmOilStatus = "UNKNOWN",
                    veganStatus = "UNKNOWN",
                    vegetarianStatus = "UNKNOWN",
                    energyKcal = null,
                    fat = null,
                    saturatedFat = null,
                    carbs = null,
                    sugars = null,
                    fiber = null,
                    proteins = null,
                    salt = null,
                    quantity = null,
                    servingSize = null,
                    scannedAt = 1000L + (i * 1000L)
                )
            )
        }

        val recent = repository.getRecentScanned(20).first()
        assertEquals(20, recent.size)
        assertEquals("Product 25", recent.first().name)
        assertEquals("Product 6", recent.last().name)
    }

    @Test
    fun `blank barcode returns failure`() = runTest(testDispatcher) {
        val result = repository.getProduct("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}

/**
 * In-memory test fake for ProductDao
 */
class FakeProductDao : ProductDao {
    private val products = MutableStateFlow<Map<String, ScannedProductEntity>>(emptyMap())

    override fun getAllScanned(): Flow<List<ScannedProductEntity>> {
        return products.map { it.values.sortedByDescending { entity -> entity.scannedAt } }
    }

    override fun getRecentScanned(limit: Int): Flow<List<ScannedProductEntity>> {
        return products.map { it.values.sortedByDescending { entity -> entity.scannedAt }.take(limit) }
    }

    override fun getFavorites(): Flow<List<ScannedProductEntity>> {
        return products.map { it.values.filter { entity -> entity.isFavorite }.sortedByDescending { entity -> entity.scannedAt } }
    }

    override fun getByBarcode(barcode: String): Flow<ScannedProductEntity?> {
        return products.map { it[barcode] }
    }

    override suspend fun getByBarcodeSync(barcode: String): ScannedProductEntity? {
        return products.value[barcode]
    }

    override suspend fun insertOrReplace(entity: ScannedProductEntity) {
        val map = products.value.toMutableMap()
        map[entity.barcode] = entity
        products.value = map
    }

    override suspend fun updateFavorite(barcode: String, isFavorite: Boolean) {
        val map = products.value.toMutableMap()
        val existing = map[barcode]
        if (existing != null) {
            map[barcode] = existing.copy(isFavorite = isFavorite)
            products.value = map
        }
    }

    override suspend fun deleteByBarcode(barcode: String) {
        val map = products.value.toMutableMap()
        map.remove(barcode)
        products.value = map
    }

    override suspend fun clearAll() {
        products.value = emptyMap()
    }
}

/**
 * In-memory test fake for OpenFoodFactsApiService
 */
class FakeOpenFoodFactsApiService : OpenFoodFactsApiService {
    var callCount = 0
    var shouldThrowNetworkError = false
    var stubbedResponse: Response<OpenFoodFactsResponse>? = null

    override suspend fun getProductByBarcode(barcode: String): Response<OpenFoodFactsResponse> {
        callCount++
        if (shouldThrowNetworkError) {
            throw java.io.IOException("Fake network connection failed")
        }
        return stubbedResponse ?: Response.error(404, "Not Found".toResponseBody("text/plain".toMediaTypeOrNull()))
    }
}
