package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.R
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
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.StringResourceProvider
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
    private val getShotQualityAnalysisUseCase: com.jodli.coffeeshottimer.domain.usecase.GetShotQualityAnalysisUseCase,
    private val memoryOptimizer: MemoryOptimizer,
    private val stringResourceProvider: StringResourceProvider,
    private val domainErrorTranslator: DomainErrorTranslator
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

        // Coaching insights thresholds
        private const val MIN_SHOTS_FOR_TREND = 3
        private const val MAX_RECENT_SHOTS = 5
        private const val MIN_SHOTS_FOR_DIAL_IN = 3
        private const val MIN_SHOTS_FOR_GRIND_ANALYSIS = 5
        private const val DIAL_IN_CONSISTENCY_THRESHOLD = 70
        private const val DIAL_IN_MIN_SCORE = 60

        // Quality score constants
        private const val SCORE_PERFECT_THRESHOLD = 80
        private const val SCORE_GOOD_THRESHOLD = 60
        private const val SCORE_OPTIMAL_TIME_POINTS = 40
        private const val SCORE_TYPICAL_RATIO_POINTS = 40
        private const val SCORE_REASONABLE_WEIGHTS_POINTS = 20
        private const val SCORE_TIME_PENALTY_PER_SECOND = 4
        private const val SCORE_RATIO_PENALTY_MULTIPLIER = 20.0

        // Shot quality ranges
        private const val MIN_REASONABLE_WEIGHT_IN = 15.0
        private const val MAX_REASONABLE_WEIGHT_IN = 25.0
        private const val MIN_REASONABLE_WEIGHT_OUT = 25.0
        private const val MAX_REASONABLE_WEIGHT_OUT = 60.0

        // Extraction time boundaries
        private const val MIN_OPTIMAL_EXTRACTION_TIME = 25
        private const val MAX_OPTIMAL_EXTRACTION_TIME = 30

        // Brew ratio boundaries
        private const val MIN_TYPICAL_BREW_RATIO = 1.5
        private const val MAX_TYPICAL_BREW_RATIO = 3.0

        // Consistency milestone thresholds
        private const val CONSISTENCY_MILESTONE_TEN = 10
        private const val CONSISTENCY_MILESTONE_INTERVAL = 5
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
                        error = domainErrorTranslator.translateError(error)
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { beans ->
                            _uiState.value = _uiState.value.copy(availableBeans = beans)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                error = domainErrorTranslator.translateError(error)
                            )
                        }
                    )
                }
        }
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
                        val insights = calculateCoachingInsights(paginatedResult.items, _currentFilter.value.beanId)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            shots = paginatedResult.items,
                            error = null,
                            hasMorePages = paginatedResult.hasNextPage,
                            coachingInsights = insights
                        )
                        hasMoreData = paginatedResult.hasNextPage
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = domainErrorTranslator.translateError(error)
                        )
                    }
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = domainErrorTranslator.translateError(exception)
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

                                // Calculate coaching insights for reactive updates
                                val insights = calculateCoachingInsights(paginatedShots, _currentFilter.value.beanId)

                                _uiState.value = _uiState.value.copy(
                                    shots = paginatedShots,
                                    hasMorePages = hasMore,
                                    error = null,
                                    coachingInsights = insights
                                )
                                hasMoreData = hasMore
                            }
                        },
                        onFailure = { error ->
                            // Only update error if we're not in a loading state
                            if (!_uiState.value.isLoading) {
                                _uiState.value = _uiState.value.copy(
                                    error = domainErrorTranslator.translateError(error)
                                )
                            }
                        }
                    )
                }
            } catch (exception: Exception) {
                // Only update error if we're not in a loading state
                if (!_uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(
                        error = stringResourceProvider.getString(R.string.error_failed_to_load_shot_history, exception.message ?: "")
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
            loadShotHistory()
            startReactiveShotHistoryCollection()
        }
    }

    fun getBeanName(beanId: String): String {
        // If the bean isn't in availableBeans (which lists active beans), it may be inactive.
        // Show a friendly placeholder instead of a generic error.
        return _uiState.value.availableBeans.find { it.id == beanId }?.name
            ?: stringResourceProvider.getString(R.string.text_inactive_bean)
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

                // Load aggregate quality analysis (Epic 5)
                loadAggregateQualityAnalysis()

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
                    error = stringResourceProvider.getString(R.string.error_failed_to_load_analysis, exception.message ?: "")
                )
            }
        }
    }

    fun refreshAnalysis() {
        if (_uiState.value.showAnalysis) {
            loadAnalysisData()
        }
    }

    /**
     * Load aggregate quality analysis for the current shots.
     * Calculates quality metrics, trends, and distribution for visual dashboard.
     */
    private suspend fun loadAggregateQualityAnalysis() {
        val shots = _uiState.value.shots
        if (shots.isEmpty()) {
            _uiState.value = _uiState.value.copy(aggregateQualityAnalysis = null)
            return
        }

        // Use current shots as context for quality calculation
        val aggregateAnalysis = getShotQualityAnalysisUseCase.calculateAggregateQualityAnalysis(
            shots = shots,
            allShots = shots
        )

        _uiState.value = _uiState.value.copy(aggregateQualityAnalysis = aggregateAnalysis)
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
                            error = domainErrorTranslator.translateError(error)
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
                    error = stringResourceProvider.getString(R.string.error_failed_to_load_more_shots, exception.message ?: "")
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
                grinderSettingAnalysis = null,
                aggregateQualityAnalysis = null
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

    // COACHING INSIGHTS CALCULATION METHODS

    /**
     * Calculate coaching insights for the current view of shots.
     * Insights are bean-specific when a bean filter is active.
     *
     * @param shots List of shots to analyze
     * @param beanId Optional bean ID for bean-specific analysis
     * @return CoachingInsights or null if insufficient data
     */
    private fun calculateCoachingInsights(shots: List<Shot>, beanId: String?): CoachingInsights? {
        if (shots.isEmpty()) return null

        val beanName = beanId?.let { getBeanName(it) }
        val recentTrend = calculateRecentTrend(shots, beanId)
        val dialInStatus = getDialInStatus(shots, beanId)
        val grindCoaching = analyzeGrindCoachingEffectiveness(shots, beanId)

        // Only return insights if we have at least one piece of useful information
        return if (recentTrend != null || dialInStatus != null || grindCoaching != null) {
            CoachingInsights(
                beanId = beanId,
                beanName = beanName,
                recentTrend = recentTrend,
                dialInStatus = dialInStatus,
                grindCoachingEffectiveness = grindCoaching
            )
        } else {
            null
        }
    }

    /**
     * Analyze the last 3-5 shots for recent brewing patterns.
     *
     * @param shots List of shots (assumed sorted by timestamp descending)
     * @param beanId Optional bean ID for filtering
     * @return RecentTrend or null if insufficient data
     */
    private fun calculateRecentTrend(shots: List<Shot>, beanId: String?): RecentTrend? {
        val recentShots = shots.filter { beanId == null || it.beanId == beanId }.take(MAX_RECENT_SHOTS)
        if (recentShots.size < MIN_SHOTS_FOR_TREND) return null

        var perfectCount = 0
        var goodCount = 0
        var totalQuality = 0

        recentShots.forEach { shot ->
            val quality = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, shots)
            totalQuality += quality

            when {
                quality >= SCORE_PERFECT_THRESHOLD -> perfectCount++
                quality >= SCORE_GOOD_THRESHOLD -> goodCount++
            }
        }

        val avgQuality = totalQuality / recentShots.size
        val message = buildConsistencyMessage(recentShots.size, perfectCount, goodCount, beanId)

        return RecentTrend(
            shotCount = recentShots.size,
            perfectShotCount = perfectCount,
            goodShotCount = goodCount,
            averageQualityScore = avgQuality,
            consistencyMessage = message
        )
    }

    /**
     * Build a human-readable consistency message for recent shots.
     */
    private fun buildConsistencyMessage(
        shotCount: Int,
        perfectCount: Int,
        goodCount: Int,
        beanId: String?
    ): String {
        val beanName = beanId?.let { getBeanName(it) } ?: ""

        return when {
            perfectCount >= 2 -> {
                if (beanId != null) {
                    stringResourceProvider.getString(
                        R.string.coaching_recent_trend_perfect_with_bean,
                        shotCount,
                        beanName,
                        perfectCount
                    )
                } else {
                    stringResourceProvider.getString(R.string.coaching_recent_trend_perfect, shotCount, perfectCount)
                }
            }
            perfectCount + goodCount >= shotCount - 1 -> {
                if (beanId != null) {
                    stringResourceProvider.getString(
                        R.string.coaching_recent_trend_consistent_with_bean,
                        beanName,
                        perfectCount,
                        goodCount
                    )
                } else {
                    stringResourceProvider.getString(R.string.coaching_recent_trend_consistent, perfectCount, goodCount)
                }
            }
            perfectCount + goodCount >= shotCount / 2 -> {
                if (beanId != null) {
                    stringResourceProvider.getString(R.string.coaching_recent_trend_progress_with_bean, beanName)
                } else {
                    stringResourceProvider.getString(R.string.coaching_recent_trend_progress)
                }
            }
            else -> {
                if (beanId != null) {
                    stringResourceProvider.getString(R.string.coaching_recent_trend_exploring_with_bean, beanName)
                } else {
                    stringResourceProvider.getString(R.string.coaching_recent_trend_exploring)
                }
            }
        }
    }

    /**
     * Determine dial-in status for a specific bean.
     *
     * @param shots List of all shots
     * @param beanId Bean ID to analyze (required)
     * @return DialInStatus or null if not enough data or beanId is null
     */
    private fun getDialInStatus(shots: List<Shot>, beanId: String?): DialInStatus? {
        if (beanId == null) return null // Dial-in is bean-specific

        val beanShots = shots.filter { it.beanId == beanId }.sortedBy { it.timestamp }
        if (beanShots.size < MIN_SHOTS_FOR_DIAL_IN) return null

        // Check last 3 shots for consistency
        val recentShots = beanShots.takeLast(MIN_SHOTS_FOR_DIAL_IN)
        val recentQualityScores = recentShots.map {
            getShotQualityAnalysisUseCase.calculateShotQualityScore(it, beanShots)
        }
        val avgRecentQuality = recentQualityScores.average().toInt()

        val isDialedIn = avgRecentQuality >= DIAL_IN_CONSISTENCY_THRESHOLD &&
            recentQualityScores.all { it >= DIAL_IN_MIN_SCORE }

        // If dialed in, find when they achieved it
        val shotsToDialIn = if (isDialedIn) {
            findDialInPoint(beanShots)
        } else {
            null
        }

        val beanName = getBeanName(beanId)
        val message = if (isDialedIn) {
            stringResourceProvider.getString(
                R.string.coaching_dial_in_achieved,
                beanName,
                shotsToDialIn ?: beanShots.size
            )
        } else {
            stringResourceProvider.getString(
                R.string.coaching_dial_in_exploring,
                beanName,
                beanShots.size
            )
        }

        return DialInStatus(
            isDialedIn = isDialedIn,
            shotCount = beanShots.size,
            shotsToDialIn = shotsToDialIn,
            statusMessage = message
        )
    }

    /**
     * Find at which shot number the bean was dialed in.
     * Returns the index of the first shot in a consistent good streak.
     */
    private fun findDialInPoint(beanShots: List<Shot>): Int? {
        if (beanShots.size < MIN_SHOTS_FOR_DIAL_IN) return null

        // Look for first occurrence of 3 consecutive good shots
        for (i in 0..beanShots.size - MIN_SHOTS_FOR_DIAL_IN) {
            val threeShots = beanShots.subList(i, i + MIN_SHOTS_FOR_DIAL_IN)
            val allGood = threeShots.all {
                getShotQualityAnalysisUseCase.calculateShotQualityScore(it, beanShots) >= DIAL_IN_MIN_SCORE
            }
            if (allGood) {
                return i + MIN_SHOTS_FOR_DIAL_IN // Return the count when they achieved it
            }
        }

        return null
    }

    /**
     * Analyze how effective grind coaching has been.
     * Note: Currently a placeholder as we need to track recommendation follow-through.
     *
     * @param shots List of shots
     * @param beanId Optional bean ID for filtering
     * @return GrindCoachingEffectiveness or null if no grind coaching data
     */
    @Suppress("UnusedParameter")
    private fun analyzeGrindCoachingEffectiveness(
        shots: List<Shot>,
        beanId: String?
    ): GrindCoachingEffectiveness? {
        // Placeholder: Will be implemented when recommendation tracking is added to Shot model
        return null
    }

    // ACHIEVEMENT DETECTION METHODS

    /**
     * Get the achievement for a specific shot, if any.
     * Checks for bean-specific milestones.
     *
     * @param shot The shot to check for achievements
     * @return Achievement or null if no achievement
     */
    fun getAchievementForShot(shot: Shot): Achievement? {
        val allShots = _uiState.value.shots
        val beanName = getBeanName(shot.beanId)

        return when {
            isFirstPerfectForBean(shot, allShots) -> Achievement(
                type = AchievementType.FIRST_PERFECT,
                label = stringResourceProvider.getString(R.string.achievement_first_perfect, beanName),
                emoji = "🎉",
                beanId = shot.beanId,
                beanName = beanName
            )
            isDialInMilestone(shot, allShots) -> Achievement(
                type = AchievementType.DIALED_IN,
                label = stringResourceProvider.getString(R.string.achievement_dialed_in, beanName),
                emoji = "🎯",
                beanId = shot.beanId,
                beanName = beanName
            )
            else -> {
                val consistencyStreak = isBeanConsistencyMilestone(shot, allShots)
                consistencyStreak?.let {
                    Achievement(
                        type = AchievementType.CONSISTENCY,
                        label = stringResourceProvider.getString(
                            R.string.achievement_consistency_streak,
                            it,
                            beanName
                        ),
                        emoji = "🔥",
                        beanId = shot.beanId,
                        beanName = beanName
                    )
                }
            }
        }
    }

    /**
     * Check if this is the first perfect shot with this bean.
     */
    private fun isFirstPerfectForBean(shot: Shot, allShots: List<Shot>): Boolean {
        val quality = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, allShots)
        if (quality < SCORE_PERFECT_THRESHOLD) return false

        // Get all shots for this bean, sorted by timestamp
        val beanShots = allShots
            .filter { it.beanId == shot.beanId }
            .sortedBy { it.timestamp }

        // Find all perfect shots before this one
        val perfectShotsBeforeThis = beanShots
            .filter { it.timestamp < shot.timestamp }
            .filter { getShotQualityAnalysisUseCase.calculateShotQualityScore(it, allShots) >= SCORE_PERFECT_THRESHOLD }

        return perfectShotsBeforeThis.isEmpty()
    }

    /**
     * Check if this shot represents the moment the bean was dialed in.
     * True if this is the 3rd consecutive good shot with this bean.
     */
    private fun isDialInMilestone(shot: Shot, allShots: List<Shot>): Boolean {
        // Get all shots for this bean, sorted by timestamp
        val beanShots = allShots
            .filter { it.beanId == shot.beanId }
            .sortedBy { it.timestamp }

        // Find the index of this shot
        val shotIndex = beanShots.indexOfFirst { it.id == shot.id }
        if (shotIndex < 2) return false // Need at least 3 shots total

        // Check if this is the 3rd consecutive good shot
        val lastThreeShots = beanShots.subList(maxOf(0, shotIndex - 2), shotIndex + 1)
        if (lastThreeShots.size != MIN_SHOTS_FOR_DIAL_IN) return false

        val allGood = lastThreeShots.all {
            getShotQualityAnalysisUseCase.calculateShotQualityScore(it, allShots) >= DIAL_IN_MIN_SCORE
        }
        if (!allGood) return false

        // Make sure there weren't 3 good shots in a row before this
        if (shotIndex >= MIN_SHOTS_FOR_DIAL_IN) {
            val previousThree = beanShots.subList(shotIndex - MIN_SHOTS_FOR_DIAL_IN, shotIndex)
            val previouslyDialedIn = previousThree.all {
                getShotQualityAnalysisUseCase.calculateShotQualityScore(it, allShots) >= DIAL_IN_MIN_SCORE
            }
            if (previouslyDialedIn) return false
        }

        return true
    }

    /**
     * Check if this shot represents a consistency milestone.
     * Returns the streak count if this is part of a notable consistency streak (3+ good shots).
     */
    private fun isBeanConsistencyMilestone(shot: Shot, allShots: List<Shot>): Int? {
        val quality = getShotQualityAnalysisUseCase.calculateShotQualityScore(shot, allShots)
        if (quality < DIAL_IN_MIN_SCORE) return null

        // Get all shots for this bean, sorted by timestamp
        val beanShots = allShots
            .filter { it.beanId == shot.beanId }
            .sortedBy { it.timestamp }

        // Find the index of this shot
        val shotIndex = beanShots.indexOfFirst { it.id == shot.id }
        if (shotIndex < 0) return null

        // Count consecutive good shots ending with this one
        val streakCount = countConsecutiveGoodShots(beanShots, shotIndex)

        // Only show achievement for streaks of 3, 5, or 10+
        return when {
            streakCount == MIN_SHOTS_FOR_DIAL_IN -> MIN_SHOTS_FOR_DIAL_IN
            streakCount == MAX_RECENT_SHOTS -> MAX_RECENT_SHOTS
            streakCount >= CONSISTENCY_MILESTONE_TEN &&
                streakCount % CONSISTENCY_MILESTONE_INTERVAL == 0 -> streakCount
            else -> null
        }
    }

    private fun countConsecutiveGoodShots(beanShots: List<Shot>, fromIndex: Int): Int {
        var count = 0
        for (i in fromIndex downTo 0) {
            if (getShotQualityAnalysisUseCase.calculateShotQualityScore(beanShots[i], beanShots) >= DIAL_IN_MIN_SCORE) {
                count++
            } else {
                break
            }
        }
        return count
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
    val hasMorePages: Boolean = true,
    // Coaching insights
    val coachingInsights: CoachingInsights? = null,
    // Aggregate quality analysis (Epic 5)
    val aggregateQualityAnalysis: com.jodli.coffeeshottimer.domain.usecase.AggregateQualityAnalysis? = null
) {
    val isEmpty: Boolean
        get() = shots.isEmpty() && !isLoading

    val hasAnalysisData: Boolean
        get() = overallStatistics != null || shotTrends != null || brewRatioAnalysis != null
}

