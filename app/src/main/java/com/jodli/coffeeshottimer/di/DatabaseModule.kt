package com.jodli.coffeeshottimer.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jodli.coffeeshottimer.BuildConfig
import com.jodli.coffeeshottimer.data.dao.BasketConfigDao
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.dao.GrinderConfigDao
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
            // Indices are now created by Room annotations, no manual creation needed
        }

        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)

            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON")
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
     * Provides the GrinderConfigDao instance.
     *
     * @param database The AppDatabase instance
     * @return GrinderConfigDao instance
     */
    @Provides
    fun provideGrinderConfigDao(database: AppDatabase): GrinderConfigDao {
        return database.grinderConfigDao()
    }

    /**
     * Provides the BasketConfigDao instance.
     *
     * @param database The AppDatabase instance
     * @return BasketConfigDao instance
     */
    @Provides
    fun provideBasketConfigDao(database: AppDatabase): BasketConfigDao {
        return database.basketConfigDao()
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
        shotDao: ShotDao,
        grinderConfigDao: GrinderConfigDao
    ): DatabasePopulator? {
        return if (BuildConfig.DEBUG) {
            DatabasePopulator(beanDao, shotDao, grinderConfigDao)
        } else {
            null
        }
    }
}
