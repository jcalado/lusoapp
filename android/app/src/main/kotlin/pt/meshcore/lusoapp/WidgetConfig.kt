package pt.meshcore.lusoapp

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit

/// Per-widget customization: which action keys (in display order) and the
/// accent color applied to highlight roles.
data class WidgetConfig(
    val buttons: List<String>,
    val accent: Int,
) {
    companion object {
        const val PREFS = "HomeWidgetPreferences"
        const val DEFAULT_ACCENT = 0xFFFF6B00.toInt()

        val DEFAULT_BUTTONS = listOf("sos", "advert", "chats", "map", "connect")

        val ALL_BUTTONS = listOf(
            "sos", "advert", "connect", "chats", "map", "plan333", "telemetry",
        )

        const val MIN_BUTTONS = 3
        const val MAX_BUTTONS = 5

        fun keyButtons(widgetId: Int) = "widget_${widgetId}_buttons"
        fun keyAccent(widgetId: Int)  = "widget_${widgetId}_accent"

        fun load(context: Context, widgetId: Int): WidgetConfig {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(keyButtons(widgetId), null)
            val buttons = raw
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() && it in ALL_BUTTONS }
                ?.takeIf { it.size in MIN_BUTTONS..MAX_BUTTONS }
                ?: DEFAULT_BUTTONS
            // SharedPreferences stores ints natively; older writes used a long.
            val accent = when (val v = prefs.all[keyAccent(widgetId)]) {
                is Int  -> v
                is Long -> v.toInt()
                else    -> DEFAULT_ACCENT
            }
            return WidgetConfig(buttons, accent)
        }

        fun save(context: Context, widgetId: Int, config: WidgetConfig) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                putString(keyButtons(widgetId), config.buttons.joinToString(","))
                putInt(keyAccent(widgetId), config.accent)
            }
        }

        fun clear(context: Context, widgetId: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                remove(keyButtons(widgetId))
                remove(keyAccent(widgetId))
            }
        }
    }
}
