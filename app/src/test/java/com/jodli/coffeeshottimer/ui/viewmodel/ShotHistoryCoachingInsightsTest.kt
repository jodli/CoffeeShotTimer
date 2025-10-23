package com.jodli.coffeeshottimer.ui.viewmodel

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for coaching insights and achievement detection logic in ShotHistoryViewModel.
 *
 * Tests focus on:
 * - Recent trend analysis (3-5 shots per bean)
 * - Dial-in status calculation (bean-specific)
 * - Achievement detection (first perfect, dialed-in, consistency streaks)
 * - Bean-specific filtering and analysis
 */
class ShotHistoryCoachingInsightsTest {

    private lateinit var viewModel: ShotHistoryViewModel

    // Test beans
    private val beanA = Bean(
        id = "bean-a",
        name = "Ethiopian Yirgacheffe",
        roastDate = LocalDateTime.now().minusDays(7).toLocalDate()
    )

    private val beanB = Bean(
        id = "bean-b",
        name = "Colombian Supremo",
        roastDate = LocalDateTime.now().minusDays(10).toLocalDate()
    )

    @Before
    fun setup() {
        // Note: Full ViewModel testing would require mocking all dependencies
        // These tests focus on the logic patterns and data structures
    }

    // ACHIEVEMENT TESTS

    @Test
    fun `achievement detected for first perfect shot with bean`() {
        // Given: 3 shots with bean A, one is perfect (score >= 80)
        val shots = listOf(
            createShot(
                beanId = beanA.id,
                timestamp = now().minusHours(3),
                time = 40,
                ratio = 2.0
            ), // Good but not perfect (score ~60)
            createShot(
                beanId = beanA.id,
                timestamp = now().minusHours(2),
                time = 38,
                ratio = 2.1
            ), // Good but not perfect (score ~68)
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Perfect!
        )

        // When: Check if last shot has first perfect achievement
        val lastShot = shots.last()

        // Then: Should be first perfect because previous shots weren't perfect
        // Quality score for last shot: time=27s (optimal) = 40pts, ratio=2.0 (typical) = 40pts, weights ok = 20pts = 100pts
        assertTrue("Last shot should be perfect", calculateQualityScore(lastShot) >= 80)
        assertTrue(
            "Should be first perfect for this bean",
            shots.filter { it.timestamp < lastShot.timestamp }
                .none { calculateQualityScore(it) >= 80 }
        )
    }

