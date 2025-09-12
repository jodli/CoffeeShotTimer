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

    // Field labels removed - sliders now handle validation by constraining values

    // Basic validation errors
    fun getFieldRequiredError(fieldName: String): String =
        stringResourceProvider.getString(R.string.validation_field_required, fieldName)

    fun getValidNumberError(): String =
        stringResourceProvider.getString(R.string.validation_valid_number)


    // Extraction time validation
    fun getExtractionTimeMinimumError(minTime: Int): String =
        stringResourceProvider.getString(R.string.validation_extraction_time_minimum, minTime)

    fun getExtractionTimeMaximumError(maxTime: Int): String =
        stringResourceProvider.getString(R.string.validation_extraction_time_maximum, maxTime)

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


    // Cross-field validation
    fun getOutputWeightLessThanInputError(): String =
        stringResourceProvider.getString(R.string.validation_output_weight_less_than_input)

    // Tips and helpful messages
    fun getGrinderSettingTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_grinder_setting)

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

    fun getNotesHelpfulTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_notes_helpful)

    fun getCharacterLimitNote(): String =
        stringResourceProvider.getString(R.string.validation_note_character_limit)

    fun getShortExtractionSourTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_short_extraction_sour)

    fun getLongExtractionBitterTip(): String =
        stringResourceProvider.getString(R.string.validation_tip_long_extraction_bitter)

    // Brew ratio warnings
    fun getRatioConcentratedWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_concentrated)

    fun getRatioDilutedWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_diluted)

    fun getRatioHigherWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_higher)

    fun getRatioLowerWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_ratio_lower)

    // Extraction time warnings
    fun getGrindFinerWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_grind_finer)

    fun getGrindCoarserWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_grind_coarser)

    fun getOptimalTimeSuccess(): String =
        stringResourceProvider.getString(R.string.validation_success_optimal_time)

    // Bean age warnings
    fun getVeryFreshBeansWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_very_fresh_beans)

    fun getFreshBeansSuccess(): String =
        stringResourceProvider.getString(R.string.validation_success_fresh_beans)

    fun getAgingBeansWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_aging_beans)

    fun getOldBeansWarning(): String =
        stringResourceProvider.getString(R.string.validation_warning_old_beans)

    // Content descriptions
    fun getErrorContentDescription(): String =
        stringResourceProvider.getString(R.string.validation_cd_error)

    fun getWarningContentDescription(): String =
        stringResourceProvider.getString(R.string.validation_cd_warning)
}
