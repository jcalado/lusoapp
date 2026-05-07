package pt.meshcore.lusoapp

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
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

    companion object {
        // Deep-link URIs handled by Flutter side via HomeWidget.widgetClicked.
        private const val URI_OPEN     = "meshcore-widget://open"
        private const val URI_ADVERT   = "meshcore-widget://action/advert"
        private const val URI_SOS      = "meshcore-widget://action/sos"
        private const val URI_CHATS    = "meshcore-widget://nav/channels"
        private const val URI_MAP      = "meshcore-widget://nav/map"
        private const val URI_CONNECT  = "meshcore-widget://nav/connect"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
        ) {
            // home_widget stores data in SharedPreferences under "HomeWidgetPreferences"
            // Use HomeWidgetPlugin.getData() so the key stays in sync with the library.
            val prefs = HomeWidgetPlugin.getData(context)

            val radioName   = prefs.getString("radio_name",   "—")     ?: "—"
            val connected   = prefs.getBoolean("connected",   false)
            val gpsSharing  = prefs.getBoolean("gps_sharing", false)
            val batteryPct  = (prefs.all["battery_pct"]  as? Number)?.toInt() ?: 0
            val contacts    = (prefs.all["contact_count"] as? Number)?.toInt() ?: 0
            val channels    = (prefs.all["channel_count"] as? Number)?.toInt() ?: 0
            val lastUpdated = prefs.getString("last_updated", "--:--") ?: "--:--"

            Log.d("MCWidget", "update: radio=$radioName connected=$connected gps=$gpsSharing " +
                    "bat=$batteryPct% contacts=$contacts channels=$channels ts=$lastUpdated")

            val views = RemoteViews(context.packageName, R.layout.widget_meshcore)

            views.setTextViewText(R.id.widget_radio_name, radioName)

            // GPS-sharing badge — visible only when the user opted in.
            views.setViewVisibility(
                R.id.widget_gps_badge,
                if (gpsSharing) android.view.View.VISIBLE else android.view.View.GONE,
            )

            views.setTextViewText(
                R.id.widget_status,
                if (connected) "ONLINE" else "OFFLINE",
            )
            views.setInt(
                R.id.widget_status_dot,
                "setBackgroundResource",
                if (connected) {
                    R.drawable.widget_status_dot_online
                } else {
                    R.drawable.widget_status_dot_offline
                },
            )

            views.setTextViewText(R.id.widget_battery,  "$batteryPct%")
            views.setImageViewResource(
                R.id.widget_battery_icon,
                batteryIconFor(batteryPct),
            )
            // Semantic tint: critical → red, low → brand orange, else neutral.
            val batteryTint = when {
                batteryPct <= 15 -> Color.parseColor("#FFB3261E")
                batteryPct <= 40 -> Color.parseColor("#FFFF6B00")
                else             -> null
            }
            views.setInt(
                R.id.widget_battery_icon,
                "setColorFilter",
                batteryTint ?: context.getColor(R.color.widget_on_surface_variant),
            )
            if (batteryTint != null) {
                views.setTextColor(R.id.widget_battery, batteryTint)
            } else {
                views.setTextColor(
                    R.id.widget_battery,
                    context.getColor(R.color.widget_on_surface),
                )
            }
            views.setTextViewText(R.id.widget_contacts, contacts.toString())
            views.setTextViewText(R.id.widget_channels, channels.toString())
            views.setTextViewText(R.id.widget_updated,  lastUpdated)

            // Connect button: filled-tonal when off, brand orange when on.
            views.setTextViewText(
                R.id.widget_btn_connect_label,
                if (connected) "LIGADO" else "LIGAR",
            )
            views.setInt(
                R.id.widget_btn_connect,
                "setBackgroundResource",
                if (connected) {
                    R.drawable.widget_action_btn_active_bg
                } else {
                    R.drawable.widget_action_btn_bg
                },
            )
            views.setTextColor(
                R.id.widget_btn_connect_label,
                if (connected) {
                    Color.parseColor("#FFFF6B00")
                } else {
                    context.getColor(R.color.widget_on_surface_variant)
                },
            )
            // Power icon: white on orange when active, neutral otherwise.
            views.setInt(
                R.id.widget_btn_connect_icon,
                "setColorFilter",
                if (connected) Color.WHITE else context.getColor(R.color.widget_on_surface),
            )

            // Header (radio name + status) → just open the app.
            views.setOnClickPendingIntent(
                R.id.widget_header,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(URI_OPEN),
                ),
            )

            // Quick-action buttons — each carries a unique URI handled in Dart.
            views.setOnClickPendingIntent(
                R.id.widget_btn_sos,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(URI_SOS),
                ),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_advert,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(URI_ADVERT),
                ),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_chats,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(URI_CHATS),
                ),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_map,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(URI_MAP),
                ),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_connect,
                HomeWidgetLaunchIntent.getActivity(
                    context,
                    MainActivity::class.java,
                    Uri.parse(URI_CONNECT),
                ),
            )

            appWidgetManager.updateAppWidget(widgetId, views)
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
