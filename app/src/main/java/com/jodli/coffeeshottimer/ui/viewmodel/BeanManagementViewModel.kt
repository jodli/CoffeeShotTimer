package com.jodli.coffeeshottimer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.usecase.GetActiveBeansUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetBeanHistoryUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetShotQualityAnalysisUseCase
import com.jodli.coffeeshottimer.domain.usecase.UpdateBeanUseCase
import com.jodli.coffeeshottimer.ui.util.BeanStatus
import com.jodli.coffeeshottimer.ui.util.DomainErrorTranslator
import com.jodli.coffeeshottimer.ui.util.calculateBeanStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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
    private val shotRepository: ShotRepository,
    private val qualityAnalysisUseCase: GetShotQualityAnalysisUseCase,
    private val domainErrorTranslator: DomainErrorTranslator
) : ViewModel() {

    private val _uiState = MutableStateFlow(BeanManagementUiState())
    val uiState: StateFlow<BeanManagementUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showInactive = MutableStateFlow(false)
    val showInactive: StateFlow<Boolean> = _showInactive.asStateFlow()

    init {
        // Load the current bean ID from repository
        _uiState.value = _uiState.value.copy(
            currentBeanId = beanRepository.getCurrentBeanId()
        )
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

                        // Load statistics for all beans
                        val statistics = loadBeanStatistics(beans)

                        // Sort beans intelligently
                        val sortedBeans = sortBeans(
                            beans = beans,
                            statuses = statistics.statuses,
                            lastUsedDates = statistics.lastUsedDates,
                            currentBeanId = _uiState.value.currentBeanId
                        )

                        _uiState.value = _uiState.value.copy(
                            beans = sortedBeans,
                            beanStatuses = statistics.statuses,
                            beanShotCounts = statistics.shotCounts,
                            beanLastUsed = statistics.lastUsedDates,
                            beanGrinderSettings = statistics.grinderSettings,
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

                        // Load statistics for all beans
                        val statistics = loadBeanStatistics(beans)

                        // Sort beans intelligently
                        val sortedBeans = sortBeans(
                            beans = beans,
                            statuses = statistics.statuses,
                            lastUsedDates = statistics.lastUsedDates,
                            currentBeanId = _uiState.value.currentBeanId
                        )

                        _uiState.value = _uiState.value.copy(
                            beans = sortedBeans,
                            beanStatuses = statistics.statuses,
                            beanShotCounts = statistics.shotCounts,
                            beanLastUsed = statistics.lastUsedDates,
                            beanGrinderSettings = statistics.grinderSettings,
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
     * Load bean statistics including status, shot count, last used date, and grinder setting.
     *
     * @param beans List of beans to load statistics for
     * @return BeanStatistics containing all computed statistics
     */
    private suspend fun loadBeanStatistics(
        beans: List<Bean>
    ): BeanStatistics {
        val statuses = mutableMapOf<String, BeanStatus>()
        val shotCounts = mutableMapOf<String, Int>()
        val lastUsedDates = mutableMapOf<String, LocalDateTime?>()
        val grinderSettings = mutableMapOf<String, String?>()

        beans.forEach { bean ->
            val shotsResult = shotRepository.getShotsByBean(bean.id).first()
            val shots = shotsResult.getOrNull() ?: emptyList()

            statuses[bean.id] = calculateBeanStatus(shots, qualityAnalysisUseCase)
            shotCounts[bean.id] = shots.size
            
            // Get the most recent shot for last used date and grinder setting
            val mostRecentShot = shots.maxByOrNull { it.timestamp }
            lastUsedDates[bean.id] = mostRecentShot?.timestamp
            grinderSettings[bean.id] = mostRecentShot?.grinderSetting
        }

        return BeanStatistics(
            statuses = statuses,
            shotCounts = shotCounts,
            lastUsedDates = lastUsedDates,
            grinderSettings = grinderSettings
        )
    }

    /**
     * Sort beans intelligently based on multiple criteria.
     * Priority: active bean → dialed-in beans → fresh beans → recently used → alphabetical
     *
     * @param beans List of beans to sort
     * @param statuses Map of bean statuses
     * @param lastUsedDates Map of last used dates
     * @param currentBeanId ID of the currently active bean
     * @return Sorted list of beans
     */
    private fun sortBeans(
        beans: List<Bean>,
        statuses: Map<String, BeanStatus>,
        lastUsedDates: Map<String, LocalDateTime?>,
        currentBeanId: String?
    ): List<Bean> {
        return beans.sortedWith(
            compareBy<Bean>(
                { it.id != currentBeanId }, // Active bean first (false comes before true)
                { statuses[it.id] != BeanStatus.DIALED_IN }, // Then dialed-in beans
                { !it.isFresh() } // Then fresh beans (within 4-21 days)
            )
                .thenByDescending { lastUsedDates[it.id] } // Recently used (desc, nulls last)
                .thenBy { it.name.lowercase() } // Finally alphabetically
        )
    }

    /**
     * Set a bean as the current active bean for shot recording.
     * Implements requirement 3.2 for connecting bean selection between screens.
     */
    fun setCurrentBean(beanId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Set the bean as current in the repository
            val result = beanRepository.setCurrentBean(beanId)

            if (result.isSuccess) {
                // Update UI state to reflect the current bean selection
                _uiState.value = _uiState.value.copy(
                    currentBeanId = beanId,
                    isLoading = false
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

/**
 * Data class containing statistics for beans.
 */
private data class BeanStatistics(
    val statuses: Map<String, BeanStatus>,
    val shotCounts: Map<String, Int>,
    val lastUsedDates: Map<String, LocalDateTime?>,
    val grinderSettings: Map<String, String?>
)

/**
 * UI state for the bean management screen.
 */
data class BeanManagementUiState(
    val beans: List<Bean> = emptyList(),
    val beansWithFreshness: List<com.jodli.coffeeshottimer.domain.usecase.BeanWithFreshness> = emptyList(),
    val beanStatuses: Map<String, BeanStatus> = emptyMap(),
    val beanShotCounts: Map<String, Int> = emptyMap(),
    val beanLastUsed: Map<String, LocalDateTime?> = emptyMap(),
    val beanGrinderSettings: Map<String, String?> = emptyMap(),
    val currentBeanId: String? = null,
    val hasActiveBeans: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)
