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

        /**
         * Database callback for handling database creation and opening events.
         */
        private class DatabaseCallback : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                // Create indexes for performance optimization
                createIndexes(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)

                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON")
            }

            /**
             * Create database indexes for performance optimization.
             */
            private fun createIndexes(db: SupportSQLiteDatabase) {
                // Primary indexes for foreign key performance
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_bean_id ON shots(beanId)")

                // Timestamp indexes for chronological queries and pagination
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_timestamp ON shots(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_timestamp_desc ON shots(timestamp DESC)")

                // Bean filtering indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_beans_active ON beans(isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_beans_name ON beans(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_beans_roast_date ON beans(roastDate)")

                // Shot filtering indexes for performance
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_grinder_setting ON shots(grinderSetting)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_brew_ratio ON shots(brewRatio)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_extraction_time ON shots(extractionTimeSeconds)")

                // Composite indexes for complex queries
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_bean_timestamp ON shots(beanId, timestamp DESC)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_bean_ratio ON shots(beanId, brewRatio)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_bean_time ON shots(beanId, extractionTimeSeconds)")

                // Indexes for statistics and analysis queries
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_timestamp_ratio ON shots(timestamp, brewRatio)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_timestamp_extraction ON shots(timestamp, extractionTimeSeconds)")

                // Covering index for shot list queries (includes most commonly accessed columns)
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS idx_shots_list_covering 
                    ON shots(timestamp DESC, beanId, coffeeWeightIn, coffeeWeightOut, extractionTimeSeconds, brewRatio, grinderSetting)
                """
                )

                // Index for bean statistics
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_beans_active_created ON beans(isActive, createdAt DESC)")
            }
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