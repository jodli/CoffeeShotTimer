package com.jodli.coffeeshottimer.ui.validation

import com.jodli.coffeeshottimer.R
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides validation-specific string resources with proper formatting.
 * This keeps validation strings centralized and easily testable.
 */
@Singleton
class ValidationStringProvider @Inject constructor(
    private val stringResourceProvider: StringResourceProvider
) {

    // Bean name validation
    fun getBeanNameRequiredError(): String =
        stringResourceProvider.getString(R.string.text_bean_name_required)

    fun getBeanNameMinimumLengthError(minLength: Int): String =
        stringResourceProvider.getString(R.string.validation_bean_name_minimum_length, minLength)

    fun getBeanNameMaximumLengthError(maxLength: Int): String =
        stringResourceProvider.getString(R.string.validation_bean_name_maximum_length, maxLength)

    fun getBeanNameInvalidCharactersError(): String =
        stringResourceProvider.getString(R.string.validation_bean_name_invalid_characters)

    fun getBeanNameExistsError(): String =
        stringResourceProvider.getString(R.string.text_bean_name_exists)

    // Roast date validation
    fun getRoastDateFutureError(): String =
        stringResourceProvider.getString(R.string.validation_roast_date_future)

    fun getRoastDateTooOldError(): String =
        stringResourceProvider.getString(R.string.validation_roast_date_too_old)

    // Notes validation
    fun getNotesMaximumLengthError(maxLength: Int): String =
        stringResourceProvider.getString(R.string.validation_notes_maximum_length, maxLength)

    // Tips and helpful messages
    fun getDescriptiveNameTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_descriptive_name)

    fun getBasicPunctuationTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_basic_punctuation)

    fun getUniqueNameTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_unique_name)

    fun getRoastDateTodayTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_roast_date_today)

    fun getOldBeansNote(): String =
        stringResourceProvider.getString(R.string.validation_note_old_beans)

    fun getCharacterLimitNote(): String =
        stringResourceProvider.getString(R.string.validation_note_character_limit)

    // Brew ratio warnings
    fun getRatioConcentratedWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_concentrated)

    fun getRatioDilutedWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_diluted)

    fun getRatioHigherWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_higher)

    fun getRatioLowerWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_lower)
}
