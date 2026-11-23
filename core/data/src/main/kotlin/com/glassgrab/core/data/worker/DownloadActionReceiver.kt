package com.glassgrab.core.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.glassgrab.core.engine.YtDlpEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DownloadActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var engine: YtDlpEngine

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        when (intent.action) {
            ACTION_PAUSE, ACTION_CANCEL -> {
                engine.cancelProcess(taskId)
                WorkManager.getInstance(context).cancelUniqueWork("download_$taskId")
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.glassgrab.action.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL = "com.glassgrab.action.CANCEL_DOWNLOAD"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
