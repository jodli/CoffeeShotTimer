package com.jodli.coffeeshottimer.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Database instrumentation tests for taste feedback functionality.
 * Tests actual database persistence, type conversion, and queries.
 */
@RunWith(AndroidJUnit4::class)
class ShotDaoTasteTest {

    private lateinit var database: AppDatabase
    private lateinit var shotDao: ShotDao
    private lateinit var beanDao: BeanDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shotDao = database.shotDao()
        beanDao = database.beanDao()

        // Create test bean
        runBlocking {
            val testBean = Bean(
                id = "test-bean",
                name = "Test Bean",
                roastDate = LocalDate.now().minusDays(7),
                notes = "",
                isActive = true
            )
            beanDao.insertBean(testBean)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertShot_withTasteFeedback_storesCorrectly() = runBlocking {
        // Given
        val shot = Shot(
            id = "test-shot",
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            notes = "",
            tastePrimary = TastePrimary.PERFECT,
            tasteSecondary = TasteSecondary.STRONG
        )

        // When
        shotDao.insertShot(shot)
        val retrievedShot = shotDao.getShotById("test-shot")

        // Then
        assertNotNull(retrievedShot)
        assertEquals(TastePrimary.PERFECT, retrievedShot!!.tastePrimary)
        assertEquals(TasteSecondary.STRONG, retrievedShot.tasteSecondary)
    }

    @Test
    fun insertShot_withoutTasteFeedback_storesAsNull() = runBlocking {
        // Given
        val shot = Shot(
            id = "test-shot",
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            notes = ""
        )

        // When
        shotDao.insertShot(shot)
        val retrievedShot = shotDao.getShotById("test-shot")

        // Then
        assertNotNull(retrievedShot)
        assertNull(retrievedShot!!.tastePrimary)
        assertNull(retrievedShot.tasteSecondary)
    }

    @Test
    fun updateTasteFeedback_modifiesExistingShot() = runBlocking {
        // Given - create shot without taste feedback
        val shot = Shot(
            id = "test-shot",
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            notes = ""
        )
        shotDao.insertShot(shot)

        // When - update with taste feedback
        shotDao.updateTasteFeedback("test-shot", TastePrimary.SOUR, TasteSecondary.WEAK)
        val updatedShot = shotDao.getShotById("test-shot")

        // Then
        assertNotNull(updatedShot)
        assertEquals(TastePrimary.SOUR, updatedShot!!.tastePrimary)
        assertEquals(TasteSecondary.WEAK, updatedShot.tasteSecondary)
    }

    @Test
    fun updateTasteFeedback_clearsTasteFeedback() = runBlocking {
        // Given - create shot with taste feedback
        val shot = Shot(
            id = "test-shot",
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            notes = "",
            tastePrimary = TastePrimary.PERFECT,
            tasteSecondary = TasteSecondary.STRONG
        )
        shotDao.insertShot(shot)

        // When - clear taste feedback
        shotDao.updateTasteFeedback("test-shot", null, null)
        val updatedShot = shotDao.getShotById("test-shot")

        // Then
        assertNotNull(updatedShot)
        assertNull(updatedShot!!.tastePrimary)
        assertNull(updatedShot.tasteSecondary)
    }

    @Test
    fun getShotsByTaste_filtersCorrectly() = runBlocking {
        // Given - create shots with different taste feedback
        val shots = listOf(
            Shot(
                id = "shot-1",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 22,
                grinderSetting = "15",
                tastePrimary = TastePrimary.SOUR
            ),
            Shot(
                id = "shot-2",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                grinderSetting = "14",
                tastePrimary = TastePrimary.PERFECT
            ),
            Shot(
                id = "shot-3",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 35,
                grinderSetting = "16",
                tastePrimary = TastePrimary.BITTER
            ),
            Shot(
                id = "shot-4",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 30,
                grinderSetting = "15"
                // No taste feedback
            )
        )

        shots.forEach { shotDao.insertShot(it) }

        // When - filter by PERFECT taste
        val perfectShots = shotDao.getShotsByTaste(TastePrimary.PERFECT, "test-bean").first()

        // Then - should only return the PERFECT shot
        assertEquals(1, perfectShots.size)
        assertEquals("shot-2", perfectShots.first().id)
        assertEquals(TastePrimary.PERFECT, perfectShots.first().tastePrimary)
    }

    @Test
    fun getShotsByTaste_withNullTaste_returnsAllShots() = runBlocking {
        // Given - create shots with mixed taste feedback
        val shots = listOf(
            Shot(
                id = "shot-1",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 22,
                grinderSetting = "15",
                tastePrimary = TastePrimary.SOUR
            ),
            Shot(
                id = "shot-2",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 30,
                grinderSetting = "15"
                // No taste feedback
            )
        )

        shots.forEach { shotDao.insertShot(it) }

        // When - filter with null taste (should return all)
        val allShots = shotDao.getShotsByTaste(null, "test-bean").first()

        // Then - should return all shots for the bean
        assertEquals(2, allShots.size)
    }

    @Test
    fun getTasteDistributionForBean_calculatesCorrectly() = runBlocking {
        // Given - create shots with various taste feedback
        val shots = listOf(
            Shot(
                id = "shot-1",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 22,
                grinderSetting = "15",
                tastePrimary = TastePrimary.SOUR
            ),
            Shot(
                id = "shot-2",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                grinderSetting = "14",
                tastePrimary = TastePrimary.PERFECT
            ),
            Shot(
                id = "shot-3",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 27,
                grinderSetting = "14",
                tastePrimary = TastePrimary.PERFECT
            ),
            Shot(
                id = "shot-4",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 35,
                grinderSetting = "16",
                tastePrimary = TastePrimary.BITTER
            )
            // Note: Shots without taste feedback should not be included in distribution
        )

        shots.forEach { shotDao.insertShot(it) }

        // When
        val distribution = shotDao.getTasteDistributionForBean("test-bean")

        // Then
        assertEquals(3, distribution.size) // 3 different taste types

        val sourCount = distribution.find { it.tastePrimary == TastePrimary.SOUR }?.count ?: 0
        val perfectCount = distribution.find { it.tastePrimary == TastePrimary.PERFECT }?.count ?: 0
        val bitterCount = distribution.find { it.tastePrimary == TastePrimary.BITTER }?.count ?: 0

        assertEquals(1, sourCount)
        assertEquals(2, perfectCount)
        assertEquals(1, bitterCount)
    }

    @Test
    fun tasteEnumConversion_handlesAllValues() = runBlocking {
        // Test all combinations of primary and secondary taste values
        val testCases = listOf(
            Pair(TastePrimary.SOUR, null),
            Pair(TastePrimary.SOUR, TasteSecondary.WEAK),
            Pair(TastePrimary.SOUR, TasteSecondary.STRONG),
            Pair(TastePrimary.PERFECT, null),
            Pair(TastePrimary.PERFECT, TasteSecondary.WEAK),
            Pair(TastePrimary.PERFECT, TasteSecondary.STRONG),
            Pair(TastePrimary.BITTER, null),
            Pair(TastePrimary.BITTER, TasteSecondary.WEAK),
            Pair(TastePrimary.BITTER, TasteSecondary.STRONG)
        )

        testCases.forEachIndexed { index, (primary, secondary) ->
            // Given
            val shot = Shot(
                id = "shot-$index",
                beanId = "test-bean",
                coffeeWeightIn = 18.0,
                coffeeWeightOut = 36.0,
                extractionTimeSeconds = 28,
                grinderSetting = "15",
                tastePrimary = primary,
                tasteSecondary = secondary
            )

            // When
            shotDao.insertShot(shot)
            val retrievedShot = shotDao.getShotById("shot-$index")

            // Then
            assertNotNull("Shot $index should be retrieved", retrievedShot)
            assertEquals("Primary taste for shot $index", primary, retrievedShot!!.tastePrimary)
            assertEquals("Secondary taste for shot $index", secondary, retrievedShot.tasteSecondary)
        }
    }

    @Test
    fun updateTasteFeedback_onlyModifyingPrimary_preservesSecondary() = runBlocking {
        // Given - shot with both primary and secondary taste
        val shot = Shot(
            id = "test-shot",
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            tastePrimary = TastePrimary.SOUR,
            tasteSecondary = TasteSecondary.WEAK
        )
        shotDao.insertShot(shot)

        // When - update only primary taste
        shotDao.updateTasteFeedback("test-shot", TastePrimary.PERFECT, TasteSecondary.WEAK)
        val updatedShot = shotDao.getShotById("test-shot")

        // Then - both should be updated as specified
        assertNotNull(updatedShot)
        assertEquals(TastePrimary.PERFECT, updatedShot!!.tastePrimary)
        assertEquals(TasteSecondary.WEAK, updatedShot.tasteSecondary)
    }

    @Test
    fun typeConverters_handleNullValues() = runBlocking {
        // Given - shot with null taste values
        val shot = Shot(
            id = "test-shot",
            beanId = "test-bean",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15",
            tastePrimary = null,
            tasteSecondary = null
        )

        // When
        shotDao.insertShot(shot)
        val retrievedShot = shotDao.getShotById("test-shot")

        // Then
        assertNotNull(retrievedShot)
        assertNull(retrievedShot!!.tastePrimary)
        assertNull(retrievedShot.tasteSecondary)
    }
}
