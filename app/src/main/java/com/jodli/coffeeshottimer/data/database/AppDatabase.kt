package com.jodli.coffeeshottimer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
    version = 1,
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
         * Currently returns empty array as we're on version 1.
         */
        fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // Future migrations will be added here
                // MIGRATION_1_2,
                // MIGRATION_2_3,
                // etc.
            )
        }

        // Example migration for future use (commented out as we're on version 1)
        /*
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Add a new column to beans table
                database.execSQL("ALTER TABLE beans ADD COLUMN origin TEXT DEFAULT ''")
            }
        }
        */
    }
}