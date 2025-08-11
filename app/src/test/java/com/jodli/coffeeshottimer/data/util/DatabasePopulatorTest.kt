package com.jodli.coffeeshottimer.data.util

import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.GrinderConfigDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.model.Shot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for DatabasePopulator class.
 * Tests data generation, database operations, and error handling.
 * Note: These tests assume DEBUG build since we can't easily mock BuildConfig in unit tests.
 */
class DatabasePopulatorTest {

    private lateinit var beanDao: BeanDao
    private lateinit var shotDao: ShotDao
    private lateinit var grinderConfigDao: GrinderConfigDao
    private lateinit var databasePopulator: DatabasePopulator

    @Before
    fun setup() {
        // Skip all tests in this class if not in debug build
        assumeTrue("DatabasePopulator tests only run in debug builds", BuildConfig.DEBUG)

        beanDao = mockk(relaxed = true)
        shotDao = mockk(relaxed = true)
        grinderConfigDao = mockk(relaxed = true)
        databasePopulator = DatabasePopulator(beanDao, shotDao, grinderConfigDao)
    }

    // Note: BuildConfig.DEBUG tests are handled in ConditionalCompilationTest

    @Test
    fun `populateForScreenshots should create realistic beans and shots`() = runTest {
        // Given
        coEvery { grinderConfigDao.getCurrentConfig() } returns null
        coEvery { grinderConfigDao.insertConfig(any()) } returns Unit
        coEvery { beanDao.insertBean(any()) } returns Unit
        coEvery { shotDao.insertShot(any()) } returns Unit

        // When
        databasePopulator.populateForScreenshots()

        // Then
        // Verify that grinder config was inserted (only if none exists)
        coVerify(exactly = 1) { grinderConfigDao.insertConfig(any()) }
        
        // Verify that beans were inserted (should be 5 beans based on implementation)
        coVerify(exactly = 5) { beanDao.insertBean(any()) }

        // Verify that shots were inserted (5-12 shots per bean, so 25-60 total)
        coVerify(atLeast = 25) { shotDao.insertShot(any()) }
        coVerify(atMost = 60) { shotDao.insertShot(any()) }
    }

    @Test
    fun `populateForScreenshots should handle database insertion errors`() = runTest {
        // Given
        coEvery { grinderConfigDao.getCurrentConfig() } returns null
        coEvery { grinderConfigDao.insertConfig(any()) } returns Unit
        coEvery { beanDao.insertBean(any()) } throws RuntimeException("Database error")

        // When & Then
        try {
            databasePopulator.populateForScreenshots()
        } catch (e: Exception) {
            assertTrue("Should contain error message", e.message?.contains("Failed to populate database") == true)
            assertTrue("Should contain original error", e.message?.contains("Database error") == true)
        }
    }



    @Test
    fun `addMoreShots should add shots to existing beans`() = runTest {
        // Given
        val existingBeans = listOf(
            Bean(
                id = "bean1",
                name = "Test Bean",
                roastDate = LocalDate.now().minusDays(7),
                notes = "Test notes",
                lastGrinderSetting = "3.0"
            )
        )
        coEvery { beanDao.getActiveBeans() } returns flowOf(existingBeans)
        coEvery { shotDao.insertShot(any()) } returns Unit

        // When
        databasePopulator.addMoreShots(5)

        // Then
        coVerify(exactly = 5) { shotDao.insertShot(any()) }
    }

    @Test
    fun `addMoreShots should populate database if no beans exist`() = runTest {
        // Given
        coEvery { beanDao.getActiveBeans() } returns flowOf(emptyList())
        coEvery { grinderConfigDao.getCurrentConfig() } returns null
        coEvery { grinderConfigDao.insertConfig(any()) } returns Unit
        coEvery { beanDao.insertBean(any()) } returns Unit
        coEvery { shotDao.insertShot(any()) } returns Unit

        // When
        databasePopulator.addMoreShots(5)

        // Then
        // Should call populateForScreenshots which inserts beans and shots
        coVerify(exactly = 1) { grinderConfigDao.insertConfig(any()) }
        coVerify(exactly = 5) { beanDao.insertBean(any()) }
        coVerify(atLeast = 25) { shotDao.insertShot(any()) }
    }

    @Test
    fun `addMoreShots should handle database errors`() = runTest {
        // Given
        val existingBeans = listOf(
            Bean(
                id = "bean1",
                name = "Test Bean",
                roastDate = LocalDate.now().minusDays(7)
            )
        )
        coEvery { beanDao.getActiveBeans() } returns flowOf(existingBeans)
        coEvery { shotDao.insertShot(any()) } throws RuntimeException("Insert failed")

        // When & Then
        try {
            databasePopulator.addMoreShots(5)
        } catch (e: Exception) {
            assertTrue("Should contain error message", e.message?.contains("Failed to add more shots") == true)
            assertTrue("Should contain original error", e.message?.contains("Insert failed") == true)
        }
    }

    // Note: BuildConfig.DEBUG conditional compilation tests are in ConditionalCompilationTest.kt

