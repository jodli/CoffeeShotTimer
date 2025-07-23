package com.jodli.coffeeshottimer.data.model

/**
 * Data class representing paginated results for performance optimization.
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
) {
}

/**
 * Configuration for pagination requests.
 */
data class PaginationConfig(
    val page: Int = 0,
    val pageSize: Int = DEFAULT_PAGE_SIZE
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
    }

    val offset: Int = page * pageSize

    init {
        require(page >= 0) { "Page must be non-negative" }
        require(pageSize > 0) { "Page size must be positive" }
        require(pageSize <= MAX_PAGE_SIZE) { "Page size cannot exceed $MAX_PAGE_SIZE" }
    }
}