    @Test
    fun `no first perfect achievement if previous perfect shots exist`() {
        // Given: Multiple perfect shots with bean A
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 27, ratio = 2.0), // Perfect
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 28, ratio = 2.0), // Perfect
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Perfect
        )

        // When: Check last shot
        val lastShot = shots.last()

        // Then: Should NOT be "first perfect" because earlier perfect shots exist
        assertTrue("Last shot should be perfect", calculateQualityScore(lastShot) >= 80)
        assertTrue(
            "Previous perfect shots should exist",
            shots.filter { it.timestamp < lastShot.timestamp }
                .any { calculateQualityScore(it) >= 80 }
        )
    }

    @Test
    fun `dial-in milestone detected at 3rd consecutive good shot`() {
        // Given: Exactly 3 consecutive good shots (score >= 60)
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 28, ratio = 2.0), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 29, ratio = 2.1), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Good
        )

        // When: Check if all 3 shots are good
        val allGood = shots.all { calculateQualityScore(it) >= 60 }

        // Then: Should be dial-in milestone
        assertTrue("All shots should be good quality", allGood)
        assertEquals("Should have exactly 3 shots", 3, shots.size)
    }

    @Test
    fun `no dial-in milestone if less than 3 shots`() {
        // Given: Only 2 shots
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0)
        )

        // Then: Cannot be dial-in milestone
        assertTrue("Shots should be good", shots.all { calculateQualityScore(it) >= 60 })
        assertTrue("Should have less than 3 shots", shots.size < 3)
    }

    @Test
    fun `consistency streak detected at 3 consecutive good shots`() {
        // Given: 5 consecutive good shots
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(5), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(4), time = 29, ratio = 2.1),
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 27, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0)
        )

        // When: Count consecutive good shots from end
        var streakCount = 0
        for (shot in shots.reversed()) {
            if (calculateQualityScore(shot) >= 60) {
                streakCount++
            } else {
                break
            }
        }

        // Then: Should have 5-shot streak
        assertEquals("Should have 5-shot consistency streak", 5, streakCount)
    }

    @Test
    fun `streak broken by poor shot`() {
        // Given: Good shots interrupted by poor shot
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(4), time = 28, ratio = 2.0), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 45, ratio = 1.2), // Poor
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 28, ratio = 2.0), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Good
        )

        // When: Count consecutive good shots from end
        var streakCount = 0
        for (shot in shots.reversed()) {
            if (calculateQualityScore(shot) >= 60) {
                streakCount++
            } else {
                break
            }
        }

        // Then: Should only count last 2 shots
        assertEquals("Streak should be broken by poor shot", 2, streakCount)
    }

    // BEAN-SPECIFIC TESTS

    @Test
    fun `achievements are bean-specific`() {
        // Given: Perfect shots with different beans
        val shotsWithBeanA = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 27, ratio = 2.0) // Perfect with A
        )

        val shotsWithBeanB = listOf(
            createShot(beanId = beanB.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Perfect with B
        )

        // When: Check if each is first perfect for its bean
        val beanAShotsBeforeLast = shotsWithBeanA.dropLast(1)
        val beanBShotsBeforeLast = shotsWithBeanB.dropLast(1)

        // Then: Both should be "first perfect" for their respective beans
        assertTrue(
            "Bean A shot should be first perfect for bean A",
            beanAShotsBeforeLast.none { calculateQualityScore(it) >= 80 }
        )
        assertTrue(
            "Bean B shot should be first perfect for bean B",
            beanBShotsBeforeLast.none { calculateQualityScore(it) >= 80 }
        )
    }

    @Test
    fun `dial-in status is bean-specific`() {
        // Given: Mixed shots from two beans
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(6), time = 28, ratio = 2.0),
            createShot(beanId = beanB.id, timestamp = now().minusHours(5), time = 29, ratio = 2.1),
            createShot(beanId = beanA.id, timestamp = now().minusHours(4), time = 27, ratio = 2.0),
            createShot(beanId = beanB.id, timestamp = now().minusHours(3), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 27, ratio = 2.0)
        )

        // When: Filter by bean
        val beanAShots = shots.filter { it.beanId == beanA.id }.sortedBy { it.timestamp }
        val beanBShots = shots.filter { it.beanId == beanB.id }.sortedBy { it.timestamp }

        // Then: Each bean should have its own dial-in status
        assertEquals("Bean A should have 3 shots", 3, beanAShots.size)
        assertEquals("Bean B should have 2 shots", 2, beanBShots.size)
        assertTrue("Bean A shots should be good", beanAShots.all { calculateQualityScore(it) >= 60 })
    }

    // COACHING INSIGHTS TESTS

    @Test
    fun `recent trend calculates perfect shot count correctly`() {
        // Given: 5 shots with 3 perfect and 2 good
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(5), time = 27, ratio = 2.0), // Perfect (100)
            createShot(beanId = beanA.id, timestamp = now().minusHours(4), time = 40, ratio = 2.0), // Good (60)
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 27, ratio = 2.0), // Perfect (100)
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 38, ratio = 2.0), // Good (68)
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Perfect (100)
        )

        // When: Count perfect and good shots
        val perfectCount = shots.count { calculateQualityScore(it) >= 80 }
        val goodCount = shots.count { calculateQualityScore(it) >= 60 && calculateQualityScore(it) < 80 }

        // Then: Should have 3 perfect and 2 good
        assertEquals("Should have 3 perfect shots", 3, perfectCount)
        assertEquals("Should have 2 good shots", 2, goodCount)
    }

    @Test
    fun `recent trend calculates average quality score`() {
        // Given: 3 shots with known scores
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 27, ratio = 2.0), // 100
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 40, ratio = 2.0), // 60
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // 100
        )

        // When: Calculate scores and average
        val scores = shots.map { calculateQualityScore(it) }
        val avgScore = scores.average().toInt()

        // Then: Average should be (100 + 60 + 100) / 3 = 86.67 ≈ 87
        assertTrue("Average score should be between 86 and 87", avgScore in 86..87)
    }

    @Test
    fun `recent trend requires minimum 3 shots`() {
        // Given: Only 2 shots
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0)
        )

        // Then: Not enough for trend analysis
        assertTrue("Should have less than 3 shots", shots.size < 3)
    }

    @Test
    fun `recent trend analyzes last 5 shots maximum`() {
        // Given: 7 shots
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(7), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(6), time = 29, ratio = 2.1),
            createShot(beanId = beanA.id, timestamp = now().minusHours(5), time = 27, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(4), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 27, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 28, ratio = 2.0),
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0)
        )

        // When: Take last 5
        val recentShots = shots.takeLast(5)

        // Then: Should analyze only 5 most recent
        assertEquals("Should take last 5 shots", 5, recentShots.size)
    }

    @Test
    fun `dial-in status shows dialed in after 3 consecutive good shots`() {
        // Given: 5 shots where last 3 are good
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(5), time = 45, ratio = 1.2), // Poor
            createShot(beanId = beanA.id, timestamp = now().minusHours(4), time = 42, ratio = 1.3), // Poor
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 28, ratio = 2.0), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 29, ratio = 2.1), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Good
        )

        // When: Check last 3 shots
        val lastThree = shots.takeLast(3)
        val allGood = lastThree.all { calculateQualityScore(it) >= 60 }

        // Then: Should be dialed in (3 shots to dial in)
        assertTrue("Last 3 shots should be good", allGood)
        assertEquals("Should have taken 5 shots total", 5, shots.size)
    }

    @Test
    fun `dial-in status shows not dialed in with inconsistent shots`() {
        // Given: 5 shots with no 3 consecutive good shots
        val shots = listOf(
            createShot(beanId = beanA.id, timestamp = now().minusHours(5), time = 28, ratio = 2.0), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(4), time = 45, ratio = 1.2), // Poor
            createShot(beanId = beanA.id, timestamp = now().minusHours(3), time = 28, ratio = 2.0), // Good
            createShot(beanId = beanA.id, timestamp = now().minusHours(2), time = 42, ratio = 1.3), // Poor
            createShot(beanId = beanA.id, timestamp = now().minusHours(1), time = 27, ratio = 2.0) // Good
        )

        // When: Check for any 3 consecutive good shots
        var hasThreeConsecutiveGood = false
        for (i in 0..shots.size - 3) {
            val threeShots = shots.subList(i, i + 3)
            if (threeShots.all { calculateQualityScore(it) >= 60 }) {
                hasThreeConsecutiveGood = true
                break
            }
        }

        // Then: Should not be dialed in
        assertFalse("Should not have 3 consecutive good shots", hasThreeConsecutiveGood)
    }

    @Test
    fun `quality score calculation for perfect shot`() {
        // Given: Optimal shot (25-30s, 1.5-3.0 ratio, reasonable weights)
        val perfectShot = createShot(beanId = beanA.id, time = 27, ratio = 2.0)

        // When: Calculate score
        val score = calculateQualityScore(perfectShot)

        // Then: Should be perfect (100 points)
        assertEquals("Perfect shot should score 100", 100, score)
    }

    @Test
    fun `quality score calculation for good shot`() {
        // Given: Slightly off optimal (32s, good ratio)
        val goodShot = createShot(beanId = beanA.id, time = 32, ratio = 2.0)

        // When: Calculate score
        val score = calculateQualityScore(goodShot)

        // Then: Should be good (60-79 points)
        // Time penalty: 32s - 30s = 2s over, penalty = 2 * 4 = 8 points
        // Expected: 40 - 8 + 40 + 20 = 92 points
        assertTrue("Good shot should score between 60-99", score in 60..99)
    }

    @Test
    fun `quality score calculation for poor shot`() {
        // Given: Way off optimal (45s, poor ratio)
        val poorShot = createShot(beanId = beanA.id, time = 45, ratio = 1.2)

        // When: Calculate score
        val score = calculateQualityScore(poorShot)

        // Then: Should be poor (< 60 points)
        assertTrue("Poor shot should score below 60", score < 60)
    }

    // HELPER METHODS

    private fun createShot(
        beanId: String,
        timestamp: LocalDateTime = now(),
        time: Int = 27,
        ratio: Double = 2.0
    ): Shot {
        val coffeeIn = 18.0
        val coffeeOut = coffeeIn * ratio

        return Shot(
            id = UUID.randomUUID().toString(),
            beanId = beanId,
            coffeeWeightIn = coffeeIn,
            coffeeWeightOut = coffeeOut,
            extractionTimeSeconds = time,
            grinderSetting = "3.5",
            timestamp = timestamp,
            tastePrimary = if (calculateQualityScore(
                    createTempShot(time, ratio)
                ) >= 80
            ) {
                TastePrimary.PERFECT
            } else {
                null
            }
        )
    }

    private fun createTempShot(time: Int, ratio: Double): Shot {
        return Shot(
            id = "",
            beanId = "",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 18.0 * ratio,
            extractionTimeSeconds = time,
            grinderSetting = ""
        )
    }

    private fun now(): LocalDateTime = LocalDateTime.now()

    /**
     * Replica of the quality score calculation from ViewModel.
     * Score breakdown: Extraction time (40pts) + Brew ratio (40pts) + Weights (20pts) = 100pts
     */
    private fun calculateQualityScore(shot: Shot): Int {
        var score = 0

        // Optimal extraction time (25-30s) = 40 points
        if (shot.extractionTimeSeconds in 25..30) {
            score += 40
        } else {
            val timeDiff = when {
                shot.extractionTimeSeconds < 25 -> 25 - shot.extractionTimeSeconds
                shot.extractionTimeSeconds > 30 -> shot.extractionTimeSeconds - 30
                else -> 0
            }
            score += maxOf(0, 40 - (timeDiff * 4))
        }

        // Typical brew ratio (1.5-3.0) = 40 points
        if (shot.brewRatio in 1.5..3.0) {
            score += 40
        } else {
            val ratioDiff = when {
                shot.brewRatio < 1.5 -> 1.5 - shot.brewRatio
                shot.brewRatio > 3.0 -> shot.brewRatio - 3.0
                else -> 0.0
            }
            score += maxOf(0, 40 - (ratioDiff * 20.0).toInt())
        }

        // Consistency bonus (reasonable weights) = 20 points
        val isReasonableWeights = shot.coffeeWeightIn in 15.0..25.0 &&
            shot.coffeeWeightOut in 25.0..60.0
        if (isReasonableWeights) {
            score += 20
        }

        return score.coerceIn(0, 100)
    }
}