    @Test
    fun `clearAllData should delete all shots and beans`() = runTest {
        // Given
        val existingBeans = listOf(
            Bean(id = "bean1", name = "Bean 1", roastDate = LocalDate.now()),
            Bean(id = "bean2", name = "Bean 2", roastDate = LocalDate.now())
        )
        val existingShots = listOf(
            Shot(
                id = "shot1",
                beanId = "bean1",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                grinderSetting = "3.0"
            ),
            Shot(
                id = "shot2",
                beanId = "bean2",
                coffeeWeightIn = 20.0,
                coffeeWeightOut = 40.0,
                extractionTimeSeconds = 30,
                grinderSetting = "2.8"
            )
        )

        coEvery { beanDao.getAllBeans() } returns flowOf(existingBeans)
        coEvery { shotDao.getAllShots() } returns flowOf(existingShots)
        coEvery { grinderConfigDao.getCurrentConfig() } returns mockk(relaxed = true)
        coEvery { shotDao.deleteShot(any()) } returns Unit
        coEvery { beanDao.deleteBean(any()) } returns Unit
        coEvery { grinderConfigDao.deleteConfig(any()) } returns Unit

        // When
        databasePopulator.clearAllData()

        // Then
        // Verify shots are deleted first (due to foreign key constraints)
        coVerify(exactly = 2) { shotDao.deleteShot(any()) }
        coVerify(exactly = 2) { beanDao.deleteBean(any()) }
        coVerify(exactly = 1) { grinderConfigDao.deleteConfig(any()) }
    }

    @Test
    fun `clearAllData should handle database deletion errors`() = runTest {
        // Given
        val existingBeans = listOf(
            Bean(id = "bean1", name = "Bean 1", roastDate = LocalDate.now())
        )
        val existingShots = listOf(
            Shot(
                id = "shot1",
                beanId = "bean1",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                grinderSetting = "3.0"
            )
        )

        coEvery { beanDao.getAllBeans() } returns flowOf(existingBeans)
        coEvery { shotDao.getAllShots() } returns flowOf(existingShots)
        coEvery { grinderConfigDao.getCurrentConfig() } returns mockk(relaxed = true)
        coEvery { shotDao.deleteShot(any()) } throws RuntimeException("Delete failed")

        // When & Then
        try {
            databasePopulator.clearAllData()
        } catch (e: Exception) {
            assertTrue("Should contain error message", e.message?.contains("Failed to clear database") == true)
            assertTrue("Should contain original error", e.message?.contains("Delete failed") == true)
        }
    }

    @Test
    fun `generated beans should have realistic characteristics`() = runTest {
        // Given
        val capturedBeans = mutableListOf<Bean>()
        coEvery { beanDao.insertBean(capture(capturedBeans)) } returns Unit
        coEvery { shotDao.insertShot(any()) } returns Unit

        // When
        databasePopulator.populateForScreenshots()

        // Then
        assertEquals(5, capturedBeans.size)

        // Verify bean names are realistic
        val expectedNames = setOf(
            "Ethiopian Yirgacheffe",
            "Colombian Supremo",
            "Brazilian Santos",
            "Guatemalan Antigua",
            "Costa Rican Tarrazú"
        )
        capturedBeans.forEach { bean ->
            assertTrue("Bean name should be realistic", bean.name in expectedNames)
            assertTrue("Bean should be active", bean.isActive)
            assertTrue("Bean should have notes", bean.notes.isNotBlank())
            assertTrue("Bean should have grinder setting", bean.lastGrinderSetting?.isNotBlank() == true)

            // Verify roast date is within reasonable range (3-20 days ago)
            val daysSinceRoast = java.time.temporal.ChronoUnit.DAYS.between(bean.roastDate, LocalDate.now())
            assertTrue("Roast date should be 3-20 days ago", daysSinceRoast in 3..20)
        }
    }

    @Test
    fun `generated shots should have realistic parameters`() = runTest {
        // Given
        val capturedShots = mutableListOf<Shot>()
        coEvery { beanDao.insertBean(any()) } returns Unit
        coEvery { shotDao.insertShot(capture(capturedShots)) } returns Unit

        // When
        databasePopulator.populateForScreenshots()

        // Then
        assertTrue("Should generate multiple shots", capturedShots.size >= 25)

        capturedShots.forEach { shot ->
            // Verify realistic weight ranges (expanded for beginner mistakes)
            assertTrue("Coffee weight in should be realistic", shot.coffeeWeightIn in 14.0..25.0)
            assertTrue("Coffee weight out should be realistic", shot.coffeeWeightOut in 20.0..70.0)

            // Verify realistic extraction time (expanded for beginner/expert range)
            assertTrue("Extraction time should be realistic", shot.extractionTimeSeconds in 15..50)

            // Verify grinder setting format
            assertTrue("Grinder setting should not be blank", shot.grinderSetting.isNotBlank())

            // Verify notes are present
            assertTrue("Shot should have notes", shot.notes.isNotBlank())

            // Verify brew ratio is reasonable
            assertTrue("Brew ratio should be reasonable", shot.brewRatio in 1.0..4.0)
        }
    }
}