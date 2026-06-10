package com.example.data.api

import com.example.data.model.*
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.*

interface GitHubService {
    @GET("users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getUserRepos(@Path("username") username: String): List<GitHubRepo>

    @GET("users/{username}/events")
    suspend fun getUserEvents(@Path("username") username: String): List<GitHubEvent>

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authHeader: String
    ): GitHubUser

    @GET("user/repos")
    suspend fun getAuthenticatedUserRepos(
        @Header("Authorization") authHeader: String,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubRepo>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepoMetadata(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authHeader: String? = null
    ): GitHubRepoDetailResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getRepoContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") authHeader: String? = null
    ): List<GitHubContentItem>

    @GET("repos/{owner}/{repo}/languages")
    suspend fun getRepoLanguages(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authHeader: String? = null
    ): Map<String, Long>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getRepoCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 1,
        @Header("Authorization") authHeader: String? = null
    ): List<GitHubCommitResponse>

    @GET("repos/{owner}/{repo}/git/trees/{branch}")
    suspend fun getRepoTreeRecursive(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Query("recursive") recursive: Int = 1,
        @Header("Authorization") authHeader: String? = null
    ): GitHubTreeResponse

    @GET("rate_limit")
    suspend fun getRateLimit(
        @Header("Authorization") authHeader: String? = null
    ): GitHubRateLimitResponse

    @GET
    suspend fun downloadRawFile(
        @Url url: String,
        @Header("Authorization") authHeader: String? = null
    ): ResponseBody
}

@JsonClass(generateAdapter = true)
data class GitHubCommitResponse(
    val sha: String,
    val commit: GitHubCommitInfo
)

@JsonClass(generateAdapter = true)
data class GitHubCommitInfo(
    val committer: GitHubCommitterInfo,
    val message: String
)

@JsonClass(generateAdapter = true)
data class GitHubCommitterInfo(
    val date: String
)

@JsonClass(generateAdapter = true)
data class GitHubTreeResponse(
    val tree: List<GitHubTreeEntry>
)

@JsonClass(generateAdapter = true)
data class GitHubTreeEntry(
    val path: String,
    val mode: String,
    val type: String, // "blob" (file) or "tree" (dir)
    val sha: String,
    val size: Long? = 0L,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepoDetailResponse(
    val name: String,
    val description: String? = null,
    val owner: GitHubRepoOwner,
    val default_branch: String? = "main",
    val private: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubRepoOwner(
    val login: String,
    val avatar_url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRateLimitResponse(
    val resources: GitHubRateLimitResources,
    val rate: GitHubRateLimitInfo
)

@JsonClass(generateAdapter = true)
data class GitHubRateLimitResources(
    val core: GitHubRateLimitInfo,
    val search: GitHubRateLimitInfo
)

@JsonClass(generateAdapter = true)
data class GitHubRateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val reset: Long,
    val used: Int
)

@JsonClass(generateAdapter = true)
data class GitHubContentItem(
    val name: String,
    val path: String,
    val type: String, // "dir" or "file"
    val download_url: String? = null,
    val size: Int = 0
)

interface LeetCodeService {
    @GET("{username}")
    suspend fun getUserProfile(@Path("username") username: String): LeetCodeProfileResponse

    @GET("{username}/solved")
    suspend fun getUserSolved(@Path("username") username: String): LeetCodeSolvedResponse

    @GET("{username}/contest")
    suspend fun getUserContest(@Path("username") username: String): LeetCodeContestResponse

    @GET("{username}/calendar")
    suspend fun getUserCalendar(@Path("username") username: String): LeetCodeCalendarResponse

    @GET("{username}/submission")
    suspend fun getUserSubmissions(@Path("username") username: String): LeetCodeSubmissionsResponse
}

