package pt.meshcore.lusoapp

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetLaunchIntent
import es.antonborri.home_widget.HomeWidgetPlugin

class MeshCoreWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            WidgetConfig.clear(context, widgetId)
        }
    }

    companion object {
        // Deep-link URIs handled by Flutter side via HomeWidget.widgetClicked.
        private const val URI_OPEN      = "meshcore-widget://open"
        private const val URI_ADVERT    = "meshcore-widget://action/advert"
        private const val URI_SOS       = "meshcore-widget://action/sos"
        private const val URI_CHATS     = "meshcore-widget://nav/channels"
        private const val URI_MAP       = "meshcore-widget://nav/map"
        private const val URI_CONNECT   = "meshcore-widget://nav/connect"
        private const val URI_PLAN333   = "meshcore-widget://nav/apps/plan333"
        private const val URI_TELEMETRY = "meshcore-widget://nav/apps/telemetry"

        // Slot resource IDs in display order.
        private val slotWraps = intArrayOf(
            R.id.widget_btn_wrap_0, R.id.widget_btn_wrap_1, R.id.widget_btn_wrap_2,
            R.id.widget_btn_wrap_3, R.id.widget_btn_wrap_4,
        )
        private val slotFrames = intArrayOf(
            R.id.widget_btn_slot_0, R.id.widget_btn_slot_1, R.id.widget_btn_slot_2,
            R.id.widget_btn_slot_3, R.id.widget_btn_slot_4,
        )
        private val slotIcons = intArrayOf(
            R.id.widget_btn_icon_0, R.id.widget_btn_icon_1, R.id.widget_btn_icon_2,
            R.id.widget_btn_icon_3, R.id.widget_btn_icon_4,
        )
        private val slotLabels = intArrayOf(
            R.id.widget_btn_label_0, R.id.widget_btn_label_1, R.id.widget_btn_label_2,
            R.id.widget_btn_label_3, R.id.widget_btn_label_4,
        )

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
        ) {
            val prefs = HomeWidgetPlugin.getData(context)

            val radioName   = prefs.getString("radio_name",   "—")     ?: "—"
            val connected   = prefs.getBoolean("connected",   false)
            val gpsSharing  = prefs.getBoolean("gps_sharing", false)
            val batteryPct  = (prefs.all["battery_pct"]   as? Number)?.toInt() ?: 0
            val contacts    = (prefs.all["contact_count"] as? Number)?.toInt() ?: 0
            val channels    = (prefs.all["channel_count"] as? Number)?.toInt() ?: 0
            val lastUpdated = prefs.getString("last_updated", "--:--") ?: "--:--"

            val config = WidgetConfig.load(context, widgetId)
            val accent = config.accent

            Log.d(
                "MCWidget",
                "update id=$widgetId radio=$radioName connected=$connected " +
                "gps=$gpsSharing bat=$batteryPct% contacts=$contacts " +
                "channels=$channels ts=$lastUpdated buttons=${config.buttons} " +
                "accent=${"#%08X".format(accent)}",
            )

            val views = RemoteViews(context.packageName, R.layout.widget_meshcore)

            views.setTextViewText(R.id.widget_radio_name, radioName)

            views.setViewVisibility(
                R.id.widget_gps_badge,
                if (gpsSharing) View.VISIBLE else View.GONE,
            )

            // Status dot: neutral white drawable tinted at runtime.
            views.setTextViewText(
                R.id.widget_status,
                if (connected) "ONLINE" else "OFFLINE",
            )
            views.setInt(
                R.id.widget_status_dot,
                "setBackgroundResource",
                if (connected) {
                    R.drawable.widget_status_dot_neutral
                } else {
                    R.drawable.widget_status_dot_offline
                },
            )
            // (RemoteViews can't setColorFilter on a FrameLayout's background
            // drawable directly — we keep the dot tinted via two separate
            // drawables: neutral (white, recolored by ColorMatrixColorFilter
            // would require API hacks) ⇒ instead use a tinted shape via
            // setColorFilter on a child ImageView. Keep it simple: replace
            // the neutral drawable with a runtime-tinted one if accent
            // differs from white. The simpler path used here: when the user
            // keeps the default accent, we already render brand-orange
            // through the default drawable mapping via setBackgroundResource
            // above using the *neutral* drawable, then apply tint by
            // swapping in a ColorStateList through android:backgroundTint.)
            //
            // Practical implementation: provide a tinted drawable per accent
            // by using setInt + "setBackgroundColor" on a circular shape.
            // FrameLayouts ignore setColorFilter via RemoteViews. The
            // robust trick is to set the background color directly:
            views.setInt(
                R.id.widget_status_dot,
                "setBackgroundColor",
                if (connected) accent else context.getColor(android.R.color.transparent),
            )

            // Battery
            views.setTextViewText(R.id.widget_battery, "$batteryPct%")
            views.setImageViewResource(
                R.id.widget_battery_icon,
                batteryIconFor(batteryPct),
            )
            val batteryTint: Int? = when {
                batteryPct <= 15 -> Color.parseColor("#FFB3261E")  // critical (semantic)
                batteryPct <= 40 -> accent                          // low → accent
                else             -> null
            }
            views.setInt(
                R.id.widget_battery_icon,
                "setColorFilter",
                batteryTint ?: context.getColor(R.color.widget_on_surface_variant),
            )
            views.setTextColor(
                R.id.widget_battery,
                batteryTint ?: context.getColor(R.color.widget_on_surface),
            )

            views.setTextViewText(R.id.widget_contacts, contacts.toString())
            views.setTextViewText(R.id.widget_channels, channels.toString())
            views.setTextViewText(R.id.widget_updated,  lastUpdated)

            // Header click → just open the app.
            views.setOnClickPendingIntent(
                R.id.widget_header,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(URI_OPEN),
                ),
            )

            // Bind action slots from config.buttons (size 3..5).
            for (i in 0 until 5) {
                val key = config.buttons.getOrNull(i)
                if (key == null) {
                    views.setViewVisibility(slotWraps[i], View.GONE)
                    continue
                }
                views.setViewVisibility(slotWraps[i], View.VISIBLE)
                bindSlot(context, views, i, key, connected, accent, key in config.labels)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun bindSlot(
            context: Context,
            views: RemoteViews,
            slotIndex: Int,
            key: String,
            connected: Boolean,
            accent: Int,
            showLabel: Boolean,
        ) {
            val frame = slotFrames[slotIndex]
            val icon  = slotIcons[slotIndex]
            val label = slotLabels[slotIndex]

            // Defaults — overridden per key below.
            var iconRes = R.drawable.ic_widget_power
            var iconTint = context.getColor(R.color.widget_on_surface)
            var bgRes = R.drawable.widget_action_btn_bg
            var bgTint: Int? = null  // null ⇒ leave drawable colour as-is
            var labelText = ""
            var labelColor = context.getColor(R.color.widget_on_surface_variant)
            var uri = URI_OPEN

            when (key) {
                "sos" -> {
                    iconRes  = R.drawable.ic_widget_warning
                    iconTint = context.getColor(R.color.widget_on_error)
                    bgRes    = R.drawable.widget_action_btn_sos_bg
                    labelText = context.getString(R.string.widget_btn_sos)
                    uri = URI_SOS
                }
                "advert" -> {
                    iconRes = R.drawable.ic_widget_broadcast
                    labelText = context.getString(R.string.widget_btn_advert)
                    uri = URI_ADVERT
                }
                "connect" -> {
                    iconRes = R.drawable.ic_widget_power
                    if (connected) {
                        bgRes      = R.drawable.widget_action_btn_active_neutral
                        bgTint     = accent
                        iconTint   = Color.WHITE
                        labelText  = "LIGADO"
                        labelColor = accent
                    } else {
                        labelText  = "LIGAR"
                    }
                    uri = URI_CONNECT
                }
                "chats" -> {
                    iconRes = R.drawable.ic_widget_chat
                    labelText = context.getString(R.string.widget_btn_chats)
                    uri = URI_CHATS
                }
                "map" -> {
                    iconRes = R.drawable.ic_widget_map
                    labelText = context.getString(R.string.widget_btn_map)
                    uri = URI_MAP
                }
                "plan333" -> {
                    iconRes = R.drawable.ic_widget_plan333
                    labelText = context.getString(R.string.widget_btn_plan333)
                    uri = URI_PLAN333
                }
                "telemetry" -> {
                    iconRes = R.drawable.ic_widget_telemetry
                    labelText = context.getString(R.string.widget_btn_telemetry)
                    uri = URI_TELEMETRY
                }
            }

            views.setImageViewResource(icon, iconRes)
            views.setInt(icon, "setColorFilter", iconTint)
            views.setInt(frame, "setBackgroundResource", bgRes)
            if (bgTint != null) {
                views.setInt(frame, "setBackgroundColor", bgTint)
            }
            views.setTextViewText(label, labelText)
            views.setTextColor(label, labelColor)
            views.setViewVisibility(
                label,
                if (showLabel) View.VISIBLE else View.GONE,
            )
            views.setOnClickPendingIntent(
                frame,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(uri),
                ),
            )
        }

        private fun batteryIconFor(pct: Int): Int = when {
            pct <= 15 -> R.drawable.ic_widget_battery_alert
            pct <= 40 -> R.drawable.ic_widget_battery_low
            pct <= 70 -> R.drawable.ic_widget_battery_mid
            pct <= 90 -> R.drawable.ic_widget_battery_high
            else      -> R.drawable.ic_widget_battery_full
        }
    }
}
