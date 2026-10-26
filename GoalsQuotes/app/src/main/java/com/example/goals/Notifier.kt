package com.example.goals

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.graphics.drawable.Icon

object Notifier {
    private const val CHANNEL = "reminders"

    fun show(ctx: Context, kind: String) {
        val store = Store(ctx)
        val items = store.load(kind).filter { it.enabled }
        if (items.isEmpty()) return

        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Напоминания", NotificationManager.IMPORTANCE_HIGH)
        )

        fun fmt(i: Item): String =
            if (kind == "quotes")
                "«${i.text}»" + if (i.author.isNotBlank()) " — ${i.author}" else ""
            else i.text

        val body = when (store.mode(kind)) {
            "seq" -> fmt(items[store.nextIndex(kind, items.size)])
            "random" -> fmt(items.random())
            else -> items.joinToString("\n") { "• " + fmt(it) }
        }

        val open = PendingIntent.getActivity(
            ctx, 100, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val id = if (kind == "goals") 1 else 2
        val done = PendingIntent.getBroadcast(
            ctx, 200 + id, Intent(ctx, DoneReceiver::class.java).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (kind == "goals") "Мои цели" else "Цитата")
            .setContentText(body.lines().first())
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(ctx, android.R.drawable.ic_menu_close_clear_cancel),
                    "Прочитано", done
                ).build()
            )
            .build()

        nm.notify(id, n)
    }
}

class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        ctx.getSystemService(NotificationManager::class.java)
            .cancel(intent.getIntExtra("id", 0))
    }
}
