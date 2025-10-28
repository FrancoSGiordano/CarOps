// ReminderReceiver.kt
package com.example.trabajointegrador_modulonativo.ui.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.trabajointegrador_modulonativo.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.trabajointegrador_modulonativo.model.Reminder
import android.app.PendingIntent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "reminders_channel"
        const val NOTIF_ID_BASE = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminderId")
        val title = intent.getStringExtra("title") ?: "Recordatorio"

        // 1) Mostrar notificación
        val notifIntent = Intent(context, /* actividad que querés abrir al tocar la notificación */ com.example.trabajointegrador_modulonativo.carDetailHostActivity::class.java).apply {
            // pasar extras si hace falta
        }
        val pending = PendingIntent.getActivity(
            context,
            (reminderId ?: "").hashCode(),
            notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // tu icono
            .setContentTitle("Recordatorio")
            .setContentText(title)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // para que resulte visible
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Opcional: full-screen (call-style) - usar solo si es una alarma crítica
        // val fullScreenIntent = Intent(context, IncomingCallActivity::class.java)
        // val fullScreenPending = PendingIntent.getActivity(context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // builder.setFullScreenIntent(fullScreenPending, true)

        NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + (reminderId ?: "").hashCode(), builder.build())

        // 2) Actualizar Firestore: marcar PENDIENTE y notificationSent = true
        try {
            if (reminderId != null) {
                val db = FirebaseFirestore.getInstance()
                val updates = mapOf("state" to "PENDIENTE", "notificationSent" to true)
                db.collection("reminders").document(reminderId)
                    .update(updates)
                    .addOnSuccessListener { Log.d("ReminderReceiver", "Firestore actualizado para $reminderId") }
                    .addOnFailureListener { e -> Log.w("ReminderReceiver", "Error update Firestore", e) }
            }
        } catch (e: Exception) {
            Log.w("ReminderReceiver", "Error actualizando Firestore: ${e.message}")
        }
    }
}
