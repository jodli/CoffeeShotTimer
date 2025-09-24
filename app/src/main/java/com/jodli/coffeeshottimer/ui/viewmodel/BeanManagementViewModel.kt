package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetBeanHistoryUseCase
import com.jodli.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing coffee bean profiles.
 * Handles bean listing, filtering, and management operations.
 */
@HiltViewModel
class BeanManagementViewModel @Inject constructor(
    private val getActiveBeansUseCase: GetActiveBeansUseCase,
    private val getBeanHistoryUseCase: GetBeanHistoryUseCase,
    private val updateBeanUseCase: UpdateBeanUseCase,
    private val beanRepository: BeanRepository,
    private val domainErrorTranslator: DomainErrorTranslator
) : ViewModel() {

    private val _uiState = MutableStateFlow(BeanManagementUiState())
    val uiState: StateFlow<BeanManagementUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showInactive = MutableStateFlow(false)
    val showInactive: StateFlow<Boolean> = _showInactive.asStateFlow()

    init {
        loadBeans()
        observeSearchAndFilter()
    }

    private fun observeSearchAndFilter() {
        combine(
            _searchQuery,
            _showInactive
        ) { query, showInactive ->
            Pair(query, showInactive)
        }.onEach { (query, showInactive) ->
            loadFilteredBeans(query, showInactive)
        }.launchIn(viewModelScope)
    }

    private fun loadBeans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            getActiveBeansUseCase.execute()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = domainErrorTranslator.translateError(exception)
                    )
                }
                .collect { result ->
                    if (result.isSuccess) {
                        val beans = result.getOrNull() ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            beans = beans,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = domainErrorTranslator.translateResultError(result)
                        )
                    }
                }
        }
    }

    private fun loadFilteredBeans(query: String, showInactive: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val useCase = if (showInactive) {
                getBeanHistoryUseCase.getBeanHistoryWithSearch(query, activeOnly = false)
            } else {
                if (query.isBlank()) {
                    getActiveBeansUseCase.execute()
                } else {
                    getActiveBeansUseCase.getActiveBeansWithSearch(query)
                }
            }

            useCase
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = domainErrorTranslator.translateError(exception)
                    )
                }
                .collect { result ->
                    if (result.isSuccess) {
                        val beans = result.getOrNull() ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            beans = beans,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = domainErrorTranslator.translateResultError(result)
                        )
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowInactive() {
        _showInactive.value = !_showInactive.value
    }

    fun deleteBean(beanId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = updateBeanUseCase.updateActiveStatus(beanId, false)
            if (result.isSuccess) {
                // Refresh the bean list
                loadFilteredBeans(_searchQuery.value, _showInactive.value)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = domainErrorTranslator.translateResultError(result)
                )
            }
        }
    }

    fun reactivateBean(beanId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = updateBeanUseCase.updateActiveStatus(beanId, true)
            if (result.isSuccess) {
                // Refresh the bean list
                loadFilteredBeans(_searchQuery.value, _showInactive.value)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = domainErrorTranslator.translateResultError(result)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadFilteredBeans(_searchQuery.value, _showInactive.value)
    }

    /**
     * Set a bean as the current active bean for shot recording.
     * Implements requirement 3.2 for connecting bean selection between screens.
     * Updates the grinder setting memory for the bean.
     */
    fun setCurrentBean(beanId: String, grinderSetting: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Update grinder setting if provided
            val grinderResult = if (!grinderSetting.isNullOrBlank()) {
                updateBeanUseCase.updateGrinderSetting(beanId, grinderSetting)
            } else {
                Result.success(Unit)
            }

            if (grinderResult.isSuccess) {
                // Set the bean as current in the repository
                val result = beanRepository.setCurrentBean(beanId)

                if (result.isSuccess) {
                    // Update UI state to reflect the current bean selection
                    _uiState.value = _uiState.value.copy(
                        currentBeanId = beanId,
                        isLoading = false
                    )
                    // Refresh the bean list to show updated grinder setting
                    loadFilteredBeans(_searchQuery.value, _showInactive.value)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = domainErrorTranslator.translateResultError(result)
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = domainErrorTranslator.translateResultError(grinderResult)
                )
            }
        }
    }
}

/**
 * UI state for the bean management screen.
 */
data class BeanManagementUiState(
    val beans: List<Bean> = emptyList(),
    val beansWithFreshness: List<com.jodli.coffeeshottimer.domain.usecase.BeanWithFreshness> = emptyList(),
    val currentBeanId: String? = null,
    val hasActiveBeans: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)
