package com.example.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.data.repository.BeanRepository
import com.example.coffeeshottimer.data.repository.ShotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for shot recording screen.
 * Demonstrates Hilt dependency injection with repositories.
 */
@HiltViewModel
class ShotRecordingViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val beanRepository: BeanRepository
) : ViewModel() {
    
    private val _activeBeans = MutableStateFlow<List<Bean>>(emptyList())
    val activeBeans: StateFlow<List<Bean>> = _activeBeans.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadActiveBeans()
    }
    
    /**
     * Load active beans for selection.
     */
    private fun loadActiveBeans() {
        viewModelScope.launch {
            _isLoading.value = true
            beanRepository.getActiveBeans().collect { result ->
                result.fold(
                    onSuccess = { beans ->
                        _activeBeans.value = beans
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Record a new shot.
     */
    fun recordShot(shot: Shot) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = shotRepository.recordShot(shot)
            result.fold(
                onSuccess = {
                    _errorMessage.value = null
                    // Shot recorded successfully
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )
            _isLoading.value = false
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}