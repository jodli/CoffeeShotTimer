package com.jodli.coffeeshottimer.data.util

import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.GrinderConfigDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
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
    private val shotDao: ShotDao,
    private val grinderConfigDao: GrinderConfigDao
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
            // Create realistic grinder configuration if none exists
            val existingConfig = grinderConfigDao.getCurrentConfig()
            if (existingConfig == null) {
                val grinderConfig = createRealisticGrinderConfiguration()
                grinderConfigDao.insertConfig(grinderConfig)
            }

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
            // Get all data
            val allBeans = beanDao.getAllBeans().first()
            val allShots = shotDao.getAllShots().first()
            val currentConfig = grinderConfigDao.getCurrentConfig()

            // Delete all shots first (due to foreign key constraints)
            allShots.forEach { shot ->
                shotDao.deleteShot(shot)
            }

            // Then delete all beans
            allBeans.forEach { bean ->
                beanDao.deleteBean(bean)
            }

            // Finally delete the grinder configuration if it exists
            currentConfig?.let { config ->
                grinderConfigDao.deleteConfig(config)
            }
        } catch (e: Exception) {
            throw Exception("Failed to clear database: ${e.message}", e)
        }
    }

    /**
     * Creates a realistic grinder configuration for test data.
     * Randomly selects from common grinder scale ranges.
     */
    private fun createRealisticGrinderConfiguration(): GrinderConfiguration {
        val commonConfigs = listOf(
            GrinderConfiguration(scaleMin = 1, scaleMax = 10), // Baratza Encore style
            GrinderConfiguration(scaleMin = 30, scaleMax = 80), // Comandante style
            GrinderConfiguration(scaleMin = 0, scaleMax = 100), // Generic percentage
            GrinderConfiguration(scaleMin = 5, scaleMax = 15), // Fine adjustment range
            GrinderConfiguration(scaleMin = 20, scaleMax = 60) // Mid-range burr grinder
        )

        return commonConfigs.random().copy(
            id = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now().minusDays(Random.nextLong(1, 30))
        )
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
                lastGrinderSetting = null, // Deprecated: kept for DB compatibility, not used
                createdAt = LocalDateTime.now().minusDays(index.toLong())
            )
        }
    }

    /**
     * Creates realistic shots for a given bean with varied parameters.
     * Simulates a progression from beginner to expert barista over time.
     */
    private fun createRealisticShotsForBean(bean: Bean): List<Shot> {
        val shotCount = (5..12).random() // 5-12 shots per bean for more variety
        val shots = mutableListOf<Shot>()

        repeat(shotCount) { index ->
            // Simulate skill progression over time - early shots are more beginner-like
            val skillLevel = when {
                index < 2 -> BaristaSkillLevel.BEGINNER
                index < 4 -> BaristaSkillLevel.INTERMEDIATE
                index < 7 -> BaristaSkillLevel.ADVANCED
                else -> if (Random.nextFloat() < 0.7f) BaristaSkillLevel.EXPERT else BaristaSkillLevel.ADVANCED
            }

            shots.add(createShotForSkillLevel(bean, skillLevel, index))
        }

        return shots
    }

    /**
     * Creates a single realistic shot for a given bean with random skill level.
     */
    private fun createRandomShotForBean(bean: Bean, dayOffset: Int = 0): Shot {
        val skillLevels = BaristaSkillLevel.values()
        val randomSkillLevel = skillLevels.random()
        return createShotForSkillLevel(bean, randomSkillLevel, dayOffset)
    }

    /**
     * Creates a shot based on barista skill level, simulating realistic mistakes and improvements.
     */
    private fun createShotForSkillLevel(bean: Bean, skillLevel: BaristaSkillLevel, dayOffset: Int = 0): Shot {
        val profiles = getShotProfilesForSkillLevel(skillLevel)
        val profile = profiles.random()

        // Apply skill-based variations
        val skillVariations = getSkillVariations(skillLevel)

        // Base parameters with skill-level appropriate variations
        val weightInVariation = Random.nextDouble(-skillVariations.weightVariance, skillVariations.weightVariance)
        val timeVariation = Random.nextInt(-skillVariations.timeVariance, skillVariations.timeVariance + 1)
        val ratioVariation = Random.nextDouble(-skillVariations.ratioVariance, skillVariations.ratioVariance)

        val coffeeWeightIn = (profile.coffeeWeightIn + weightInVariation).coerceIn(14.0, 25.0)
        val extractionTime = (profile.extractionTimeSeconds + timeVariation).coerceIn(15, 50)

        // Calculate grinder setting with skill-level consistency
        val baseGrinderSetting = 3.0 // Default grinder setting
        val grinderVariation = Random.nextDouble(-skillVariations.grinderVariance, skillVariations.grinderVariance)
        val grinderSetting = String.format(
            java.util.Locale.ROOT,
            "%.1f",
            (baseGrinderSetting + grinderVariation).coerceIn(1.0, 5.0)
        )

        // Calculate output weight with skill-level appropriate ratio control
        val targetRatio = profile.targetRatio + ratioVariation
        val coffeeWeightOut = (coffeeWeightIn * targetRatio).coerceIn(20.0, 70.0)

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
     * Returns shot profiles based on barista skill level.
     */
    private fun getShotProfilesForSkillLevel(skillLevel: BaristaSkillLevel): List<ShotProfile> {
        return when (skillLevel) {
            BaristaSkillLevel.BEGINNER -> getBeginnerShotProfiles()
            BaristaSkillLevel.INTERMEDIATE -> getIntermediateShotProfiles()
            BaristaSkillLevel.ADVANCED -> getAdvancedShotProfiles()
            BaristaSkillLevel.EXPERT -> getExpertShotProfiles()
        }
    }

    /**
     * Returns skill-based variation parameters.
     */
    private fun getSkillVariations(skillLevel: BaristaSkillLevel): SkillVariations {
        return when (skillLevel) {
            BaristaSkillLevel.BEGINNER -> SkillVariations(
                weightVariance = 3.0,
                timeVariance = 8,
                ratioVariance = 0.8,
                grinderVariance = 0.5
            )
            BaristaSkillLevel.INTERMEDIATE -> SkillVariations(
                weightVariance = 1.5,
                timeVariance = 5,
                ratioVariance = 0.4,
                grinderVariance = 0.3
            )
            BaristaSkillLevel.ADVANCED -> SkillVariations(
                weightVariance = 0.8,
                timeVariance = 3,
                ratioVariance = 0.2,
                grinderVariance = 0.2
            )
            BaristaSkillLevel.EXPERT -> SkillVariations(
                weightVariance = 0.3,
                timeVariance = 2,
                ratioVariance = 0.1,
                grinderVariance = 0.1
            )
        }
    }

    /**
     * Beginner barista shot profiles - lots of mistakes and inconsistency.
     */
    private fun getBeginnerShotProfiles(): List<ShotProfile> {
        return listOf(
            ShotProfile(
                coffeeWeightIn = 15.0,
                targetRatio = 1.5,
                extractionTimeSeconds = 15,
                notes = listOf(
                    "Too fast! Grind was way too coarse",
                    "Sour and thin, need to adjust everything",
                    "First shot attempt, not sure what I'm doing",
                    "Watched a YouTube video, still confused"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 22.0,
                targetRatio = 3.2,
                extractionTimeSeconds = 45,
                notes = listOf(
                    "This took forever! Grind too fine",
                    "Really bitter, over-extracted for sure",
                    "Scale ran out of coffee, had to stop early",
                    "Forgot to time it properly"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 16.5,
                targetRatio = 2.8,
                extractionTimeSeconds = 35,
                notes = listOf(
                    "Getting better but still not great",
                    "Tastes okay I think?",
                    "Not sure if this is right",
                    "At least it's drinkable this time"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 20.5,
                targetRatio = 1.8,
                extractionTimeSeconds = 22,
                notes = listOf(
                    "Stopped the shot too early",
                    "Very concentrated, probably under-extracted",
                    "Learning is hard",
                    "Why is espresso so complicated?"
                )
            )
        )
    }

    /**
     * Intermediate barista shot profiles - some consistency, still learning.
     */
    private fun getIntermediateShotProfiles(): List<ShotProfile> {
        return listOf(
            ShotProfile(
                coffeeWeightIn = 18.0,
                targetRatio = 2.1,
                extractionTimeSeconds = 26,
                notes = listOf(
                    "Getting closer to a good shot",
                    "Decent flavor, could be better",
                    "Starting to understand extraction",
                    "Progress! Still room for improvement"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 19.0,
                targetRatio = 2.3,
                extractionTimeSeconds = 31,
                notes = listOf(
                    "A bit slow, might adjust grind next time",
                    "Good body, slight bitterness",
                    "Learning to taste the difference",
                    "Better than my early attempts"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 17.5,
                targetRatio = 2.0,
                extractionTimeSeconds = 24,
                notes = listOf(
                    "Fast extraction, need finer grind",
                    "Some sourness, under-extracted",
                    "At least the ratio was good",
                    "Timing is getting more consistent"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 18.5,
                targetRatio = 2.4,
                extractionTimeSeconds = 29,
                notes = listOf(
                    "Pretty good shot actually!",
                    "Nice crema, balanced flavor",
                    "This setting might work again",
                    "Starting to dial in properly"
                )
            )
        )
    }

    /**
     * Advanced barista shot profiles - good consistency, fine-tuning.
     */
    private fun getAdvancedShotProfiles(): List<ShotProfile> {
        return listOf(
            ShotProfile(
                coffeeWeightIn = 18.0,
                targetRatio = 2.0,
                extractionTimeSeconds = 28,
                notes = listOf(
                    "Solid shot, good extraction",
                    "Nice balance of sweet and acidic",
                    "Crema looks great",
                    "Consistent results with this setting"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 19.0,
                targetRatio = 2.2,
                extractionTimeSeconds = 30,
                notes = listOf(
                    "Slight adjustment needed on grind",
                    "Good shot, could be a touch faster",
                    "Flavor profile is developing nicely",
                    "Small tweaks make big differences"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 17.0,
                targetRatio = 1.9,
                extractionTimeSeconds = 26,
                notes = listOf(
                    "Ristretto style, very intense",
                    "Concentrated flavors, good extraction",
                    "Perfect for milk drinks",
                    "Nail the timing on these shorter shots"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 20.0,
                targetRatio = 2.3,
                extractionTimeSeconds = 32,
                notes = listOf(
                    "Longer shot, good for this bean",
                    "Well-extracted, smooth finish",
                    "Finding the sweet spot",
                    "Reproducible results"
                )
            )
        )
    }

    /**
     * Expert barista shot profiles - very consistent, perfect technique.
     */
    private fun getExpertShotProfiles(): List<ShotProfile> {
        return listOf(
            ShotProfile(
                coffeeWeightIn = 18.0,
                targetRatio = 2.0,
                extractionTimeSeconds = 28,
                notes = listOf(
                    "Perfect extraction, textbook shot",
                    "Beautiful tiger striping, ideal timing",
                    "Exceptional clarity and balance",
                    "This is why I love espresso"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 18.5,
                targetRatio = 2.1,
                extractionTimeSeconds = 29,
                notes = listOf(
                    "Slight grind adjustment paid off",
                    "Complex flavor notes coming through",
                    "Optimal extraction for this bean",
                    "Peak performance achieved"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 17.5,
                targetRatio = 1.95,
                extractionTimeSeconds = 27,
                notes = listOf(
                    "Precision ristretto, intense and sweet",
                    "Perfect crema texture and color",
                    "Highlighting bean's best qualities",
                    "Masterful extraction technique"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 19.0,
                targetRatio = 2.15,
                extractionTimeSeconds = 30,
                notes = listOf(
                    "Dialed in perfectly for this roast",
                    "Exceptional balance and mouthfeel",
                    "Every variable optimized",
                    "Coffee perfection achieved"
                )
            ),
            ShotProfile(
                coffeeWeightIn = 18.2,
                targetRatio = 2.05,
                extractionTimeSeconds = 28,
                notes = listOf(
                    "Competition-level shot quality",
                    "Flawless execution from start to finish",
                    "Years of practice showing",
                    "This is what espresso should taste like"
                )
            )
        )
    }

    /**
     * Enum representing different barista skill levels.
     */
    private enum class BaristaSkillLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }

    /**
     * Data class representing skill-based variation parameters.
     */
    private data class SkillVariations(
        val weightVariance: Double,
        val timeVariance: Int,
        val ratioVariance: Double,
        val grinderVariance: Double
    )

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
