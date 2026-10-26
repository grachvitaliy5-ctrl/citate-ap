package com.example.goals

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

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

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (kind == "goals") "Мои цели" else "Цитата")
            .setContentText(body.lines().first())
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        nm.notify(if (kind == "goals") 1 else 2, n)
    }
}
