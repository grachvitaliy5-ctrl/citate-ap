package com.example.goals

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Item(val id: Long, val text: String, val author: String, val enabled: Boolean)

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("data", Context.MODE_PRIVATE)

    fun load(kind: String): List<Item> {
        val arr = JSONArray(p.getString(kind, "[]"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            Item(o.getLong("id"), o.getString("text"), o.optString("author"), o.optBoolean("on", true))
        }
    }

    fun save(kind: String, items: List<Item>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("text", it.text)
                .put("author", it.author).put("on", it.enabled))
        }
        p.edit().putString(kind, arr.toString()).apply()
    }

    /** Время в минутах от полуночи. slot: 0 или 1 */
    fun time(kind: String, slot: Int): Int =
        p.getInt("t_${kind}_$slot", if (slot == 0) 9 * 60 else 20 * 60)

    fun setTime(kind: String, slot: Int, minutes: Int) =
        p.edit().putInt("t_${kind}_$slot", minutes).apply()

    /** all / seq / random */
    fun mode(kind: String): String =
        p.getString("mode_$kind", if (kind == "goals") "all" else "random")!!

    fun setMode(kind: String, mode: String) =
        p.edit().putString("mode_$kind", mode).apply()

    fun nextIndex(kind: String, size: Int): Int {
        val i = p.getInt("idx_$kind", 0) % size
        p.edit().putInt("idx_$kind", i + 1).apply()
        return i
    }
}