/**
 * Coaching insights for the current bean or filtered view.
 * Provides retrospective analysis to help users understand their brewing journey.
 */
data class CoachingInsights(
    val beanId: String?, // null if showing all beans
    val beanName: String?, // null if showing all beans
    val recentTrend: RecentTrend?,
    val dialInStatus: DialInStatus?,
    val grindCoachingEffectiveness: GrindCoachingEffectiveness?
)

/**
 * Recent trend analysis for the last 3-5 shots with a specific bean.
 * Helps users understand their recent brewing patterns.
 */
data class RecentTrend(
    val shotCount: Int, // Number of recent shots analyzed (3-5)
    val perfectShotCount: Int, // How many were perfect
    val goodShotCount: Int, // How many were good (within range)
    val averageQualityScore: Int, // Average quality score (0-100)
    val consistencyMessage: String // E.g., "Your last 3 shots were consistently good!"
)

/**
 * Dial-in status for a specific bean.
 * Tracks the journey to finding optimal settings.
 */
data class DialInStatus(
    val isDialedIn: Boolean, // Has the user found good settings?
    val shotCount: Int, // Total shots with this bean
    val shotsToDialIn: Int?, // How many shots it took to dial in (null if not yet dialed in)
    val statusMessage: String // E.g., "Dialed in Fazenda in 5 shots!"
)

/**
 * Analysis of how effective grind coaching suggestions have been.
 * Tracks if users followed suggestions and the outcomes.
 */
data class GrindCoachingEffectiveness(
    val suggestionFollowRate: Int, // Percentage of suggestions followed (0-100)
    val successRateWhenFollowed: Int, // Percentage of successful shots when suggestions followed
    val totalSuggestionsGiven: Int, // Total number of suggestions provided
    val totalSuggestionsFollowed: Int, // How many were actually followed
    val effectivenessMessage: String // E.g., "Following grind suggestions helped achieve 80% perfect shots"
)

/**
 * Achievement for a specific shot with a specific bean.
 * Represents a milestone or notable accomplishment in the brewing journey.
 */
data class Achievement(
    val type: AchievementType,
    val label: String, // Translatable label text
    val emoji: String, // Visual indicator
    val beanId: String, // Bean this achievement is tied to
    val beanName: String // Bean name for display
)

/**
 * Type of achievement to determine display color and significance.
 */
enum class AchievementType {
    FIRST_PERFECT, // First perfect shot with this bean
    DIALED_IN, // Successfully dialed in the bean
    CONSISTENCY // Consistency streak with the bean
}
