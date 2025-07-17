package com.example.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.Shot
import com.example.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.example.coffeeshottimer.domain.usecase.GetShotHistoryUseCase
import com.example.coffeeshottimer.domain.usecase.GetShotStatisticsUseCase
import com.example.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.example.coffeeshottimer.domain.usecase.OverallStatistics
import com.example.coffeeshottimer.domain.usecase.ShotTrends
import com.example.coffeeshottimer.domain.usecase.BrewRatioAnalysis
import com.example.coffeeshottimer.domain.usecase.ExtractionTimeAnalysis
import com.example.coffeeshottimer.domain.usecase.GrinderSettingAnalysis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel for the Shot History screen.
 * Manages shot history data, filtering, and UI state.
 */
@HiltViewModel
class ShotHistoryViewModel @Inject constructor(
    private val getShotHistoryUseCase: GetShotHistoryUseCase,
    private val getActiveBeansUseCase: GetActiveBeansUseCase,
    private val getShotStatisticsUseCase: GetShotStatisticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShotHistoryUiState())
    val uiState: StateFlow<ShotHistoryUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(ShotHistoryFilter())
    val currentFilter: StateFlow<ShotHistoryFilter> = _currentFilter.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load beans for filtering
            getActiveBeansUseCase.execute()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load beans: ${error.message}"
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { beans ->
                            _uiState.value = _uiState.value.copy(availableBeans = beans)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load beans: ${error.message}"
                            )
                        }
                    )
                }
        }

        // Load initial shot history
        loadShotHistory()
    }

    private fun loadShotHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val filter = _currentFilter.value
            val shotsFlow = if (filter.hasFilters()) {
                getShotHistoryUseCase.getFilteredShots(filter)
            } else {
                getShotHistoryUseCase.getAllShots()
            }

            shotsFlow
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load shot history: ${error.message}"
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { shots ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                shots = shots,
                                error = null
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load shot history: ${error.message}"
                            )
                        }
                    )
                }
        }
    }

    fun applyFilter(filter: ShotHistoryFilter) {
        _currentFilter.value = filter
        loadShotHistory()
        // Refresh analysis if currently showing to reflect new filter
        if (_uiState.value.showAnalysis) {
            loadAnalysisData()
        }
    }

    fun clearFilters() {
        _currentFilter.value = ShotHistoryFilter()
        loadShotHistory()
        // Refresh analysis if currently showing to reflect cleared filters
        if (_uiState.value.showAnalysis) {
            loadAnalysisData()
        }
    }

    fun setDateRangeFilter(startDate: LocalDate?, endDate: LocalDate?) {
        val startDateTime = startDate?.atStartOfDay()
        val endDateTime = endDate?.atTime(LocalTime.MAX)
        
        _currentFilter.value = _currentFilter.value.copy(
            startDate = startDateTime,
            endDate = endDateTime
        )
        loadShotHistory()
    }

    fun setBeanFilter(beanId: String?) {
        _currentFilter.value = _currentFilter.value.copy(beanId = beanId)
        loadShotHistory()
    }

    fun setGrinderSettingFilter(grinderSetting: String?) {
        _currentFilter.value = _currentFilter.value.copy(
            grinderSetting = if (grinderSetting.isNullOrBlank()) null else grinderSetting
        )
        loadShotHistory()
    }

    fun setBrewRatioFilter(minRatio: Double?, maxRatio: Double?) {
        _currentFilter.value = _currentFilter.value.copy(
            minBrewRatio = minRatio,
            maxBrewRatio = maxRatio
        )
        loadShotHistory()
    }

    fun setExtractionTimeFilter(minTime: Int?, maxTime: Int?) {
        _currentFilter.value = _currentFilter.value.copy(
            minExtractionTime = minTime,
            maxExtractionTime = maxTime
        )
        loadShotHistory()
    }

    fun toggleOptimalExtractionTimeFilter() {
        val current = _currentFilter.value.onlyOptimalExtractionTime
        _currentFilter.value = _currentFilter.value.copy(
            onlyOptimalExtractionTime = if (current == true) null else true
        )
        loadShotHistory()
    }

    fun toggleTypicalBrewRatioFilter() {
        val current = _currentFilter.value.onlyTypicalBrewRatio
        _currentFilter.value = _currentFilter.value.copy(
            onlyTypicalBrewRatio = if (current == true) null else true
        )
        loadShotHistory()
    }

    fun refreshData() {
        loadInitialData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getBeanName(beanId: String): String {
        return _uiState.value.availableBeans.find { it.id == beanId }?.name ?: "Unknown Bean"
    }

    fun toggleAnalysisView() {
        val currentShowAnalysis = _uiState.value.showAnalysis
        _uiState.value = _uiState.value.copy(showAnalysis = !currentShowAnalysis)
        
        if (!currentShowAnalysis && !_uiState.value.hasAnalysisData) {
            loadAnalysisData()
        }
    }

    private fun loadAnalysisData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(analysisLoading = true)
            
            try {
                val currentBeanId = _currentFilter.value.beanId
                
                // Load overall statistics (always global)
                val overallStatsResult = getShotStatisticsUseCase.getOverallStatistics()
                val overallStats = overallStatsResult.getOrNull()
                
                // Load shot trends (filtered by bean if selected, last 30 days)
                val trendsResult = getShotStatisticsUseCase.getShotTrends(beanId = currentBeanId, days = 30)
                val trends = trendsResult.getOrNull()
                
                // Load brew ratio analysis (filtered by bean if selected)
                val brewRatioResult = getShotStatisticsUseCase.getBrewRatioAnalysis(beanId = currentBeanId)
                val brewRatioAnalysis = brewRatioResult.getOrNull()
                
                // Load extraction time analysis (filtered by bean if selected)
                val extractionTimeResult = getShotStatisticsUseCase.getExtractionTimeAnalysis(beanId = currentBeanId)
                val extractionTimeAnalysis = extractionTimeResult.getOrNull()
                
                // Load grinder setting analysis (only for specific bean)
                val grinderAnalysisResult = if (currentBeanId != null) {
                    getShotStatisticsUseCase.getGrinderSettingAnalysis(currentBeanId)
                } else {
                    Result.success(null)
                }
                val grinderAnalysis = grinderAnalysisResult.getOrNull()
                
                _uiState.value = _uiState.value.copy(
                    analysisLoading = false,
                    overallStatistics = overallStats,
                    shotTrends = trends,
                    brewRatioAnalysis = brewRatioAnalysis,
                    extractionTimeAnalysis = extractionTimeAnalysis,
                    grinderSettingAnalysis = grinderAnalysis
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    analysisLoading = false,
                    error = "Failed to load analysis: ${exception.message}"
                )
            }
        }
    }

    fun refreshAnalysis() {
        if (_uiState.value.showAnalysis) {
            loadAnalysisData()
        }
    }
}

/**
 * UI state for the Shot History screen
 */
data class ShotHistoryUiState(
    val isLoading: Boolean = false,
    val shots: List<Shot> = emptyList(),
    val availableBeans: List<Bean> = emptyList(),
    val error: String? = null,
    val showAnalysis: Boolean = false,
    val analysisLoading: Boolean = false,
    val overallStatistics: OverallStatistics? = null,
    val shotTrends: ShotTrends? = null,
    val brewRatioAnalysis: BrewRatioAnalysis? = null,
    val extractionTimeAnalysis: ExtractionTimeAnalysis? = null,
    val grinderSettingAnalysis: GrinderSettingAnalysis? = null
) {
    val isEmpty: Boolean
        get() = shots.isEmpty() && !isLoading
        
    val hasAnalysisData: Boolean
        get() = overallStatistics != null || shotTrends != null || brewRatioAnalysis != null
}