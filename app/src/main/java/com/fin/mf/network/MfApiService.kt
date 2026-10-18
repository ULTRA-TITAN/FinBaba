package com.fin.mf.network

import com.fin.mf.data.MfApiResponse
import com.fin.mf.data.MfSearchDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit REST interface for fetching real-time and historical Mutual Fund NAV data
 * and performing universal AMFI scheme search from the public AMFI wrapper endpoint: https://api.mfapi.in/
 */
interface MfApiService {

    /**
     * Searches for mutual fund schemes matching the given query string.
     * Endpoint: `https://api.mfapi.in/mf/search?q={query}`
     */
    @GET("mf/search")
    suspend fun searchSchemes(
        @Query("q") query: String
    ): Response<List<MfSearchDto>>

    /**
     * Fetches the complete master list of all AMFI mutual fund schemes for offline fuzzy reconciliation.
     * Endpoint: `https://api.mfapi.in/mf`
     */
    @GET("mf")
    suspend fun getAllSchemes(): Response<List<MfSearchDto>>

    /**
     * Fetches scheme metadata and full NAV history for a given AMFI scheme code.
     * The latest published NAV is always at index 0 of `response.body()?.data`.
     */
    @GET("mf/{schemeCode}")
    suspend fun getSchemeNavDetails(
        @Path("schemeCode") schemeCode: Int
    ): Response<MfApiResponse>

    /**
     * Fetches ONLY the latest NAV entry for a given AMFI scheme code (`mf/{schemeCode}/latest`).
     * Faster payload for parallel bulk portfolio refreshes across 20+ schemes.
     */
    @GET("mf/{schemeCode}/latest")
    suspend fun getSchemeLatestNav(
        @Path("schemeCode") schemeCode: Int
    ): Response<MfApiResponse>
}

/**
 * Singleton Network Module providing the shared Moshi instance, OkHttpClient,
 * and Retrofit service.
 */
object NetworkClient {

    private const val BASE_URL = "https://api.mfapi.in/"

    /**
     * Shared Moshi instance exposed so PortfolioRepository can reuse the exact same
     * reflection factory for reading/writing `portfolios_manifest.json` and `portfolio_{id}.json`.
     */
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val apiService: MfApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MfApiService::class.java)
    }
}