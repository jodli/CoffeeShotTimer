package com.jodli.coffeeshottimer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import kotlin.system.measureTimeMillis

/**
 * Performance tests to verify app performance with large datasets.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PerformanceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testShotHistoryPerformanceWithLargeDataset() = runBlocking {
        // Create test data - 1000 shots across 10 beans
        val testBeans = createTestBeans(10)
        val testShots = createTestShots(testBeans, 1000)

        // Insert test data
        val insertTime = measureTimeMillis {
            database.beanDao().let { beanDao ->
                testBeans.forEach { bean ->
                    beanDao.insertBean(bean)
                }
            }
            
            database.shotDao().let { shotDao ->
                testShots.forEach { shot ->
                    shotDao.insertShot(shot)
                }
            }
        }

        // Verify insert performance (should be under 5 seconds)
        assert(insertTime < 5000) { "Data insertion took too long: ${insertTime}ms" }

        // Test shot history loading performance
        composeTestRule.onNodeWithText("History").performClick()

        val loadTime = measureTimeMillis {
            // Wait for initial load
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodesWithText("Loading shot history...").fetchSemanticsNodes().isEmpty()
            }
        }

        // Verify load performance (should be under 3 seconds)
        assert(loadTime < 3000) { "Shot history loading took too long: ${loadTime}ms" }

        // Test scrolling performance
        val scrollTime = measureTimeMillis {
            repeat(10) {
                composeTestRule.onNodeWithTag("shot_history_list").performScrollToIndex(it * 20)
                Thread.sleep(100) // Small delay to simulate real scrolling
            }
        }

        // Verify scrolling performance (should be under 2 seconds)
        assert(scrollTime < 2000) { "Scrolling took too long: ${scrollTime}ms" }

        // Test filtering performance
        composeTestRule.onNodeWithContentDescription("Filter shots").performClick()
        
        val filterTime = measureTimeMillis {
            composeTestRule.onNodeWithText("Filter by Bean").performClick()
            composeTestRule.onNodeWithText(testBeans.first().name).performClick()
            composeTestRule.onNodeWithText("Apply Filters").performClick()
            
            // Wait for filter to apply
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithText("Filtered results").fetchSemanticsNodes().isNotEmpty()
            }
        }

        // Verify filtering performance (should be under 2 seconds)
        assert(filterTime < 2000) { "Filtering took too long: ${filterTime}ms" }
    }

    @Test
    fun testBeanManagementPerformanceWithManyBeans() = runBlocking {
        // Create 100 test beans
        val testBeans = createTestBeans(100)

        val insertTime = measureTimeMillis {
            database.beanDao().let { beanDao ->
                testBeans.forEach { bean ->
                    beanDao.insertBean(bean)
                }
            }
        }

        // Verify insert performance
        assert(insertTime < 3000) { "Bean insertion took too long: ${insertTime}ms" }

        // Test bean list loading
        composeTestRule.onNodeWithText("Beans").performClick()

        val loadTime = measureTimeMillis {
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithText("Loading beans...").fetchSemanticsNodes().isEmpty()
            }
        }

        // Verify load performance
        assert(loadTime < 2000) { "Bean list loading took too long: ${loadTime}ms" }

        // Test search performance
        val searchTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("Search beans").performClick()
            composeTestRule.onNodeWithText("Search").performTextInput("Bean 1")
            
            // Wait for search results
            Thread.sleep(500)
        }

        // Verify search performance
        assert(searchTime < 1000) { "Bean search took too long: ${searchTime}ms" }
    }

    @Test
    fun testMemoryUsageWithLargeDataset() = runBlocking {
        // Get initial memory usage
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Create large dataset
        val testBeans = createTestBeans(50)
        val testShots = createTestShots(testBeans, 2000)

        // Insert data
        database.beanDao().let { beanDao ->
            testBeans.forEach { bean ->
                beanDao.insertBean(bean)
            }
        }
        
        database.shotDao().let { shotDao ->
            testShots.forEach { shot ->
                shotDao.insertShot(shot)
            }
        }

        // Load shot history multiple times to test memory leaks
        repeat(5) {
            composeTestRule.onNodeWithText("History").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithText("Loading shot history...").fetchSemanticsNodes().isEmpty()
            }
            
            // Navigate away and back
            composeTestRule.onNodeWithText("Record").performClick()
            Thread.sleep(100)
        }

        // Check memory usage after operations
        System.gc() // Suggest garbage collection
        Thread.sleep(1000) // Wait for GC
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)

        // Memory increase should be reasonable (less than 100MB for this test)
        assert(memoryIncreaseMB < 100) { "Memory usage increased too much: ${memoryIncreaseMB}MB" }
    }

    @Test
    fun testDatabaseQueryPerformance() = runBlocking {
        // Create test data
        val testBeans = createTestBeans(20)
        val testShots = createTestShots(testBeans, 5000)

        // Insert data
        database.beanDao().let { beanDao ->
            testBeans.forEach { bean ->
                beanDao.insertBean(bean)
            }
        }
        
        database.shotDao().let { shotDao ->
            testShots.forEach { shot ->
                shotDao.insertShot(shot)
            }
        }

        val shotDao = database.shotDao()
        val beanDao = database.beanDao()

        // Test various query performance
        val getAllShotsTime = measureTimeMillis {
            shotDao.getShotsPaginated(20, 0)
        }
        assert(getAllShotsTime < 100) { "Get shots query too slow: ${getAllShotsTime}ms" }

        val getFilteredShotsTime = measureTimeMillis {
            shotDao.getFilteredShotsPaginated(
                testBeans.first().id,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now(),
                20,
                0
            )
        }
        assert(getFilteredShotsTime < 150) { "Filtered shots query too slow: ${getFilteredShotsTime}ms" }

        val getShotStatisticsTime = measureTimeMillis {
            shotDao.getShotStatistics(testBeans.first().id)
        }
        assert(getShotStatisticsTime < 50) { "Statistics query too slow: ${getShotStatisticsTime}ms" }

        val getActiveBeansTime = measureTimeMillis {
            beanDao.getActiveBeanCount()
        }
        assert(getActiveBeansTime < 20) { "Active beans query too slow: ${getActiveBeansTime}ms" }
    }

    private fun createTestBeans(count: Int): List<Bean> {
        return (1..count).map { i ->
            Bean(
                id = UUID.randomUUID().toString(),
                name = "Test Bean $i",
                origin = "Test Origin $i",
                roaster = "Test Roaster $i",
                roastDate = LocalDate.now().minusDays((i % 30).toLong()),
                notes = "Test notes for bean $i",
                isActive = true,
                lastGrinderSetting = "${10 + (i % 10)}",
                createdAt = LocalDateTime.now().minusDays((i % 100).toLong())
            )
        }
    }

    private fun createTestShots(beans: List<Bean>, count: Int): List<Shot> {
        val random = Random()
        return (1..count).map { i ->
            val bean = beans[i % beans.size]
            val coffeeWeightIn = 16.0 + random.nextDouble() * 4.0 // 16-20g
            val brewRatio = 1.8 + random.nextDouble() * 1.0 // 1.8-2.8 ratio
            val coffeeWeightOut = coffeeWeightIn * brewRatio
            
            Shot(
                id = UUID.randomUUID().toString(),
                beanId = bean.id,
                coffeeWeightIn = coffeeWeightIn,
                coffeeWeightOut = coffeeWeightOut,
                extractionTimeSeconds = 20 + random.nextInt(20), // 20-40 seconds
                grinderSetting = "${10 + random.nextInt(10)}", // 10-20
                notes = if (i % 5 == 0) "Test shot notes $i" else "",
                timestamp = LocalDateTime.now().minusDays((i % 365).toLong()).minusHours(random.nextInt(24).toLong())
            )
        }
    }
}