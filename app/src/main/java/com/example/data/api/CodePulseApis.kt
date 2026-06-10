package com.example.data.api

import com.example.data.model.*
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubService {
    @GET("users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getUserRepos(@Path("username") username: String): List<GitHubRepo>

    @GET("users/{username}/events")
    suspend fun getUserEvents(@Path("username") username: String): List<GitHubEvent>

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
}

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
    val login: String
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

