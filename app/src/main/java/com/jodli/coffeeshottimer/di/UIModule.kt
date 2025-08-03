package com.jodli.coffeeshottimer.di

import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import com.jodli.coffeeshottimer.ui.validation.ValidationStringProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing UI-related dependencies.
 * This module provides UI utilities, string providers, and validation components.
 */
@Module
@InstallIn(SingletonComponent::class)
object UIModule {

    /**
     * Provides the ValidationStringProvider instance.
     * Uses singleton scope to ensure consistent validation strings across the app.
     *
     * @param stringResourceProvider The StringResourceProvider dependency
     * @return ValidationStringProvider instance
     */
    @Provides
    @Singleton
    fun provideValidationStringProvider(
        stringResourceProvider: StringResourceProvider
    ): ValidationStringProvider {
        return ValidationStringProvider(stringResourceProvider)
    }

    /**
     * Provides the ValidationUtils instance.
     * Uses singleton scope to ensure consistent validation behavior across the app.
     *
     * @param validationStringProvider The ValidationStringProvider dependency
     * @return ValidationUtils instance
     */
    @Provides
    @Singleton
    fun provideValidationUtils(
        validationStringProvider: ValidationStringProvider
    ): ValidationUtils {
        return ValidationUtils(validationStringProvider)
    }
}
