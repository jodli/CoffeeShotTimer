package com.jodli.coffeeshottimer.data.util

import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Utility class for populating and clearing database with realistic test data.
 * This class is only available in debug builds to prevent accidental use in production.
 */
@Singleton
class DatabasePopulator @Inject constructor(
    private val beanDao: BeanDao,
    private val shotDao: ShotDao
) {

    /**
     * Populates the database with realistic test data for screenshots and testing.
     * Creates a variety of coffee beans and associated shots with realistic parameters.
     * 
     * @throws IllegalStateException if called in a release build
     * @throws Exception if database operations fail
     */
    suspend fun populateForScreenshots() = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            throw IllegalStateException("DatabasePopulator can only be used in debug builds")
        }

        try {
            // Create realistic coffee beans
            val beans = createRealisticBeans()
            
            // Insert beans first
            beans.forEach { bean ->
                beanDao.insertBean(bean)
            }

            // Create shots for each bean
            beans.forEach { bean ->
                val shots = createRealisticShotsForBean(bean)
                shots.forEach { shot ->
                    shotDao.insertShot(shot)
                }
            }

        } catch (e: Exception) {
            throw Exception("Failed to populate database: ${e.message}", e)
        }
    }

    /**
     * Adds additional shots to existing beans for testing purposes.
     * 
     * @param count Number of additional shots to create (default: 10)
     * @throws IllegalStateException if called in a release build
     * @throws Exception if database operations fail
     */
    suspend fun addMoreShots(count: Int = 10) = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            throw IllegalStateException("DatabasePopulator can only be used in debug builds")
        }

        try {
            // Get existing active beans
            val existingBeans = beanDao.getActiveBeans()
            
            // If no beans exist, create some first
            if (existingBeans.first().isEmpty()) {
                populateForScreenshots()
                return@withContext
            }

            val beans = existingBeans.first()
            repeat(count) {
                val randomBean = beans.random()
                val shot = createRandomShotForBean(randomBean)
                shotDao.insertShot(shot)
            }

        } catch (e: Exception) {
            throw Exception("Failed to add more shots: ${e.message}", e)
        }
    }

    /**
     * Clears all data from the database.
     * Removes all shots and beans from the database.
     * 
     * @throws IllegalStateException if called in a release build
     * @throws Exception if database operations fail
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            throw IllegalStateException("DatabasePopulator can only be used in debug builds")
        }

        try {
            // Get all beans and shots
            val allBeans = beanDao.getAllBeans().first()
            val allShots = shotDao.getAllShots().first()

            // Delete all shots first (due to foreign key constraints)
            allShots.forEach { shot ->
                shotDao.deleteShot(shot)
            }

            // Then delete all beans
            allBeans.forEach { bean ->
                beanDao.deleteBean(bean)
            }

        } catch (e: Exception) {
            throw Exception("Failed to clear database: ${e.message}", e)
        }
    }

    /**
     * Creates a list of realistic coffee beans with varied characteristics.
     */
    private fun createRealisticBeans(): List<Bean> {
        val beanProfiles = listOf(
            BeanProfile(
                "Ethiopian Yirgacheffe", 
                "Bright and floral with citrus notes. Light roast brings out the natural acidity.",
                "2.8"
            ),
            BeanProfile(
                "Colombian Supremo", 
                "Well-balanced with chocolate and caramel notes. Medium roast, very consistent.",
                "3.0"
            ),
            BeanProfile(
                "Brazilian Santos", 
                "Nutty and smooth with low acidity. Great for espresso blends.",
                "2.5"
            ),
            BeanProfile(
                "Guatemalan Antigua", 
                "Full-bodied with spicy and smoky undertones. Complex flavor profile.",
                "3.2"
            ),
            BeanProfile(
                "Costa Rican Tarrazú", 
                "Bright acidity with wine-like characteristics. Clean finish.",
                "2.9"
            )
        )

        return beanProfiles.mapIndexed { index, profile ->
            Bean(
                id = UUID.randomUUID().toString(),
                name = profile.name,
                roastDate = LocalDate.now().minusDays((3..20).random().toLong()),
                notes = profile.notes,
                isActive = true,
                lastGrinderSetting = profile.grinderSetting,
                createdAt = LocalDateTime.now().minusDays(index.toLong())
            )
        }
    }

    /**
     * Creates realistic shots for a given bean with varied parameters.
     */
    private fun createRealisticShotsForBean(bean: Bean): List<Shot> {
        val shotCount = (3..8).random() // 3-8 shots per bean
        val shots = mutableListOf<Shot>()

        repeat(shotCount) { index ->
            shots.add(createRandomShotForBean(bean, index))
        }

        return shots
    }

    /**
     * Creates a single realistic shot for a given bean.
     */
    private fun createRandomShotForBean(bean: Bean, dayOffset: Int = 0): Shot {
        val shotProfiles = getRealisticShotProfiles()
        val profile = shotProfiles.random()

        // Add some variation to the base profile
        val weightInVariation = Random.nextDouble(-1.0, 1.0)
        val timeVariation = Random.nextInt(-3, 4)
        val grinderVariation = Random.nextDouble(-0.2, 0.2)

        val coffeeWeightIn = (profile.coffeeWeightIn + weightInVariation).coerceIn(16.0, 24.0)
        val extractionTime = (profile.extractionTimeSeconds + timeVariation).coerceIn(20, 40)
        val baseGrinderSetting = bean.lastGrinderSetting?.toDoubleOrNull() ?: 3.0
        val grinderSetting = String.format("%.1f", (baseGrinderSetting + grinderVariation).coerceIn(1.0, 5.0))

        // Calculate output weight based on target ratio with some variation
        val targetRatio = profile.targetRatio + Random.nextDouble(-0.2, 0.2)
        val coffeeWeightOut = (coffeeWeightIn * targetRatio).coerceIn(25.0, 60.0)

        return Shot(
            id = UUID.randomUUID().toString(),
            beanId = bean.id,
            coffeeWeightIn = Math.round(coffeeWeightIn * 10) / 10.0,
            coffeeWeightOut = Math.round(coffeeWeightOut * 10) / 10.0,
            extractionTimeSeconds = extractionTime,
            grinderSetting = grinderSetting,
            notes = profile.notes.random(),
            timestamp = LocalDateTime.now().minusDays(dayOffset.toLong()).minusHours(Random.nextLong(0, 24))
        )
    }

    /**
     * Returns a list of realistic shot profiles with different characteristics.
     */
    private fun getRealisticShotProfiles(): List<ShotProfile> {
        return listOf(
            ShotProfile(
                coffeeWeightIn = 18.0,
                targetRatio = 2.0,
                extractionTimeSeconds = 28,
                notes = listOf(
                    "Perfect crema, balanced flavor",
                    "Great extraction, smooth finish",
                    "Excellent shot, will repeat this setting"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 20.0,
                targetRatio = 2.2,
                extractionTimeSeconds = 30,
                notes = listOf(
                    "Slightly over-extracted, reduce grind",
                    "Good body, maybe a bit bitter",
                    "Decent shot, could be better"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 19.0,
                targetRatio = 1.8,
                extractionTimeSeconds = 25,
                notes = listOf(
                    "Fast extraction, increase grind fineness",
                    "Sour notes, need longer extraction",
                    "Under-extracted, adjust grinder"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 21.0,
                targetRatio = 2.5,
                extractionTimeSeconds = 32,
                notes = listOf(
                    "Long shot, nice for milk drinks",
                    "Good volume, mild flavor",
                    "Perfect for cappuccino"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 17.5,
                targetRatio = 2.1,
                extractionTimeSeconds = 27,
                notes = listOf(
                    "Concentrated shot, intense flavor",
                    "Strong and bold, great crema",
                    "Ristretto style, very tasty"
                )
            )
        )
    }

    /**
     * Data class representing a coffee bean profile for test data generation.
     */
    private data class BeanProfile(
        val name: String,
        val notes: String,
        val grinderSetting: String
    )

    /**
     * Data class representing a shot profile for test data generation.
     */
    private data class ShotProfile(
        val coffeeWeightIn: Double,
        val targetRatio: Double,
        val extractionTimeSeconds: Int,
        val notes: List<String>
    )
}