package com.example.goals

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        Scheduler.scheduleAll(this)

        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                App()
            }
        }
    }
}

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Цели", "Цитаты", "Настройки")
    Scaffold(bottomBar = {
        NavigationBar {
            titles.forEachIndexed { i, t ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i },
                    icon = {}, label = { Text(t) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ItemsScreen("goals")
                1 -> ItemsScreen("quotes")
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
fun ItemsScreen(kind: String) {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    val list = remember { mutableStateListOf<Item>().also { it.addAll(store.load(kind)) } }
    var editing by remember { mutableStateOf<Item?>(null) }
    var adding by remember { mutableStateOf(false) }
    val isQuotes = kind == "quotes"

    fun persist() = store.save(kind, list.toList())

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { adding = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) { Text(if (isQuotes) "Добавить цитату" else "Добавить цель") }

        if (list.isEmpty()) {
            Text("Пока пусто", Modifier.padding(horizontal = 16.dp))
        }

        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(list, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { editing = item }) {
                            Text(item.text)
                            if (item.author.isNotBlank())
                                Text("— ${item.author}", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = item.enabled, onCheckedChange = { on ->
                            val i = list.indexOfFirst { it.id == item.id }
                            if (i >= 0) { list[i] = item.copy(enabled = on); persist() }
                        })
                        TextButton(onClick = { list.removeAll { it.id == item.id }; persist() }) { Text("✕") }
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        val cur = editing
        ItemDialog(
            initialText = cur?.text ?: "",
            initialAuthor = cur?.author ?: "",
            showAuthor = isQuotes,
            onDismiss = { adding = false; editing = null },
            onSave = { text, author ->
                if (cur != null) {
                    val i = list.indexOfFirst { it.id == cur.id }
                    if (i >= 0) list[i] = cur.copy(text = text, author = author)
                } else {
                    list.add(Item(System.currentTimeMillis(), text, author, true))
                }
                persist()
                adding = false; editing = null
            }
        )
    }
}

@Composable
fun ItemDialog(
    initialText: String, initialAuthor: String, showAuthor: Boolean,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var author by remember { mutableStateOf(initialAuthor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showAuthor) "Цитата" else "Цель") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text("Текст") })
                if (showAuthor) OutlinedTextField(author, { author = it }, label = { Text("Автор") })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim(), author.trim()) }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var tick by remember { mutableIntStateOf(0) }
    @Suppress("UNUSED_VARIABLE") val t = tick // перерисовка после изменений

    val modes = listOf("all" to "Все сразу", "seq" to "По очереди", "random" to "Случайная")

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((kind, title) in listOf("goals" to "Цели", "quotes" to "Цитаты")) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (slot in 0..1) {
                    val m = store.time(kind, slot)
                    OutlinedButton(onClick = {
                        TimePickerDialog(ctx, { _, h, mi ->
                            store.setTime(kind, slot, h * 60 + mi)
                            Scheduler.scheduleAll(ctx)
                            tick++
                        }, m / 60, m % 60, true).show()
                    }) { Text("%02d:%02d".format(m / 60, m % 60)) }
                }
            }
            for ((v, label) in modes) {
                Row(
                    Modifier.fillMaxWidth().clickable { store.setMode(kind, v); tick++ },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = store.mode(kind) == v, onClick = { store.setMode(kind, v); tick++ })
                    Text(label)
                }
            }
            Button(onClick = { Notifier.show(ctx, kind) }) { Text("Тестовое уведомление") }
            HorizontalDivider()
        }

        Text("Чтобы уведомления приходили вовремя", style = MaterialTheme.typography.titleMedium)
        if (Build.VERSION.SDK_INT >= 31) {
            OutlinedButton(onClick = {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                )
            }) { Text("Разрешить точные будильники") }
        }
        OutlinedButton(onClick = {
            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }) { Text("Отключить экономию батареи") }
        Text(
            "Найдите это приложение в списке и выберите «Не оптимизировать». " +
                "На Xiaomi, Samsung, Huawei и др. также включите автозапуск и разрешите работу в фоне " +
                "в настройках приложения.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
