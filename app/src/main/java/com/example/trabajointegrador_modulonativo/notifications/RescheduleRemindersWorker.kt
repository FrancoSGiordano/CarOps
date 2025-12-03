package com.example.trabajointegrador_modulonativo.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trabajointegrador_modulonativo.data.ReminderRepository
import com.example.trabajointegrador_modulonativo.model.Reminder
import com.example.trabajointegrador_modulonativo.notifications.ReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
class RescheduleRemindersWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object { private const val TAG = "RescheduleWorker" }

    override suspend fun doWork(): Result {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Log.w(TAG, "No hay usuario autenticado en boot — no se reprogramarán reminders user-scoped")
                return Result.success()
            }

            val db = FirebaseFirestore.getInstance()
            val now = com.google.firebase.Timestamp.now()

            val snap = db.collection("reminders")
                .whereEqualTo("userId", uid)
                .whereEqualTo("pending", false)
                .whereGreaterThanOrEqualTo("notifyAt", now)
                .get()
                .await()

            for (doc in snap.documents) {
                val r = doc.toObject(Reminder::class.java) ?: continue
                r.id = doc.id
                ReminderScheduler.schedule(applicationContext, r)
                Log.d(TAG, "Recordatorio reprogramado: ${r.title} para ${r.notifyAt?.toDate()}")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error reprogramando reminders: ${e.message}", e)
            return Result.retry()
        }
    }
}
