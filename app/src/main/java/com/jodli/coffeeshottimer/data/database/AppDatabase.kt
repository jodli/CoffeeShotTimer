package com.jodli.coffeeshottimer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot

/**
 * Room database configuration for the Espresso Shot Tracker app.
 *
 * This database stores coffee beans and espresso shot records with proper
 * relationships and performance optimizations.
 */
@Database(
    entities = [Bean::class, Shot::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun beanDao(): BeanDao
    abstract fun shotDao(): ShotDao

    companion object {
        const val DATABASE_NAME = "espresso_tracker_database"

        /**
         * Get all database migrations.
         */
        fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                MIGRATION_1_2
            )
        }

        /**
         * Migration from version 1 to 2: Add photoPath field to beans table.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add photoPath column to beans table
                    database.execSQL("ALTER TABLE beans ADD COLUMN photoPath TEXT DEFAULT NULL")
                    
                    // Add index for photoPath to optimize photo-related queries
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_beans_photoPath ON beans (photoPath)")
                } catch (e: Exception) {
                    // Log the error but don't fail the migration - let Room handle it
                    // This prevents app crashes due to database corruption
                    throw RuntimeException("Migration 1->2 failed: ${e.message}", e)
                }
            }
        }
    }
}