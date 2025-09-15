package com.jodli.coffeeshottimer.di

import android.content.Context
import android.content.SharedPreferences
import com.jodli.coffeeshottimer.data.onboarding.OnboardingManager
import com.jodli.coffeeshottimer.data.onboarding.OnboardingPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for providing onboarding-related dependencies.
 * This module provides the onboarding manager and its required SharedPreferences.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    companion object {
        /**
         * Provides SharedPreferences specifically for onboarding data.
         * Uses a separate preferences file to isolate onboarding state from other app preferences.
         *
         * @param context Application context
         * @return SharedPreferences instance for onboarding
         */
        @Provides
        @Singleton
        @OnboardingPrefs
        fun provideOnboardingSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences {
            return context.getSharedPreferences(
                ONBOARDING_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
        }

        /**
         * Provides SharedPreferences specifically for grind recommendation data.
         * Uses a separate preferences file to isolate recommendation data from other app preferences.
         *
         * @param context Application context
         * @return SharedPreferences instance for recommendations
         */
        @Provides
        @Singleton
        @RecommendationPrefs
        fun provideRecommendationSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences {
            return context.getSharedPreferences(
                RECOMMENDATION_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
        }

        private const val ONBOARDING_PREFERENCES_NAME = "onboarding_preferences"
        private const val RECOMMENDATION_PREFERENCES_NAME = "recommendation_preferences"
    }

    /**
     * Binds the OnboardingPreferences implementation to the OnboardingManager interface.
     * Uses singleton scope to ensure consistent onboarding state across the app.
     *
     * @param onboardingPreferences The implementation to bind
     * @return OnboardingManager interface
     */
    @Binds
    @Singleton
    abstract fun bindOnboardingManager(
        onboardingPreferences: OnboardingPreferences
    ): OnboardingManager
}

/**
 * Qualifier annotation to distinguish onboarding SharedPreferences from other SharedPreferences.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnboardingPrefs

/**
 * Qualifier annotation to distinguish recommendation SharedPreferences from other SharedPreferences.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RecommendationPrefs
