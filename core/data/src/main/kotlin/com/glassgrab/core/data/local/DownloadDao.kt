package com.glassgrab.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads WHERE status != 'COMPLETED' ORDER BY createdAtEpochMs DESC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAtEpochMs DESC")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, errorMessage: String? = null)

    @Query(
        """
        UPDATE downloads
        SET status = :status,
            progress = :progress,
            downloadedBytes = :downloadedBytes,
            speedBytesPerSec = :speedBytesPerSec,
            etaSeconds = :etaSeconds
        WHERE id = :id
        """,
    )
    suspend fun updateProgress(
        id: String,
        status: String,
        progress: Float,
        downloadedBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long?,
    )

    @Query(
        """
        UPDATE downloads
        SET status = 'COMPLETED',
            progress = 1.0,
            outputUri = :outputUri,
            outputFilePath = :outputFilePath,
            totalBytes = :totalBytes,
            downloadedBytes = :totalBytes,
            speedBytesPerSec = 0,
            etaSeconds = 0,
            completedAtEpochMs = :completedAtEpochMs,
            errorMessage = null
        WHERE id = :id
        """,
    )
    suspend fun markCompleted(
        id: String,
        outputUri: String,
        outputFilePath: String?,
        totalBytes: Long,
        completedAtEpochMs: Long,
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}
