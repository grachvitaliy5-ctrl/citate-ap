package com.example.goals

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Scheduler {
    private val kinds = listOf("goals", "quotes")

    /** Ставит ближайший будильник для каждого из 4 слотов (2 цели + 2 цитаты). */
    fun scheduleAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val store = Store(ctx)
        kinds.forEachIndexed { ki, kind ->
            for (slot in 0..1) {
                val m = store.time(kind, slot)
                val c = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, m / 60)
                    set(Calendar.MINUTE, m % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (c.timeInMillis <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_YEAR, 1)

                val pi = PendingIntent.getBroadcast(
                    ctx, ki * 2 + slot,
                    Intent(ctx, AlarmReceiver::class.java).putExtra("kind", kind),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
                else
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
            }
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Notifier.show(ctx, intent.getStringExtra("kind") ?: return)
        Scheduler.scheduleAll(ctx) // ставим следующий день
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.scheduleAll(ctx)
    }
}
