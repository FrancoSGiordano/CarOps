package com.example.trabajointegrador_modulonativo.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    companion object { private const val TAG = "BootReceiver" }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "BOOT_COMPLETED recibido — encolando reschedule worker")
            val work = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
