package com.example.trabajointegrador_modulonativo.ui.reminder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.example.trabajointegrador_modulonativo.MyApp
import com.example.trabajointegrador_modulonativo.R
import com.google.firebase.firestore.FirebaseFirestore

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        const val CHANNEL_ID = MyApp.REMINDERS_CHANNEL_ID
        const val NOTIF_ID_BASE = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        ensureChannel(context)

        // Check runtime permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPerm) {
                Log.w(TAG, "No se tiene permiso POST_NOTIFICATIONS — no se mostrará la notificación")
                // Podés guardar un flag en prefs para avisar al usuario cuando abra la app que habilite notifs.
                return
            }
        }

        // Check if notifications are globally enabled for this app (user could have blocked)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notificaciones desactivadas por el usuario para esta app — abortando notify")
            return
        }

        val reminderId = intent.getStringExtra("reminderId")
        val title = intent.getStringExtra("title") ?: "Recordatorio"

        val notifId = NOTIF_ID_BASE + (reminderId ?: "").hashCode()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // asegúrate de tener este drawable
            .setContentTitle("Recordatorio")
            .setContentText(title)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            Log.d(TAG, "Notificación mostrada notifId=$notifId reminderId=$reminderId")
        } catch (se: SecurityException) {
            // Por seguridad: si algo falla con permisos no crasheamos la app
            Log.e(TAG, "SecurityException al notificar: ${se.message}", se)
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando notificación: ${e.message}", e)
        }

        // Actualizar Firestore (no crítico para mostrar la notificación)
        reminderId?.let { id ->
            val updates = mapOf("state" to "PENDIENTE", "notificationSent" to true)
            FirebaseFirestore.getInstance().collection("reminders").document(id)
                .update(updates)
                .addOnSuccessListener { Log.d(TAG, "Firestore actualizado para $id") }
                .addOnFailureListener { e -> Log.w(TAG, "Error update Firestore para $id", e) }
        }
    }

    private fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            if (nm?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones para recordatorios de autos"
                }
                nm?.createNotificationChannel(channel)
            }
        }
    }
}
