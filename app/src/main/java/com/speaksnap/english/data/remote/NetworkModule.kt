package com.speaksnap.english.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network dependency provider using simple singleton pattern.
 * Supports multiple AI providers (Anthropic Claude, DeepSeek).
 */
object NetworkModule {

    const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/"
    const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun createRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(createOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Lazy singletons
    val anthropicRetrofit: Retrofit by lazy { createRetrofit(ANTHROPIC_BASE_URL) }
    val deepseekRetrofit: Retrofit by lazy { createRetrofit(DEEPSEEK_BASE_URL) }

    inline fun <reified T> createAnthropicService(): T = anthropicRetrofit.create(T::class.java)
    inline fun <reified T> createDeepSeekService(): T = deepseekRetrofit.create(T::class.java)
}
