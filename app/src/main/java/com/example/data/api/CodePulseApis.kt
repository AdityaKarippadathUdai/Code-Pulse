package com.example.data.api

import com.example.data.model.GitHubEvent
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.LeetCodeSubmissionsResponse
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
    @GET("{username}/submission")
    suspend fun getUserSubmissions(@Path("username") username: String): LeetCodeSubmissionsResponse
}
