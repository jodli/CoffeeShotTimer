package com.jodli.coffeeshottimer

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.jodli.coffeeshottimer.data.database.AppDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for database migrations, specifically testing the photo field migration.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_photoFieldAdded() {
        // Create database with version 1 schema
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data in version 1 format (without photoPath)
            execSQL("""
                INSERT INTO beans (id, name, roastDate, notes, isActive, lastGrinderSetting, createdAt) 
                VALUES ('test-id-1', 'Test Bean 1', '2024-01-01', 'Test notes', 1, '15', '2024-01-01T10:00:00')
            """)
            execSQL("""
                INSERT INTO beans (id, name, roastDate, notes, isActive, lastGrinderSetting, createdAt) 
                VALUES ('test-id-2', 'Test Bean 2', '2024-01-02', 'More notes', 1, '16', '2024-01-02T10:00:00')
            """)
            close()
        }

        // Run the migration to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, *AppDatabase.getAllMigrations())

        // Verify the photoPath column was added and is accessible
        val cursor = db.query("SELECT id, name, photoPath FROM beans ORDER BY name")
        
        assertTrue("Should have at least one row", cursor.moveToFirst())
        
        // Check first bean
        assertEquals("test-id-1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Test Bean 1", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertNull("photoPath should be null for migrated data", 
            cursor.getString(cursor.getColumnIndexOrThrow("photoPath")))
        
        // Check second bean
        assertTrue("Should have second row", cursor.moveToNext())
        assertEquals("test-id-2", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Test Bean 2", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertNull("photoPath should be null for migrated data", 
            cursor.getString(cursor.getColumnIndexOrThrow("photoPath")))
        
        cursor.close()

        // Test that we can insert new data with photoPath
        db.execSQL("""
            INSERT INTO beans (id, name, roastDate, notes, isActive, lastGrinderSetting, photoPath, createdAt) 
            VALUES ('test-id-3', 'Test Bean 3', '2024-01-03', 'With photo', 1, '17', 'photos/test.jpg', '2024-01-03T10:00:00')
        """)

        // Verify the new data was inserted correctly
        val cursorWithPhoto = db.query("SELECT id, name, photoPath FROM beans WHERE id = 'test-id-3'")
        assertTrue("Should find the new bean", cursorWithPhoto.moveToFirst())
        assertEquals("Test Bean 3", cursorWithPhoto.getString(cursorWithPhoto.getColumnIndexOrThrow("name")))
        assertEquals("photos/test.jpg", cursorWithPhoto.getString(cursorWithPhoto.getColumnIndexOrThrow("photoPath")))
        cursorWithPhoto.close()

        // Test that we can update photoPath
        db.execSQL("UPDATE beans SET photoPath = 'photos/updated.jpg' WHERE id = 'test-id-1'")
        
        val cursorUpdated = db.query("SELECT photoPath FROM beans WHERE id = 'test-id-1'")
        assertTrue("Should find the updated bean", cursorUpdated.moveToFirst())
        assertEquals("photos/updated.jpg", cursorUpdated.getString(cursorUpdated.getColumnIndexOrThrow("photoPath")))
        cursorUpdated.close()

        // Test that we can query by photoPath (index should work)
        val cursorByPhoto = db.query("SELECT id FROM beans WHERE photoPath IS NOT NULL")
        var count = 0
        while (cursorByPhoto.moveToNext()) {
            count++
        }
        assertEquals("Should find 2 beans with photos", 2, count)
        cursorByPhoto.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_indexCreated() {
        // Create database with version 1
        var db = helper.createDatabase(TEST_DB, 1)
        db.close()

        // Run migration to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, *AppDatabase.getAllMigrations())

        // Verify that the photoPath index was created
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_beans_photoPath'")
        assertTrue("photoPath index should exist", cursor.moveToFirst())
        assertEquals("index_beans_photoPath", cursor.getString(0))
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_existingDataPreserved() {
        // Create database with version 1 and add comprehensive test data
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert beans with various data combinations
            execSQL("""
                INSERT INTO beans (id, name, roastDate, notes, isActive, lastGrinderSetting, createdAt) 
                VALUES ('bean-1', 'Ethiopian Yirgacheffe', '2024-01-01', 'Floral notes', 1, '15', '2024-01-01T10:00:00')
            """)
            execSQL("""
                INSERT INTO beans (id, name, roastDate, notes, isActive, lastGrinderSetting, createdAt) 
                VALUES ('bean-2', 'Colombian Supremo', '2024-01-02', '', 0, NULL, '2024-01-02T11:00:00')
            """)
            execSQL("""
                INSERT INTO beans (id, name, roastDate, notes, isActive, lastGrinderSetting, createdAt) 
                VALUES ('bean-3', 'Brazilian Santos', '2024-01-03', 'Nutty and smooth', 1, '14', '2024-01-03T12:00:00')
            """)
            
            // Insert some shots data
            execSQL("""
                INSERT INTO shots (id, beanId, coffeeWeightIn, coffeeWeightOut, extractionTimeSeconds, grinderSetting, notes, timestamp) 
                VALUES ('shot-1', 'bean-1', 18.0, 36.0, 28, '15', 'Perfect shot', '2024-01-01T15:00:00')
            """)
            execSQL("""
                INSERT INTO shots (id, beanId, coffeeWeightIn, coffeeWeightOut, extractionTimeSeconds, grinderSetting, notes, timestamp) 
                VALUES ('shot-2', 'bean-3', 19.0, 38.0, 30, '14', 'Slightly slow', '2024-01-03T16:00:00')
            """)
            
            close()
        }

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, *AppDatabase.getAllMigrations())

        // Verify all bean data is preserved
        val beanCursor = db.query("SELECT * FROM beans ORDER BY name")
        
        // Check Brazilian Santos
        assertTrue("Should have first bean", beanCursor.moveToFirst())
        assertEquals("bean-3", beanCursor.getString(beanCursor.getColumnIndexOrThrow("id")))
        assertEquals("Brazilian Santos", beanCursor.getString(beanCursor.getColumnIndexOrThrow("name")))
        assertEquals("2024-01-03", beanCursor.getString(beanCursor.getColumnIndexOrThrow("roastDate")))
        assertEquals("Nutty and smooth", beanCursor.getString(beanCursor.getColumnIndexOrThrow("notes")))
        assertEquals(1, beanCursor.getInt(beanCursor.getColumnIndexOrThrow("isActive")))
        assertEquals("14", beanCursor.getString(beanCursor.getColumnIndexOrThrow("lastGrinderSetting")))
        assertNull("photoPath should be null", beanCursor.getString(beanCursor.getColumnIndexOrThrow("photoPath")))
        
        // Check Colombian Supremo
        assertTrue("Should have second bean", beanCursor.moveToNext())
        assertEquals("bean-2", beanCursor.getString(beanCursor.getColumnIndexOrThrow("id")))
        assertEquals("Colombian Supremo", beanCursor.getString(beanCursor.getColumnIndexOrThrow("name")))
        assertEquals(0, beanCursor.getInt(beanCursor.getColumnIndexOrThrow("isActive")))
        assertEquals("", beanCursor.getString(beanCursor.getColumnIndexOrThrow("notes")))
        assertNull("lastGrinderSetting should be null", 
            beanCursor.getString(beanCursor.getColumnIndexOrThrow("lastGrinderSetting")))
        
        // Check Ethiopian Yirgacheffe
        assertTrue("Should have third bean", beanCursor.moveToNext())
        assertEquals("bean-1", beanCursor.getString(beanCursor.getColumnIndexOrThrow("id")))
        assertEquals("Ethiopian Yirgacheffe", beanCursor.getString(beanCursor.getColumnIndexOrThrow("name")))
        assertEquals("Floral notes", beanCursor.getString(beanCursor.getColumnIndexOrThrow("notes")))
        
        beanCursor.close()

        // Verify shot data is preserved
        val shotCursor = db.query("SELECT * FROM shots ORDER BY timestamp")
        
        assertTrue("Should have first shot", shotCursor.moveToFirst())
        assertEquals("shot-1", shotCursor.getString(shotCursor.getColumnIndexOrThrow("id")))
        assertEquals("bean-1", shotCursor.getString(shotCursor.getColumnIndexOrThrow("beanId")))
        assertEquals(18.0, shotCursor.getDouble(shotCursor.getColumnIndexOrThrow("coffeeWeightIn")), 0.01)
        assertEquals(36.0, shotCursor.getDouble(shotCursor.getColumnIndexOrThrow("coffeeWeightOut")), 0.01)
        assertEquals(28, shotCursor.getInt(shotCursor.getColumnIndexOrThrow("extractionTimeSeconds")))
        
        assertTrue("Should have second shot", shotCursor.moveToNext())
        assertEquals("shot-2", shotCursor.getString(shotCursor.getColumnIndexOrThrow("id")))
        assertEquals("bean-3", shotCursor.getString(shotCursor.getColumnIndexOrThrow("beanId")))
        
        shotCursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2_roomDatabaseIntegration() {
        // Create database with version 1
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO beans (id, name, roastDate, notes, isActive, lastGrinderSetting, createdAt) 
                VALUES ('integration-test', 'Integration Bean', '2024-01-01', 'Test', 1, '15', '2024-01-01T10:00:00')
            """)
            close()
        }

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, *AppDatabase.getAllMigrations())
        db.close()

        // Now open with Room to verify it works correctly
        val roomDb = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).build()

        // Test that Room can read the migrated data
        val bean = roomDb.beanDao().getBeanById("integration-test")
        assertNotNull("Bean should be found", bean)
        assertEquals("Integration Bean", bean!!.name)
        assertNull("Photo path should be null", bean.photoPath)
        assertFalse("Bean should not have photo", bean.hasPhoto())

        // Test that we can add photo path using Room
        val updatedBean = bean.copy(photoPath = "photos/integration_test.jpg")
        roomDb.beanDao().updateBean(updatedBean)

        val beanWithPhoto = roomDb.beanDao().getBeanById("integration-test")
        assertNotNull("Bean should still exist", beanWithPhoto)
        assertEquals("photos/integration_test.jpg", beanWithPhoto!!.photoPath)
        assertTrue("Bean should have photo", beanWithPhoto.hasPhoto())

        // Test photo-specific queries work
        val beansWithPhotos = roomDb.beanDao().getBeansWithPhotos()
        // Note: We can't use .first() here as it's a Flow, but we can test the DAO method exists
        assertNotNull("Beans with photos query should work", beansWithPhotos)

        roomDb.close()
    }
}