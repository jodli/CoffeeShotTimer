package com.jodli.coffeeshottimer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.GrinderConfigDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.model.Shot

/**
 * Room database configuration for the Espresso Shot Tracker app.
 *
 * This database stores coffee beans and espresso shot records with proper
 * relationships and performance optimizations.
 */
@Database(
    entities = [Bean::class, Shot::class, GrinderConfiguration::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun beanDao(): BeanDao
    abstract fun shotDao(): ShotDao
    abstract fun grinderConfigDao(): GrinderConfigDao

    companion object {
        const val DATABASE_NAME = "espresso_tracker_database"

        /**
         * Get all database migrations.
         */
        fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                MIGRATION_1_2,
                MIGRATION_2_3
            )
        }

        /**
         * Migration from version 1 to 2: Add photoPath field to beans table.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // 1) Add photoPath column to beans table (nullable, no explicit default)
                    //    Room expects no explicit default for nullable columns.
                    db.execSQL("ALTER TABLE beans ADD COLUMN photoPath TEXT")

                    // 2) Ensure indexes match Room's expected (canonical) names.
                    //    Drop any legacy/custom indexes if they exist, then (re)create the expected ones.
                    //    These drops are safe as IF EXISTS and no-ops if not present.
                    db.execSQL("DROP INDEX IF EXISTS idx_beans_active")
                    db.execSQL("DROP INDEX IF EXISTS idx_beans_name")
                    db.execSQL("DROP INDEX IF EXISTS idx_beans_roast_date")

                    // 3) Create canonical indexes with the names Room generated from @Entity(indices=...)
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_isActive ON beans (isActive)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_beans_name ON beans (name)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_roastDate ON beans (roastDate)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_createdAt ON beans (createdAt)")

                    // 4) Add index for photoPath to optimize photo-related queries
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_photoPath ON beans (photoPath)")

                    // 5) Normalize legacy index names on 'shots' table to match Room's expected schema
                    //    Drop old custom names (if they exist) and create canonical ones.
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_bean_id")
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_bean_timestamp")
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_grinder_setting")
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_timestamp")

                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_beanId ON shots (beanId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_timestamp ON shots (timestamp)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_grinderSetting ON shots (grinderSetting)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_beanId_timestamp ON shots (beanId, timestamp)")
                } catch (e: Exception) {
                    // Surface the failure so Room can handle it and report clearly
                    throw RuntimeException("Migration 1->2 failed: ${e.message}", e)
                }
            }
        }

        /**
         * Migration from version 2 to 3: Add grinder_configuration table.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // Ensure legacy indices from older 2.x schemas are cleaned up so schema matches Room expectations
                    // Drop old/custom bean indices if present
                    db.execSQL("DROP INDEX IF EXISTS idx_beans_active")
                    db.execSQL("DROP INDEX IF EXISTS idx_beans_name")
                    db.execSQL("DROP INDEX IF EXISTS idx_beans_roast_date")

                    // Recreate canonical bean indices that Room expects (idempotent)
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_isActive ON beans (isActive)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_beans_name ON beans (name)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_roastDate ON beans (roastDate)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_createdAt ON beans (createdAt)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_beans_photoPath ON beans (photoPath)")

                    // Also normalize legacy index names on 'shots' table to match Room's expected schema
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_bean_id")
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_bean_timestamp")
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_grinder_setting")
                    db.execSQL("DROP INDEX IF EXISTS idx_shots_timestamp")

                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_beanId ON shots (beanId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_timestamp ON shots (timestamp)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_grinderSetting ON shots (grinderSetting)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_shots_beanId_timestamp ON shots (beanId, timestamp)")

                    // Create grinder_configuration table
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS grinder_configuration (
                            id TEXT NOT NULL PRIMARY KEY,
                            scaleMin INTEGER NOT NULL,
                            scaleMax INTEGER NOT NULL,
                            createdAt TEXT NOT NULL
                        )
                    """)

                    // Create index for createdAt to optimize queries for most recent configuration
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_grinder_configuration_createdAt ON grinder_configuration (createdAt)")

                } catch (e: Exception) {
                    // Surface the failure so Room can handle it and report clearly
                    throw RuntimeException("Migration 2->3 failed: ${e.message}", e)
                }
            }
        }
    }
}