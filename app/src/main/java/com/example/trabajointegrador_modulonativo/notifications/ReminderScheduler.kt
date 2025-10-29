package com.example.trabajointegrador_modulonativo.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.trabajointegrador_modulonativo.model.Reminder
import com.example.trabajointegrador_modulonativo.ui.reminder.ReminderReceiver

object ReminderScheduler {

    private fun buildIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminderId", reminder.id)
            putExtra("title", reminder.title)
        }
        val requestCode = (reminder.id ?: "").hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildIntent(context, reminder)
        val triggerAtMillis = reminder.notifyAt?.toDate()?.time ?: return

        // Recomendado: exacto y que funcione en Doze (usa permiso exacto en Android 12+ si necesario)
        alarmManager.setExactAndAllowWhileIdle( AlarmManager.RTC_WAKEUP, triggerAtMillis, pi ) }

    fun cancel(context: Context, reminderId: String) {
        val dummy =
            Reminder(id = reminderId, userId = null, carId = null, title = "", notifyAt = null)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildIntent(context, dummy)
        alarmManager.cancel(pi)
        pi.cancel()
    }
}