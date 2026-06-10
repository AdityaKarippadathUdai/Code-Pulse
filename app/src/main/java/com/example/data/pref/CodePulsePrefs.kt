package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CodePulsePrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("codepulse_prefs", Context.MODE_PRIVATE)

    // Flows to represent active state for Compose observing
    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<Int> = _themeMode

    private val _githubUsername = MutableStateFlow(getGithubUsername())
    val githubUsername: StateFlow<String> = _githubUsername

    private val _leetcodeUsername = MutableStateFlow(getLeetcodeUsername())
    val leetcodeUsername: StateFlow<String> = _leetcodeUsername

    private val _isLoggedIn = MutableStateFlow(isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _githubToken = MutableStateFlow(getGithubToken())
    val githubToken: StateFlow<String> = _githubToken

    private val _selectedRepo = MutableStateFlow(getSelectedRepo())
    val selectedRepo: StateFlow<String> = _selectedRepo

    companion object {
        private const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
        private const val KEY_GITHUB_USER = "github_username"
        private const val KEY_LEETCODE_USER = "leetcode_username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_NOTIFICATION_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_TIME = "notification_time" // "HH:MM"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval" // in minutes, e.g. 15, 30, 60
        private const val KEY_GITHUB_OAUTH_TOKEN = "github_oauth_token" // Representing EncryptedSharedPreferences
        private const val KEY_SELECTED_REPO_PATH = "selected_repo_path"
    }

    private fun encrypt(data: String): String {
        return android.util.Base64.encodeToString(data.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
    }

    private fun decrypt(encrypted: String): String {
        if (encrypted.isBlank()) return ""
        return try {
            String(android.util.Base64.decode(encrypted, android.util.Base64.NO_WRAP), Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    fun getGithubToken(): String {
        val encrypted = prefs.getString(KEY_GITHUB_OAUTH_TOKEN, "") ?: ""
        return decrypt(encrypted)
    }

    fun setGithubToken(token: String) {
        val encrypted = encrypt(token)
        prefs.edit().putString(KEY_GITHUB_OAUTH_TOKEN, encrypted).apply()
        _githubToken.value = token
    }

    fun getSelectedRepo(): String = prefs.getString(KEY_SELECTED_REPO_PATH, "") ?: ""
    fun setSelectedRepo(repo: String) {
        prefs.edit().putString(KEY_SELECTED_REPO_PATH, repo).apply()
        _selectedRepo.value = repo
    }


    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, 0)
    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun getGithubUsername(): String = prefs.getString(KEY_GITHUB_USER, "") ?: ""
    fun setGithubUsername(username: String) {
        prefs.edit().putString(KEY_GITHUB_USER, username).apply()
        _githubUsername.value = username
    }

    fun getLeetcodeUsername(): String = prefs.getString(KEY_LEETCODE_USER, "") ?: ""
    fun setLeetcodeUsername(username: String) {
        prefs.edit().putString(KEY_LEETCODE_USER, username).apply()
        _leetcodeUsername.value = username
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun setLoggedIn(bool: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, bool).apply()
        _isLoggedIn.value = bool
    }

    fun isNotificationEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }

    fun getNotificationTime(): String = prefs.getString(KEY_NOTIFICATION_TIME, "20:00") ?: "20:00"
    fun setNotificationTime(time: String) {
        prefs.edit().putString(KEY_NOTIFICATION_TIME, time).apply()
    }

    fun getRefreshInterval(): Int = prefs.getInt(KEY_REFRESH_INTERVAL, 30) // Default 30 min
    fun setRefreshInterval(minutes: Int) {
        prefs.edit().putInt(KEY_REFRESH_INTERVAL, minutes).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _themeMode.value = 0
        _githubUsername.value = ""
        _leetcodeUsername.value = ""
        _isLoggedIn.value = false
        _githubToken.value = ""
        _selectedRepo.value = ""
    }
}
