package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.BasketConfiguration
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.repository.BasketConfigRepository
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val grinderConfigRepository: GrinderConfigRepository,
    private val basketConfigRepository: BasketConfigRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MoreUiState())
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    /** Re-read on every visit so values saved in the settings screens show on return. */
    fun load() {
        viewModelScope.launch {
            _uiState.value = MoreUiState(
                grinder = grinderConfigRepository.getCurrentConfig().getOrNull(),
                basket = basketConfigRepository.getActiveConfig().getOrNull()
            )
        }
    }
}

data class MoreUiState(
    val grinder: GrinderConfiguration? = null,
    val basket: BasketConfiguration? = null
)
