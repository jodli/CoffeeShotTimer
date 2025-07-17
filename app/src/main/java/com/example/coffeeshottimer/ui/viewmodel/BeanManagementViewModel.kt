package com.example.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.domain.usecase.AddBeanUseCase
import com.example.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.example.coffeeshottimer.domain.usecase.GetBeanHistoryUseCase
import com.example.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    private val addBeanUseCase: AddBeanUseCase,
    private val updateBeanUseCase: UpdateBeanUseCase
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
                        error = "Failed to load beans: ${exception.message}"
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
                            error = result.exceptionOrNull()?.message ?: "Failed to load beans"
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
                getActiveBeansUseCase.getActiveBeansWithSearch(query)
            }
            
            useCase
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load beans: ${exception.message}"
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
                            error = result.exceptionOrNull()?.message ?: "Failed to load beans"
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
                    error = result.exceptionOrNull()?.message ?: "Failed to delete bean"
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
                    error = result.exceptionOrNull()?.message ?: "Failed to reactivate bean"
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
}

/**
 * UI state for the bean management screen.
 */
data class BeanManagementUiState(
    val beans: List<Bean> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)