package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// LeetCode Data Models
@JsonClass(generateAdapter = true)
data class LeetCodeStats(
    val username: String = "",
    val ranking: Int = 0,
    val reputation: Int = 0,
    val avatarUrl: String = "",
    val easySolved: Int = 0,
    val mediumSolved: Int = 0,
    val hardSolved: Int = 0,
    val totalSolved: Int = 0,
    val acceptanceRate: Double = 0.0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val contestRating: Int = 0,
    val globalRanking: Int = 0,
    val contestAttended: Int = 0,
    val ratingHistoryJson: String = "[]", // Array of ratings over time
    val heatmapDataJson: String = "{}", // Date -> Int Map
    val recentSubmissions: List<LeetCodeSubmission> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LeetCodeSubmission(
    val title: String,
    val status: String, // "Accepted", "Wrong Answer", etc.
    val language: String,
    val submissionDate: String
)

// GitHub Data Models
@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String = "",
    val avatar_url: String = "",
    val name: String? = null,
    val bio: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val public_repos: Int = 0,
    val totalStars: Int = 0,
    val watchers: Int = 0,
    val forks: Int = 0,
    val topLanguages: Map<String, Int> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val name: String,
    val description: String? = null,
    val stargazers_count: Int = 0,
    val forks_count: Int = 0,
    val watchers_count: Int = 0,
    val language: String? = null,
    val html_url: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubEvent(
    val type: String,
    val repo: GitHubEventRepo?,
    val created_at: String
)

@JsonClass(generateAdapter = true)
data class GitHubEventRepo(
    val name: String
)

// Database Caching Entities (Room)
@Entity(tableName = "leetcode_stats")
data class LeetCodeStatsEntity(
    @PrimaryKey val username: String,
    val ranking: Int,
    val reputation: Int,
    val avatarUrl: String,
    val easySolved: Int,
    val mediumSolved: Int,
    val hardSolved: Int,
    val totalSolved: Int,
    val acceptanceRate: Double,
    val currentStreak: Int,
    val longestStreak: Int,
    val contestRating: Int,
    val globalRanking: Int,
    val contestAttended: Int,
    val ratingHistoryJson: String,
    val heatmapDataJson: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "leetcode_submissions_cache")
data class LeetCodeSubmissionCache(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val title: String,
    val status: String,
    val language: String,
    val submissionDate: String
)

@Entity(tableName = "github_stats")
data class GitHubStatsEntity(
    @PrimaryKey val username: String,
    val avatarUrl: String,
    val name: String,
    val bio: String,
    val followers: Int,
    val following: Int,
    val publicRepos: Int,
    val totalStars: Int,
    val forkCount: Int,
    val watcherCount: Int,
    val languagesJson: String, // JSON representation of Map<String, Int>
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "github_repos_cache")
data class GitHubRepoCache(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val name: String,
    val description: String,
    val stars: Int,
    val forks: Int,
    val watchers: Int,
    val language: String,
    val htmlUrl: String
)

@Entity(tableName = "github_events_cache")
data class GitHubEventCache(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val type: String,
    val repoName: String,
    val createdAt: String
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "LC_SOLVED", "GH_COMMITS", "STREAK", "CONTESTS"
    val target: Int,
    val current: Int,
    val isCompleted: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val badgeId: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null
)

// Combined Coding Pulse Score Activity History
@Entity(tableName = "pulse_history")
data class CodingHistoryEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val problemsSolved: Int,
    val commitCount: Int,
    val score: Int
)
