package com.glassgrab.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.domain.repository.MediaExtractorRepository
import com.glassgrab.core.domain.repository.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class EngineUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val extractorRepository: MediaExtractorRepository,
    private val preferencesRepository: PreferencesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.preferences.first()
        if (!prefs.autoUpdateEngine) return Result.success()
        val now = System.currentTimeMillis()
        if (now - prefs.lastEngineUpdateEpochMs < ONE_DAY_MS) {
            return Result.success()
        }
        return when (val result = extractorRepository.updateEngine(force = false)) {
            is GlassResult.Success -> {
                preferencesRepository.update {
                    it.copy(
                        lastEngineUpdateEpochMs = now,
                        engineVersion = result.data,
                    )
                }
                Result.success()
            }
            is GlassResult.Failure -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "glassgrab_engine_auto_update"
        private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    }
}
