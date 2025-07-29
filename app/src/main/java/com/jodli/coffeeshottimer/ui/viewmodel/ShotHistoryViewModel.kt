package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.PaginationConfig
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.util.MemoryOptimizer
import com.jodli.coffeeshottimer.domain.usecase.BrewRatioAnalysis
import com.jodli.coffeeshottimer.domain.usecase.ExtractionTimeAnalysis
import com.jodli.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotHistoryUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotStatisticsUseCase
import com.jodli.coffeeshottimer.domain.usecase.GrinderSettingAnalysis
import com.jodli.coffeeshottimer.domain.usecase.OverallStatistics
import com.jodli.coffeeshottimer.domain.usecase.ShotHistoryFilter
import com.jodli.coffeeshottimer.domain.usecase.ShotTrends
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    private val getShotStatisticsUseCase: GetShotStatisticsUseCase,
    private val memoryOptimizer: MemoryOptimizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShotHistoryUiState())
    val uiState: StateFlow<ShotHistoryUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(ShotHistoryFilter())
    val currentFilter: StateFlow<ShotHistoryFilter> = _currentFilter.asStateFlow()

    // Pagination state
    private var currentPaginationConfig = PaginationConfig()
    private var isLoadingMore = false
    private var hasMoreData = true

    companion object {
        private const val COMPONENT_ID = "ShotHistoryViewModel"
    }

    init {
        // Load beans for filtering (this doesn't set loading state)
        loadAvailableBeans()

        // Schedule memory cleanup
        memoryOptimizer.scheduleMemoryCleanup(COMPONENT_ID) {
            performMemoryCleanup()
        }
    }

    private fun loadAvailableBeans() {
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
    }

    private fun loadInitialData() {
        loadAvailableBeans()
        // Start reactive shot history collection
        startReactiveShotHistoryCollection()
    }

    private fun loadShotHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            resetPagination()

            try {
                val filter = _currentFilter.value
                val result = if (filter.hasFilters()) {
                    getShotHistoryUseCase.getFilteredShotsPaginated(
                        beanId = filter.beanId,
                        startDate = filter.startDate,
                        endDate = filter.endDate,
                        paginationConfig = currentPaginationConfig
                    )
                } else {
                    getShotHistoryUseCase.getShotsPaginated(currentPaginationConfig)
                }

                result.fold(
                    onSuccess = { paginatedResult ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            shots = paginatedResult.items,
                            error = null,
                            hasMorePages = paginatedResult.hasNextPage
                        )
                        hasMoreData = paginatedResult.hasNextPage
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load shot history: ${error.message}"
                        )
                    }
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load shot history: ${exception.message}"
                )
            }
        }
    }

    /**
     * Start reactive collection of shot history updates for real-time updates
     */
    private fun startReactiveShotHistoryCollection() {
        viewModelScope.launch {
            try {
                val filter = _currentFilter.value
                val flow = if (filter.hasFilters()) {
                    getShotHistoryUseCase.getFilteredShots(filter)
                } else {
                    getShotHistoryUseCase.getAllShots()
                }

                flow.collect { result ->
                    result.fold(
                        onSuccess = { shots ->
                            // Only update if we're not in a loading state (to avoid conflicts with manual refresh)
                            if (!_uiState.value.isLoading) {
                                // Apply current pagination to the reactive results
                                val currentPageSize = (currentPaginationConfig.page + 1) * currentPaginationConfig.pageSize
                                val paginatedShots = shots.take(currentPageSize)
                                val hasMore = shots.size > paginatedShots.size

                                _uiState.value = _uiState.value.copy(
                                    shots = paginatedShots,
                                    hasMorePages = hasMore,
                                    error = null
                                )
                                hasMoreData = hasMore
                            }
                        },
                        onFailure = { error ->
                            // Only update error if we're not in a loading state
                            if (!_uiState.value.isLoading) {
                                _uiState.value = _uiState.value.copy(
                                    error = "Failed to load shot history: ${error.message}"
                                )
                            }
                        }
                    )
                }
            } catch (exception: Exception) {
                // Only update error if we're not in a loading state
                if (!_uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load shot history: ${exception.message}"
                    )
                }
            }
        }
    }

    fun applyFilter(filter: ShotHistoryFilter) {
        _currentFilter.value = filter
        loadShotHistory()
        // Restart reactive collection with new filter
        startReactiveShotHistoryCollection()
        // Refresh analysis if currently showing to reflect new filter
        if (_uiState.value.showAnalysis) {
            loadAnalysisData()
        }
    }

    fun clearFilters() {
        _currentFilter.value = ShotHistoryFilter()
        loadShotHistory()
        // Restart reactive collection without filters
        startReactiveShotHistoryCollection()
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
        startReactiveShotHistoryCollection()
    }

    fun setBeanFilter(beanId: String?) {
        _currentFilter.value = _currentFilter.value.copy(beanId = beanId)
        loadShotHistory()
        startReactiveShotHistoryCollection()
    }

    fun toggleOptimalExtractionTimeFilter() {
        val current = _currentFilter.value.onlyOptimalExtractionTime
        _currentFilter.value = _currentFilter.value.copy(
            onlyOptimalExtractionTime = if (current == true) null else true
        )
        loadShotHistory()
        startReactiveShotHistoryCollection()
    }

    fun toggleTypicalBrewRatioFilter() {
        val current = _currentFilter.value.onlyTypicalBrewRatio
        _currentFilter.value = _currentFilter.value.copy(
            onlyTypicalBrewRatio = if (current == true) null else true
        )
        loadShotHistory()
        startReactiveShotHistoryCollection()
    }

    fun refreshData() {
        loadShotHistory()
        startReactiveShotHistoryCollection()
    }

    /**
     * Refresh data when screen resumes - optimized for reactive updates
     */
    fun refreshOnResume() {
        // Start reactive collection if not already started
        if (_uiState.value.shots.isEmpty() && _uiState.value.error == null && !_uiState.value.isLoading) {
            startReactiveShotHistoryCollection()
        }
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
                val trendsResult =
                    getShotStatisticsUseCase.getShotTrends(beanId = currentBeanId, days = 30)
                val trends = trendsResult.getOrNull()

                // Load brew ratio analysis (filtered by bean if selected)
                val brewRatioResult =
                    getShotStatisticsUseCase.getBrewRatioAnalysis(beanId = currentBeanId)
                val brewRatioAnalysis = brewRatioResult.getOrNull()

                // Load extraction time analysis (filtered by bean if selected)
                val extractionTimeResult =
                    getShotStatisticsUseCase.getExtractionTimeAnalysis(beanId = currentBeanId)
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

    fun loadMore() {
        if (isLoadingMore || !hasMoreData) return

        viewModelScope.launch {
            isLoadingMore = true
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                // Increment page for next request
                currentPaginationConfig = currentPaginationConfig.copy(
                    page = currentPaginationConfig.page + 1
                )

                val filter = _currentFilter.value
                val result = if (filter.hasFilters()) {
                    getShotHistoryUseCase.getFilteredShotsPaginated(
                        beanId = filter.beanId,
                        startDate = filter.startDate,
                        endDate = filter.endDate,
                        paginationConfig = currentPaginationConfig
                    )
                } else {
                    getShotHistoryUseCase.getShotsPaginated(currentPaginationConfig)
                }

                result.fold(
                    onSuccess = { paginatedResult ->
                        val currentShots = _uiState.value.shots
                        val newShots = currentShots + paginatedResult.items

                        _uiState.value = _uiState.value.copy(
                            shots = newShots,
                            hasMorePages = paginatedResult.hasNextPage,
                            isLoadingMore = false,
                            error = null
                        )
                        hasMoreData = paginatedResult.hasNextPage
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            error = "Failed to load more shots: ${error.message}"
                        )
                        // Revert page increment on failure
                        currentPaginationConfig = currentPaginationConfig.copy(
                            page = currentPaginationConfig.page - 1
                        )
                    }
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = "Failed to load more shots: ${exception.message}"
                )
                // Revert page increment on failure
                currentPaginationConfig = currentPaginationConfig.copy(
                    page = currentPaginationConfig.page - 1
                )
            } finally {
                isLoadingMore = false
            }
        }
    }

    // PERFORMANCE OPTIMIZATION METHODS

    /**
     * Reset pagination state when filters change.
     */
    private fun resetPagination() {
        currentPaginationConfig = PaginationConfig()
        hasMoreData = true
        isLoadingMore = false
    }

    /**
     * Perform memory cleanup to optimize performance.
     */
    private suspend fun performMemoryCleanup() {
        // Clear analysis data if not currently showing
        if (!_uiState.value.showAnalysis) {
            _uiState.value = _uiState.value.copy(
                overallStatistics = null,
                shotTrends = null,
                brewRatioAnalysis = null,
                extractionTimeAnalysis = null,
                grinderSettingAnalysis = null
            )
        }

        // Limit shot history size if too large
        val currentShots = _uiState.value.shots
        if (currentShots.size > 200) {
            val trimmedShots = currentShots.take(100) // Keep only first 100 shots
            _uiState.value = _uiState.value.copy(shots = trimmedShots)

            // Reset pagination to allow loading more
            resetPagination()
            hasMoreData = true
        }

        // Perform general memory cleanup
        memoryOptimizer.performMemoryCleanup()
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel memory cleanup when ViewModel is destroyed
        memoryOptimizer.cancelMemoryCleanup(COMPONENT_ID)
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
    val grinderSettingAnalysis: GrinderSettingAnalysis? = null,
    // Pagination state
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true
) {
    val isEmpty: Boolean
        get() = shots.isEmpty() && !isLoading

    val hasAnalysisData: Boolean
        get() = overallStatistics != null || shotTrends != null || brewRatioAnalysis != null
}