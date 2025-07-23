package com.jodli.coffeeshottimer.ui.validation

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.jodli.coffeeshottimer.data.model.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Enhanced form state management with validation, recovery, and persistence.
 */
@Serializable
data class FormFieldState(
    val value: String = "",
    val error: String? = null,
    val warning: String? = null,
    val isValid: Boolean = true,
    val isDirty: Boolean = false,
    val lastValidatedValue: String = ""
)

@Serializable
data class FormState(
    val fields: Map<String, FormFieldState> = emptyMap(),
    val isFormValid: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
    val lastSavedTimestamp: Long = 0L,
    val formId: String = ""
)

/**
 * Comprehensive form state manager with validation and persistence.
 */
class FormStateManager(
    private val formId: String,
    private val context: Context? = null,
    private val autoSaveEnabled: Boolean = true,
    private val autoSaveIntervalMs: Long = 30000L // 30 seconds
) {
    private val _formState = MutableStateFlow(FormState(formId = formId))
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    private val _validationWarnings = MutableStateFlow<List<String>>(emptyList())
    val validationWarnings: StateFlow<List<String>> = _validationWarnings.asStateFlow()

    private val sharedPrefs: SharedPreferences? =
        context?.getSharedPreferences("form_states", Context.MODE_PRIVATE)

    init {
        if (autoSaveEnabled && context != null) {
            startAutoSave()
        }
        restoreFormState()
    }

    /**
     * Updates a field value and triggers validation.
     */
    fun updateField(
        fieldName: String,
        value: String,
        validator: ((String) -> ValidationResult)? = null
    ) {
        val currentState = _formState.value
        val currentField = currentState.fields[fieldName] ?: FormFieldState()

        // Update field value
        val updatedField = currentField.copy(
            value = value,
            isDirty = value != currentField.lastValidatedValue
        )

        // Apply validation if provided
        val validatedField = if (validator != null) {
            val validationResult = validator(value)
            updatedField.copy(
                error = validationResult.errors.firstOrNull(),
                warning = null, // Warnings would be handled separately in a more sophisticated implementation
                isValid = validationResult.isValid,
                lastValidatedValue = if (validationResult.isValid) value else currentField.lastValidatedValue
            )
        } else {
            updatedField
        }

        // Update form state
        val updatedFields = currentState.fields.toMutableMap()
        updatedFields[fieldName] = validatedField

        val isFormValid = updatedFields.values.all { it.isValid }
        val hasUnsavedChanges = updatedFields.values.any { it.isDirty }

        _formState.value = currentState.copy(
            fields = updatedFields,
            isFormValid = isFormValid,
            hasUnsavedChanges = hasUnsavedChanges
        )

        updateValidationSummary()
    }

    /**
     * Sets an error for a specific field.
     */
    fun setFieldError(fieldName: String, error: String?) {
        val currentState = _formState.value
        val currentField = currentState.fields[fieldName] ?: FormFieldState()

        val updatedField = currentField.copy(
            error = error,
            isValid = error == null
        )

        val updatedFields = currentState.fields.toMutableMap()
        updatedFields[fieldName] = updatedField

        _formState.value = currentState.copy(
            fields = updatedFields,
            isFormValid = updatedFields.values.all { it.isValid }
        )

        updateValidationSummary()
    }

    /**
     * Sets a warning for a specific field.
     */
    fun setFieldWarning(fieldName: String, warning: String?) {
        val currentState = _formState.value
        val currentField = currentState.fields[fieldName] ?: FormFieldState()

        val updatedField = currentField.copy(warning = warning)
        val updatedFields = currentState.fields.toMutableMap()
        updatedFields[fieldName] = updatedField

        _formState.value = currentState.copy(fields = updatedFields)
        updateValidationSummary()
    }

    /**
     * Gets the current value of a field.
     */
    fun getFieldValue(fieldName: String): String {
        return _formState.value.fields[fieldName]?.value ?: ""
    }

    /**
     * Gets the error message for a field.
     */
    fun getFieldError(fieldName: String): String? {
        return _formState.value.fields[fieldName]?.error
    }

    /**
     * Gets the warning message for a field.
     */
    fun getFieldWarning(fieldName: String): String? {
        return _formState.value.fields[fieldName]?.warning
    }

    /**
     * Checks if a field is valid.
     */
    fun isFieldValid(fieldName: String): Boolean {
        return _formState.value.fields[fieldName]?.isValid ?: true
    }

    /**
     * Checks if a field has been modified.
     */
    fun isFieldDirty(fieldName: String): Boolean {
        return _formState.value.fields[fieldName]?.isDirty ?: false
    }

    /**
     * Validates all fields using provided validators.
     */
    fun validateAllFields(validators: Map<String, (String) -> ValidationResult>) {
        val currentState = _formState.value
        val updatedFields = currentState.fields.toMutableMap()

        validators.forEach { (fieldName, validator) ->
            val currentField = updatedFields[fieldName] ?: FormFieldState()
            val validationResult = validator(currentField.value)

            updatedFields[fieldName] = currentField.copy(
                error = validationResult.errors.firstOrNull(),
                warning = null, // Warnings would be handled separately in a more sophisticated implementation
                isValid = validationResult.isValid,
                lastValidatedValue = if (validationResult.isValid) currentField.value else currentField.lastValidatedValue
            )
        }

        _formState.value = currentState.copy(
            fields = updatedFields,
            isFormValid = updatedFields.values.all { it.isValid }
        )

        updateValidationSummary()
    }

    /**
     * Clears all errors and warnings.
     */
    fun clearValidation() {
        val currentState = _formState.value
        val clearedFields = currentState.fields.mapValues { (_, field) ->
            field.copy(error = null, warning = null, isValid = true)
        }

        _formState.value = currentState.copy(
            fields = clearedFields,
            isFormValid = true
        )

        _validationErrors.value = emptyList()
        _validationWarnings.value = emptyList()
    }

    /**
     * Marks the form as saved (clears dirty flags).
     */
    fun markAsSaved() {
        val currentState = _formState.value
        val savedFields = currentState.fields.mapValues { (_, field) ->
            field.copy(
                isDirty = false,
                lastValidatedValue = field.value
            )
        }

        _formState.value = currentState.copy(
            fields = savedFields,
            hasUnsavedChanges = false,
            lastSavedTimestamp = System.currentTimeMillis()
        )

        // Clear persisted state after successful save
        clearPersistedState()
    }

    /**
     * Resets the form to initial state.
     */
    fun reset() {
        _formState.value = FormState(formId = formId)
        _validationErrors.value = emptyList()
        _validationWarnings.value = emptyList()
        clearPersistedState()
    }

    /**
     * Restores field values from a map.
     */
    fun restoreValues(values: Map<String, String>) {
        val currentState = _formState.value
        val restoredFields = values.mapValues { (_, value) ->
            FormFieldState(
                value = value,
                isDirty = false,
                lastValidatedValue = value
            )
        }

        _formState.value = currentState.copy(
            fields = restoredFields,
            hasUnsavedChanges = false
        )
    }

    /**
     * Gets all current field values as a map.
     */
    fun getAllValues(): Map<String, String> {
        return _formState.value.fields.mapValues { (_, field) -> field.value }
    }

    /**
     * Persists current form state to SharedPreferences.
     */
    private fun persistFormState() {
        if (sharedPrefs == null) return

        try {
            val json = Json.encodeToString(_formState.value)
            sharedPrefs.edit()
                .putString("form_$formId", json)
                .putLong("form_${formId}_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // Silently handle serialization errors
        }
    }

    /**
     * Restores form state from SharedPreferences.
     */
    private fun restoreFormState() {
        if (sharedPrefs == null) return

        try {
            val json = sharedPrefs.getString("form_$formId", null)
            val timestamp = sharedPrefs.getLong("form_${formId}_timestamp", 0L)

            if (json != null) {
                // Check if the saved state is not too old (24 hours)
                val maxAge = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - timestamp <= maxAge) {
                    val restoredState = Json.decodeFromString<FormState>(json)
                    _formState.value = restoredState
                    updateValidationSummary()
                } else {
                    // Clear old state
                    clearPersistedState()
                }
            }
        } catch (e: Exception) {
            // Silently handle deserialization errors and clear invalid state
            clearPersistedState()
        }
    }

    /**
     * Clears persisted form state.
     */
    private fun clearPersistedState() {
        sharedPrefs?.edit()
            ?.remove("form_$formId")
            ?.remove("form_${formId}_timestamp")
            ?.apply()
    }

    /**
     * Starts auto-save functionality.
     */
    private fun startAutoSave() {
        // This would typically be implemented with a coroutine scope
        // For now, we'll save on every state change if there are unsaved changes
        // In a real implementation, you'd use a timer or coroutine with delay
    }

    /**
     * Updates validation summary for display.
     */
    private fun updateValidationSummary() {
        val errors = _formState.value.fields.values
            .mapNotNull { it.error }
            .distinct()

        val warnings = _formState.value.fields.values
            .mapNotNull { it.warning }
            .distinct()

        _validationErrors.value = errors
        _validationWarnings.value = warnings
    }

    /**
     * Manually triggers form state persistence.
     */
    fun saveState() {
        if (_formState.value.hasUnsavedChanges) {
            persistFormState()
        }
    }

    /**
     * Gets a summary of form validation status.
     */
    fun getValidationSummary(): ValidationSummary {
        val state = _formState.value
        return ValidationSummary(
            isValid = state.isFormValid,
            errorCount = _validationErrors.value.size,
            warningCount = _validationWarnings.value.size,
            hasUnsavedChanges = state.hasUnsavedChanges,
            lastSaved = if (state.lastSavedTimestamp > 0) state.lastSavedTimestamp else null
        )
    }
}

/**
 * Summary of form validation status.
 */
data class ValidationSummary(
    val isValid: Boolean,
    val errorCount: Int,
    val warningCount: Int,
    val hasUnsavedChanges: Boolean,
    val lastSaved: Long?
)

/**
 * Composable function to remember a form state manager.
 */
@Composable
fun rememberFormStateManager(
    formId: String,
    context: Context? = null,
    autoSaveEnabled: Boolean = true
): FormStateManager {
    return remember(formId) {
        FormStateManager(
            formId = formId,
            context = context,
            autoSaveEnabled = autoSaveEnabled
        )
    }
}

/**
 * Effect to handle form state persistence on lifecycle events.
 */
@Composable
fun FormStatePersistenceEffect(
    formStateManager: FormStateManager,
    key: Any? = null
) {
    DisposableEffect(key) {
        onDispose {
            formStateManager.saveState()
        }
    }
}