package com.fin.mf.tools.terminal.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit interface for Yahoo Finance Chart, Autocomplete, and Fundamental Summary APIs.
 */
interface YahooFinanceApiService {

    @GET("v10/finance/quoteSummary/{symbol}")
    suspend fun getQuoteSummary(
        @Path("symbol") symbol: String,
        @Query("modules") modules: String = DEFAULT_QUOTE_SUMMARY_MODULES,
        @Header("User-Agent") userAgent: String = BROWSER_USER_AGENT
    ): Response<YahooQuoteSummaryResponse>

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("range") range: String,
        @Query("interval") interval: String
    ): Response<YahooChartResponse>

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChartData(
        @Path("symbol") symbol: String,
        @Query("range") range: String,
        @Query("interval") interval: String,
        @Query("includePrePost") includePrePost: Boolean = false,
        @Header("User-Agent") userAgent: String = BROWSER_USER_AGENT
    ): Response<YahooChartResponse>

    @GET
    suspend fun searchAssets(
        @Url url: String = "https://query2.finance.yahoo.com/v1/finance/search",
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 10,
        @Query("newsCount") newsCount: Int = 0
    ): Response<YahooSearchResponse>

    @GET("v1/finance/search")
    suspend fun searchSymbols(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 10,
        @Query("newsCount") newsCount: Int = 0,
        @Header("User-Agent") userAgent: String = BROWSER_USER_AGENT
    ): Response<YahooSearchResponse>

    companion object {
        const val BASE_URL = "https://query1.finance.yahoo.com/"
        const val DEFAULT_QUOTE_SUMMARY_MODULES =
            "summaryDetail,financialData,defaultKeyStatistics,calendarEvents"
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
}
