package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.CodePulseDatabase
import com.example.data.pref.CodePulsePrefs
import com.example.data.repository.CodePulseRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = CodePulsePrefs(context)
        if (!prefs.isNotificationEnabled()) {
            return Result.success()
        }

        val database = CodePulseDatabase.getDatabase(context)
        val repository = CodePulseRepository(context, database, prefs)

        // Generate tailored message based on actual user code goals from cache
        val goals = repository.getGoalsFlow().firstOrNull() ?: emptyList()
        val lcUser = prefs.getLeetcodeUsername()
        val lcStats = if (lcUser.isNotBlank()) database.leetCodeStatsDao().getStats(lcUser) else null

        val message = when {
            lcStats != null && lcStats.currentStreak > 0 -> {
                "🔥 Keep your ${lcStats.currentStreak}-day streak alive! Log in to CodePulse to sync today's progress."
            }
            goals.any { !it.isCompleted } -> {
                val incompleteGoal = goals.first { !it.isCompleted }
                val remaining = incompleteGoal.target - incompleteGoal.current
                "🎯 Goal Progress: Only $remaining units away from hitting goal '${incompleteGoal.title}'!"
            }
            else -> {
                val tips = listOf(
                    "💡 You haven't solved a problem today. Keep learning!",
                    "🚀 A commit a day keeps the bugs away. Push some code to GitHub!",
                    "📈 Check out your personalized coding insights on CodePulse today!"
                )
                tips.random()
            }
        }

        dispatchNotification("CodePulse Reminder", message)
        return Result.success()
    }

    private fun dispatchNotification(title: String, text: String) {
        val channelId = "codepulse_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "CodePulse Reminders"
            val descriptionText = "Scheduled consistency reminders and goal tracking alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Generate drawable or use system default launcher icon
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(Random.nextInt(1000, 9999), builder.build())
    }
}
