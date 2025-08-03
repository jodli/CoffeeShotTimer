package com.jodli.coffeeshottimer.domain.exception

import com.jodli.coffeeshottimer.domain.model.DomainErrorCode

/**
 * Exception class for domain-specific errors.
 * Carries a domain error code for proper error handling in the UI layer.
 */
class DomainException(
    val errorCode: DomainErrorCode,
    val details: String? = null,
    cause: Throwable? = null
) : Exception("Domain error: $errorCode${details?.let { " - $it" } ?: ""}", cause)
