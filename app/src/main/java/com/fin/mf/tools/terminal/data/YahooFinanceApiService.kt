package com.fin.mf.tools.terminal.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit interface for Yahoo Finance Chart and Autocomplete APIs.
 */
interface YahooFinanceApiService {

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("range") range: String,
        @Query("interval") interval: String
    ): Response<YahooChartResponse>

    @GET
    suspend fun searchAssets(
        @Url url: String = "https://query2.finance.yahoo.com/v1/finance/search",
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 10,
        @Query("newsCount") newsCount: Int = 0
    ): Response<YahooSearchResponse>
}
