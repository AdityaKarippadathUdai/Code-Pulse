package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LeetCodeStatsDao {
    @Query("SELECT * FROM leetcode_stats WHERE username = :username LIMIT 1")
    fun getStatsFlow(username: String): Flow<LeetCodeStatsEntity?>

    @Query("SELECT * FROM leetcode_stats WHERE username = :username LIMIT 1")
    suspend fun getStats(username: String): LeetCodeStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: LeetCodeStatsEntity)

    @Query("DELETE FROM leetcode_stats")
    suspend fun clearAll()
}

@Dao
interface LeetCodeSubmissionDao {
    @Query("SELECT * FROM leetcode_submissions_cache WHERE username = :username ORDER BY id DESC")
    fun getSubmissions(username: String): Flow<List<LeetCodeSubmissionCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissions(submissions: List<LeetCodeSubmissionCache>)

    @Query("DELETE FROM leetcode_submissions_cache WHERE username = :username")
    suspend fun clearSubmissions(username: String)

    @Query("DELETE FROM leetcode_submissions_cache")
    suspend fun clearAll()
}

@Dao
interface GitHubStatsDao {
    @Query("SELECT * FROM github_stats WHERE username = :username LIMIT 1")
    fun getStatsFlow(username: String): Flow<GitHubStatsEntity?>

    @Query("SELECT * FROM github_stats WHERE username = :username LIMIT 1")
    suspend fun getStats(username: String): GitHubStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: GitHubStatsEntity)

    @Query("DELETE FROM github_stats")
    suspend fun clearAll()
}

@Dao
interface GitHubRepoDao {
    @Query("SELECT * FROM github_repos_cache WHERE username = :username ORDER BY stars DESC")
    fun getRepos(username: String): Flow<List<GitHubRepoCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepos(repos: List<GitHubRepoCache>)

    @Query("DELETE FROM github_repos_cache WHERE username = :username")
    suspend fun clearRepos(username: String)

    @Query("DELETE FROM github_repos_cache")
    suspend fun clearAll()
}

@Dao
interface GitHubEventDao {
    @Query("SELECT * FROM github_events_cache WHERE username = :username ORDER BY id DESC")
    fun getEvents(username: String): Flow<List<GitHubEventCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<GitHubEventCache>)

    @Query("DELETE FROM github_events_cache WHERE username = :username")
    suspend fun clearEvents(username: String)

    @Query("DELETE FROM github_events_cache")
    suspend fun clearAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdTime DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY createdTime DESC")
    suspend fun getAllGoalsList(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals")
    suspend fun clearAll()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsList(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedDate = :unlockedDate WHERE badgeId = :badgeId")
    suspend fun unlockBadge(badgeId: String, unlockedDate: String)

    @Query("DELETE FROM achievements")
    suspend fun clearAll()
}

@Dao
interface CodingHistoryDao {
    @Query("SELECT * FROM pulse_history ORDER BY date ASC")
    fun getHistory(): Flow<List<CodingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: List<CodingHistoryEntity>)

    @Query("DELETE FROM pulse_history")
    suspend fun clearAll()
}

@Database(
    entities = [
        LeetCodeStatsEntity::class,
        LeetCodeSubmissionCache::class,
        GitHubStatsEntity::class,
        GitHubRepoCache::class,
        GitHubEventCache::class,
        GoalEntity::class,
        AchievementEntity::class,
        CodingHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CodePulseDatabase : RoomDatabase() {
    abstract fun leetCodeStatsDao(): LeetCodeStatsDao
    abstract fun leetCodeSubmissionDao(): LeetCodeSubmissionDao
    abstract fun gitHubStatsDao(): GitHubStatsDao
    abstract fun gitHubRepoDao(): GitHubRepoDao
    abstract fun gitHubEventDao(): GitHubEventDao
    abstract fun goalDao(): GoalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun codingHistoryDao(): CodingHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CodePulseDatabase? = null

        fun getDatabase(context: Context): CodePulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodePulseDatabase::class.java,
                    "codepulse_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
