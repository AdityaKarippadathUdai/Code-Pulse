package com.example.data.api

import com.example.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    @GET("users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getUserRepos(@Path("username") username: String): List<GitHubRepo>

    @GET("users/{username}/events")
    suspend fun getUserEvents(@Path("username") username: String): List<GitHubEvent>
}

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
