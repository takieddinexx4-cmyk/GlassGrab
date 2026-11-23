package com.glassgrab.core.common

/**
 * Functional result wrapper used across every module boundary.
 * Exceptions are never thrown across layers; failures are typed via [GlassError].
 */
sealed interface GlassResult<out T> {
    data class Success<T>(val data: T) : GlassResult<T>
    data class Failure(val error: GlassError) : GlassResult<Nothing>
}

inline fun <T, R> GlassResult<T>.map(transform: (T) -> R): GlassResult<R> = when (this) {
    is GlassResult.Success -> GlassResult.Success(transform(data))
    is GlassResult.Failure -> this
}

inline fun <T> GlassResult<T>.onSuccess(action: (T) -> Unit): GlassResult<T> {
    if (this is GlassResult.Success) action(data)
    return this
}

inline fun <T> GlassResult<T>.onFailure(action: (GlassError) -> Unit): GlassResult<T> {
    if (this is GlassResult.Failure) action(error)
    return this
}

sealed interface GlassError {
    val technicalDetail: String?

    data class Offline(override val technicalDetail: String? = null) : GlassError
    data class LowStorage(
        val requiredBytes: Long? = null,
        val availableBytes: Long? = null,
        override val technicalDetail: String? = null,
    ) : GlassError

    data class PrivateOrRemoved(override val technicalDetail: String? = null) : GlassError
    data class AgeRestricted(override val technicalDetail: String? = null) : GlassError
    data class GeoBlocked(override val technicalDetail: String? = null) : GlassError
    data class DrmNotSupported(
        val platformName: String,
        override val technicalDetail: String? = null,
    ) : GlassError

    data class UnsupportedUrl(
        val rawUrl: String,
        override val technicalDetail: String? = null,
    ) : GlassError

    data class TikTokExtractionFailed(
        val primaryReason: String,
        val fallbackReason: String,
        override val technicalDetail: String? = null,
    ) : GlassError

    data class EngineUpdateFailed(override val technicalDetail: String? = null) : GlassError
    data class PermissionDenied(override val technicalDetail: String? = null) : GlassError
    data class Timeout(override val technicalDetail: String? = null) : GlassError
    data class Cancelled(override val technicalDetail: String? = null) : GlassError
    data class Unknown(override val technicalDetail: String? = null) : GlassError
}
