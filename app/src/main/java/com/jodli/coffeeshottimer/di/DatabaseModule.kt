package com.jodli.coffeeshottimer.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.ShotDao
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.util.DatabasePopulator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 * This module provides the Room database instance and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance.
     * Uses singleton scope to ensure only one database instance exists.
     *
     * @param context Application context
     * @return AppDatabase instance
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addCallback(DatabaseCallback())
            .addMigrations(*AppDatabase.getAllMigrations())
            .build()
    }

    /**
     * Database callback for handling database creation and opening events.
     */
    private class DatabaseCallback : RoomDatabase.Callback() {

        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)

            // Create indexes for performance optimization
            createIndexes(db)
        }

        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)

            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON")
        }

        /**
         * Create database indexes for performance optimization.
         */
        private fun createIndexes(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Index on shots.beanId for foreign key performance
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_bean_id ON shots(beanId)")

            // Index on shots.timestamp for chronological queries
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_timestamp ON shots(timestamp)")

            // Index on beans.isActive for filtering active beans
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_beans_active ON beans(isActive)")

            // Index on beans.name for name-based queries and uniqueness
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_beans_name ON beans(name)")

            // Index on shots.grinderSetting for grinder setting queries
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_grinder_setting ON shots(grinderSetting)")

            // Composite index for shots filtered by bean and timestamp
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shots_bean_timestamp ON shots(beanId, timestamp)")

            // Index on beans.roastDate for date-based queries
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_beans_roast_date ON beans(roastDate)")
        }
    }

    /**
     * Provides the BeanDao instance.
     *
     * @param database The AppDatabase instance
     * @return BeanDao instance
     */
    @Provides
    fun provideBeanDao(database: AppDatabase): BeanDao {
        return database.beanDao()
    }

    /**
     * Provides the ShotDao instance.
     *
     * @param database The AppDatabase instance
     * @return ShotDao instance
     */
    @Provides
    fun provideShotDao(database: AppDatabase): ShotDao {
        return database.shotDao()
    }

    /**
     * Provides the DatabasePopulator instance for debug builds only.
     * This utility is used for populating the database with test data
     * and clearing database content during development and testing.
     * 
     * The provider is conditionally compiled - it only exists in debug builds
     * to ensure no debug code is included in release builds.
     *
     * @param beanDao The BeanDao instance for database operations
     * @param shotDao The ShotDao instance for database operations
     * @return DatabasePopulator instance (debug builds only)
     */
    @Provides
    @Singleton
    fun provideDatabasePopulator(
        beanDao: BeanDao,
        shotDao: ShotDao
    ): DatabasePopulator? {
        return if (BuildConfig.DEBUG) {
            DatabasePopulator(beanDao, shotDao)
        } else {
            null
        }
    }
}