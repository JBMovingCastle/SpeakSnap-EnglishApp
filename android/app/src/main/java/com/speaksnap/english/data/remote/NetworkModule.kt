package com.speaksnap.english.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    const val ANTHROPIC_BASE = "https://api.anthropic.com/"
    const val DEEPSEEK_BASE  = "https://api.deepseek.com/"
    const val TONGYI_BASE    = "https://dashscope.aliyuncs.com/"
    const val DOUBAO_BASE    = "https://ark.cn-beijing.volces.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)       // TTS/OCR can be slow
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(base: String) = Retrofit.Builder()
        .baseUrl(base)
        .client(okHttp())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ---- Lazy Retrofit instances ----
    val anthropic: Retrofit by lazy { retrofit(ANTHROPIC_BASE) }
    val deepseek:  Retrofit by lazy { retrofit(DEEPSEEK_BASE) }
    val tongyi:    Retrofit by lazy { retrofit(TONGYI_BASE) }
    val doubao:    Retrofit by lazy { retrofit(DOUBAO_BASE) }

    // ---- Type-safe service creators ----
    inline fun <reified T> anthropicService(): T = anthropic.create(T::class.java)
    inline fun <reified T> deepseekService():  T = deepseek.create(T::class.java)
    inline fun <reified T> tongyiService():    T = tongyi.create(T::class.java)
    inline fun <reified T> doubaoService():    T = doubao.create(T::class.java)
}
