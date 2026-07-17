package com.explorebyte.ar.data.remote

import com.explorebyte.ar.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

data class AppVersionResponse(
    val id: Long,
    val version_code: Int,
    val version_name: String?,
    val message: String?,
    val apk_url: String?,
    val created_at: String?
)

interface SupabaseApiService {
    @GET("rest/v1/app_versions?select=*&order=version_code.desc&limit=1")
    suspend fun getLatestVersion(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): List<AppVersionResponse>
}

object SupabaseClient {
    private val client = OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(if (BuildConfig.SUPABASE_URL.endsWith("/")) BuildConfig.SUPABASE_URL else "${BuildConfig.SUPABASE_URL}/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SupabaseApiService = retrofit.create(SupabaseApiService::class.java)
}
