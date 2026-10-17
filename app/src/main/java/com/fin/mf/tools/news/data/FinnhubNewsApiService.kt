package com.fin.mf.tools.news.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit interface for the Finnhub Stock & Financial News API (https://finnhub.io/).
 */
interface FinnhubNewsApiService {

    /**
     * Fetches major market headlines by category (e.g. general, forex, crypto, merger).
     * Endpoint: `GET /api/v1/news?category={category}&minId={minId}`
     */
    @GET("api/v1/news")
    suspend fun getMarketNews(
        @Query("category") category: String,
        @Query("minId") minId: Long = 0L
    ): Response<List<FinnhubArticle>>

    /**
     * Fetches company-specific news articles for look-through equity tracking.
     * Endpoint: `GET /api/v1/company-news?symbol={symbol}&from={YYYY-MM-DD}&to={YYYY-MM-DD}`
     */
    @GET("api/v1/company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") fromDate: String,
        @Query("to") toDate: String
    ): Response<List<FinnhubArticle>>

    /**
     * Fetches news sentiment buzz and bullish/bearish scores for a given equity ticker.
     * Endpoint: `GET /api/v1/news-sentiment?symbol={symbol}`
     */
    @GET("api/v1/news-sentiment")
    suspend fun getNewsSentiment(
        @Query("symbol") symbol: String
    ): Response<SentimentResponse>
}

/**
 * Network factory and client configuration for Finnhub API requests.
 */
object FinnhubClient {

    private const val BASE_URL = "https://finnhub.io/"
    const val FINNHUB_API_KEY = "db9k91pr01qjv8c600e0db9k91pr01qjv8c600eg"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val originalHttpUrl = original.url

        // Append token query param if missing and set header
        val url = originalHttpUrl.newBuilder()
            .apply {
                if (originalHttpUrl.queryParameter("token") == null) {
                    addQueryParameter("token", FINNHUB_API_KEY)
                }
            }
            .build()

        val request = original.newBuilder()
            .url(url)
            .header("X-Finnhub-Token", FINNHUB_API_KEY)
            .header("Accept", "application/json")
            .build()

        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val service: FinnhubNewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FinnhubNewsApiService::class.java)
    }
}
