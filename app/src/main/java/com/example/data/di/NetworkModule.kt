package com.example.data.di

import com.example.data.api.GitHubService
import com.example.data.api.LeetCodeService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val GITHUB_BASE_URL = "https://api.github.com/"
    private const val LEETCODE_BASE_URL = "https://alfa-leetcode-api.onrender.com/"

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val githubService: GitHubService by lazy {
        Retrofit.Builder()
            .baseUrl(GITHUB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GitHubService::class.java)
    }

    val leetcodeService: LeetCodeService by lazy {
        Retrofit.Builder()
            .baseUrl(LEETCODE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(LeetCodeService::class.java)
    }
}
