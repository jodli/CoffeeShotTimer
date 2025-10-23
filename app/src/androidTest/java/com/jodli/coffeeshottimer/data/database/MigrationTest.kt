package com.jodli.coffeeshottimer.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Tests for Room database migrations.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate8To9_createsRecommendationTable() {
        // Create database at version 8
        helper.createDatabase(testDb, 8).apply {
            // Insert a test shot (without recommendation fields - they don't exist yet)
            execSQL(
                """
                INSERT INTO shots (
                    id, beanId, coffeeWeightIn, coffeeWeightOut, 
                    extractionTimeSeconds, grinderSetting, notes, timestamp
                ) VALUES (
                    'test-shot-1', 'test-bean', 18.0, 36.0, 
                    28, '5.5', 'Test notes', '2025-01-01T10:00:00'
                )
                """.trimIndent()
            )
            close()
        }

        // Run migration to version 9
        helper.runMigrationsAndValidate(testDb, 9, true, AppDatabase.MIGRATION_8_9).apply {
            // Verify shot_recommendations table was created
            query("SELECT * FROM shot_recommendations").use { cursor ->
                assertNotNull(cursor)
                // Table should be empty (no data to migrate)
                assertEquals(0, cursor.count)
            }

            // Verify shot still exists
            query("SELECT * FROM shots WHERE id = 'test-shot-1'").use { cursor ->
                assertEquals(1, cursor.count)
            }

            // Verify indices were created
            query(
                "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='shot_recommendations'"
            ).use { cursor ->
                val indices = mutableListOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    indices.add(cursor.getString(nameIndex))
                }
                assertTrue("Should have shotId index", indices.any { it.contains("shotId") })
                assertTrue("Should have timestamp index", indices.any { it.contains("timestamp") })
                assertTrue("Should have wasFollowed index", indices.any { it.contains("wasFollowed") })
            }

            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9_allowsInsertingRecommendations() {
        // Create database at version 8
        helper.createDatabase(testDb, 8).apply {
            // Insert a test shot
            execSQL(
                """
                INSERT INTO shots (
                    id, beanId, coffeeWeightIn, coffeeWeightOut, 
                    extractionTimeSeconds, grinderSetting, notes, timestamp
                ) VALUES (
                    'test-shot-insert', 'test-bean', 18.0, 36.0, 
                    28, '5.5', 'Test for insert', '2025-01-01T10:00:00'
                )
                """.trimIndent()
            )
            close()
        }

        // Run migration to version 9
        helper.runMigrationsAndValidate(testDb, 9, true, AppDatabase.MIGRATION_8_9).apply {
            // Now insert a recommendation
            execSQL(
                """
                INSERT INTO shot_recommendations (
                    id, shotId, recommendedGrindSetting, adjustmentDirection,
                    wasFollowed, confidenceLevel, reasonCode, timestamp
                ) VALUES (
                    'rec-1', 'test-shot-insert', '5.0', 'FINER',
                    1, 'MEDIUM', 'TIME_TOO_FAST', '2025-01-01T10:00:00'
                )
                """.trimIndent()
            )

            // Verify recommendation was inserted
            query("SELECT * FROM shot_recommendations WHERE shotId = 'test-shot-insert'").use { cursor ->
                assertEquals(1, cursor.count)
                if (cursor.moveToFirst()) {
                    val recommendedGrindIndex = cursor.getColumnIndex("recommendedGrindSetting")
                    assertEquals("5.0", cursor.getString(recommendedGrindIndex))
                }
            }

            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9_preservesForeignKeyConstraints() {
        // Create database at version 8
        helper.createDatabase(testDb, 8).apply {
            // Insert bean first
            execSQL(
                """
                INSERT INTO beans (
                    id, name, origin, roastLevel, roastDate, 
                    purchaseDate, notes, isActive, createdAt
                ) VALUES (
                    'test-bean-fk', 'Test Bean FK', 'Test Origin', 'MEDIUM', '2025-01-01',
                    '2025-01-01', 'Test', 1, '2025-01-01T00:00:00'
                )
                """.trimIndent()
            )

            // Insert shot
            execSQL(
                """
                INSERT INTO shots (
                    id, beanId, coffeeWeightIn, coffeeWeightOut, 
                    extractionTimeSeconds, grinderSetting, notes, timestamp
                ) VALUES (
                    'test-shot-fk', 'test-bean-fk', 18.0, 36.0, 
                    28, '5.5', 'FK test', '2025-01-01T10:00:00'
                )
                """.trimIndent()
            )
            close()
        }

        // Run migration to version 9
        helper.runMigrationsAndValidate(testDb, 9, true, AppDatabase.MIGRATION_8_9).apply {
            // Insert a recommendation for the shot
            execSQL(
                """
                INSERT INTO shot_recommendations (
                    id, shotId, recommendedGrindSetting, adjustmentDirection,
                    wasFollowed, confidenceLevel, reasonCode, timestamp
                ) VALUES (
                    'rec-fk', 'test-shot-fk', '5.0', 'FINER',
                    1, 'MEDIUM', 'TIME_TOO_FAST', '2025-01-01T10:00:00'
                )
                """.trimIndent()
            )

            // Verify recommendation exists
            query("SELECT * FROM shot_recommendations WHERE shotId = 'test-shot-fk'").use { cursor ->
                assertEquals(1, cursor.count)
            }

            // Delete the shot - recommendation should cascade delete due to FK
            execSQL("DELETE FROM shots WHERE id = 'test-shot-fk'")

            // Verify recommendation was cascade deleted
            query("SELECT * FROM shot_recommendations WHERE shotId = 'test-shot-fk'").use { cursor ->
                assertEquals(0, cursor.count)
            }

            close()
        }
    }
}
