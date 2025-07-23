package com.jodli.coffeeshottimer.ui.validation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
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
    context: Context? = null
) {
    private val _formState = MutableStateFlow(FormState(formId = formId))

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())

    private val _validationWarnings = MutableStateFlow<List<String>>(emptyList())

    private val sharedPrefs: SharedPreferences? =
        context?.getSharedPreferences("form_states", Context.MODE_PRIVATE)

    init {
        restoreFormState()
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

}

