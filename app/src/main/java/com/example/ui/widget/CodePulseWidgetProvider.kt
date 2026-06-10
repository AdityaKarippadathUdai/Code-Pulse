package com.example.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.R
import com.example.data.db.CodePulseDatabase
import com.example.data.pref.CodePulsePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CodePulseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val database = CodePulseDatabase.getDatabase(context)
        val prefs = CodePulsePrefs(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ghUser = prefs.githubUsername.firstOrNull() ?: ""
                val lcUser = prefs.leetcodeUsername.firstOrNull() ?: ""

                var commitsToday = 0
                var leetcodeSolved = 0

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                if (ghUser.isNotBlank()) {
                    val ghDao = database.gitHubStatsDao()
                    val ghStats = ghDao.getStats(ghUser)
                    if (ghStats != null) {
                        val commitsMap = ghStats.commitsPerDayJson.split(",")
                            .filter { it.contains(":") }
                            .associate {
                                val splitVal = it.split(":")
                                splitVal[0] to (splitVal[1].toIntOrNull() ?: 0)
                            }
                        commitsToday = commitsMap[todayStr] ?: 3
                    }
                }

                if (lcUser.isNotBlank()) {
                    val lcDao = database.leetCodeStatsDao()
                    val lcStats = lcDao.getStats(lcUser)
                    if (lcStats != null) {
                        leetcodeSolved = lcStats.totalSolved
                    }
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.codepulse_widget_layout)
                    views.setTextViewText(R.id.widget_github_commits, commitsToday.toString())
                    views.setTextViewText(R.id.widget_leetcode_solved, leetcodeSolved.toString())

                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    views.setTextViewText(R.id.widget_footer, "Synced at: $timeStr")

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (ex: Exception) {
                Log.e("CodePulseWidget", "Failed widget update: ", ex)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidgetComponentName = ComponentName(context.packageName, javaClass.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
