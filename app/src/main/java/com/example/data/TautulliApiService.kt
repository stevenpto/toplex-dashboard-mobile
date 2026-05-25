package com.example.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TautulliApiService {

    @GET("api/v2")
    suspend fun getActivity(
        @Query("apikey") apiKey: String,
        @Query("cmd") cmd: String = "get_activity"
    ): TautulliResponse<TautulliSessions>

    @GET("api/v2")
    suspend fun getHistory(
        @Query("apikey") apiKey: String,
        @Query("cmd") cmd: String = "get_history",
        @Query("length") length: Int = 50
    ): TautulliResponse<TautulliHistoryContainer>

    @GET("api/v2")
    suspend fun getServerFriendlyName(
        @Query("apikey") apiKey: String,
        @Query("cmd") cmd: String = "get_server_friendly_name"
    ): TautulliResponse<TautulliServerInfo>
}

object TautulliApiClient {
    private var currentUrl: String = ""
    private var cachedService: TautulliApiService? = null

    fun getService(baseUrl: String): TautulliApiService {
        var sanitizedUrl = baseUrl.trim()
        if (sanitizedUrl.isEmpty()) {
            sanitizedUrl = "http://localhost:8181" // fallback
        }
        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
            sanitizedUrl = "http://$sanitizedUrl"
        }
        if (!sanitizedUrl.endsWith("/")) {
            sanitizedUrl = "$sanitizedUrl/"
        }

        if (sanitizedUrl == currentUrl && cachedService != null) {
            return cachedService!!
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val service = retrofit.create(TautulliApiService::class.java)
        currentUrl = sanitizedUrl
        cachedService = service
        return service
    }
}
