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

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY name ASC")
    fun getAllTopicsFlow(): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Query("DELETE FROM topics")
    suspend fun clearAll()
}

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problems ORDER BY title ASC")
    fun getAllProblemsFlow(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE topic = :topic ORDER BY leetcodeId ASC, title ASC")
    fun getProblemsByTopicFlow(topic: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE id = :id LIMIT 1")
    suspend fun getProblemById(id: String): ProblemEntity?

    @Query("SELECT * FROM problems WHERE id = :id LIMIT 1")
    fun getProblemByIdFlow(id: String): Flow<ProblemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<ProblemEntity>)

    @Update
    suspend fun updateProblem(problem: ProblemEntity)

    @Query("UPDATE problems SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: String, favorite: Boolean)

    @Query("SELECT * FROM problems WHERE favorite = 1")
    fun getFavoriteProblemsFlow(): Flow<List<ProblemEntity>>

    @Query("SELECT problems.* FROM problems INNER JOIN recently_viewed ON problems.id = recently_viewed.problemId ORDER BY recently_viewed.viewedAt DESC LIMIT 50")
    fun getRecentlyViewedProblemsFlow(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE title LIKE '%' || :query || '%' OR topic LIKE '%' || :query || '%' OR language LIKE '%' || :query || '%' OR CAST(leetcodeId as TEXT) LIKE :query || '%'")
    fun searchProblems(query: String): Flow<List<ProblemEntity>>

    @Query("DELETE FROM problems")
    suspend fun clearAll()
}

@Dao
interface RepositoryInfoDao {
    @Query("SELECT * FROM repository_info LIMIT 1")
    fun getInfoFlow(): Flow<RepositoryInfoEntity?>

    @Query("SELECT * FROM repository_info LIMIT 1")
    suspend fun getInfo(): RepositoryInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfo(info: RepositoryInfoEntity)

    @Query("DELETE FROM repository_info")
    suspend fun clearAll()
}

@Dao
interface RecentlyViewedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyViewed(entry: RecentlyViewedEntity)

    @Query("DELETE FROM recently_viewed")
    suspend fun clearAll()
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE problemId = :problemId")
    suspend fun deleteFavorite(problemId: String)

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}

@Dao
interface VaultRepositoryDao {
    @Query("SELECT * FROM vault_repositories ORDER BY isFavorite DESC, orderIndex ASC, id DESC")
    fun getAllRepositoriesFlow(): Flow<List<VaultRepositoryEntity>>

    @Query("SELECT * FROM vault_repositories WHERE id = :id LIMIT 1")
    fun getRepositoryFlow(id: Int): Flow<VaultRepositoryEntity?>

    @Query("SELECT * FROM vault_repositories WHERE id = :id LIMIT 1")
    suspend fun getRepository(id: Int): VaultRepositoryEntity?

    @Query("SELECT * FROM vault_repositories WHERE owner = :owner AND repo = :repo LIMIT 1")
    suspend fun getRepositoryByPath(owner: String, repo: String): VaultRepositoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: VaultRepositoryEntity): Long

    @Update
    suspend fun updateRepository(repo: VaultRepositoryEntity)

    @Update
    suspend fun updateRepositories(repos: List<VaultRepositoryEntity>)

    @Delete
    suspend fun deleteRepository(repo: VaultRepositoryEntity)

    @Query("DELETE FROM vault_repositories")
    suspend fun clearAll()
}

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files WHERE repoId = :repoId ORDER BY type ASC, name ASC")
    fun getFilesForRepoFlow(repoId: Int): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE repoId = :repoId AND path = :path LIMIT 1")
    suspend fun getFileByPath(repoId: Int, path: String): VaultFileEntity?

    @Query("SELECT * FROM vault_files WHERE pathId = :pathId LIMIT 1")
    suspend fun getFileByPathId(pathId: String): VaultFileEntity?

    @Query("SELECT * FROM vault_files WHERE repoId = :repoId AND parentPath = :parentPath ORDER BY type ASC, name ASC")
    fun getFilesByParentFolder(repoId: Int, parentPath: String): Flow<List<VaultFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<VaultFileEntity>)

    @Query("DELETE FROM vault_files WHERE repoId = :repoId")
    suspend fun deleteFilesForRepo(repoId: Int)

    @Query("DELETE FROM vault_files")
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
        CodingHistoryEntity::class,
        TopicEntity::class,
        ProblemEntity::class,
        RepositoryInfoEntity::class,
        RecentlyViewedEntity::class,
        FavoriteEntity::class,
        VaultRepositoryEntity::class,
        VaultFileEntity::class
    ],
    version = 4,
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
    abstract fun topicDao(): TopicDao
    abstract fun problemDao(): ProblemDao
    abstract fun repositoryInfoDao(): RepositoryInfoDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun vaultRepositoryDao(): VaultRepositoryDao
    abstract fun vaultFileDao(): VaultFileDao


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
