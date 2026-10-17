package com.fin.mf.data.engine

import android.content.Context
import com.fin.mf.network.NetworkClient
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Data model representing an official NSE / BSE exchange trading holiday.
 */
@JsonClass(generateAdapter = true)
data class TradingHoliday(
    @Json(name = "date") val date: String,
    @Json(name = "holidayName") val holidayName: String,
    @Json(name = "dayOfWeek") val dayOfWeek: String,
    @Json(name = "closedExchange") val closedExchange: String = "NSE / BSE"
)

/**
 * Live market status snapshot for Indian exchanges (NSE & BSE).
 */
data class MarketStatus(
    val isOpen: Boolean,
    val isPreMarket: Boolean,
    val statusTitle: String,
    val statusSubtitle: String,
    val nextHoliday: TradingHoliday?,
    val isHolidayToday: Boolean,
    val todayHolidayName: String?
)

/**
 * Institutional engine computing real-time NSE/BSE exchange status in IST (Asia/Kolkata),
 * tracking trading hours (09:15 - 15:30 IST), pre-market (09:00 - 09:15 IST), weekends,
 * and official exchange trading holidays.
 */
object MarketHolidayEngine {

    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = istTimeZone
    }
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US).apply {
        timeZone = istTimeZone
    }

    private val fallbackHolidays = listOf(
        TradingHoliday("2025-02-26", "Mahashivratri", "Wednesday"),
        TradingHoliday("2025-03-14", "Holi", "Friday"),
        TradingHoliday("2025-03-31", "Id-Ul-Fitr (Ramzan Id)", "Monday"),
        TradingHoliday("2025-04-10", "Mahavir Jayanti", "Thursday"),
        TradingHoliday("2025-04-14", "Dr. Ambedkar Jayanti", "Monday"),
        TradingHoliday("2025-04-18", "Good Friday", "Friday"),
        TradingHoliday("2025-05-01", "Maharashtra Day", "Thursday"),
        TradingHoliday("2025-08-15", "Independence Day", "Friday"),
        TradingHoliday("2025-08-27", "Ganesh Chaturthi", "Wednesday"),
        TradingHoliday("2025-10-02", "Mahatma Gandhi Jayanti / Dussehra", "Thursday"),
        TradingHoliday("2025-10-21", "Diwali Laxmi Pujan (Muhurat Trading)", "Tuesday"),
        TradingHoliday("2025-10-22", "Diwali Balipratipada", "Wednesday"),
        TradingHoliday("2025-11-05", "Guru Nanak Jayanti", "Wednesday"),
        TradingHoliday("2025-12-25", "Christmas", "Thursday"),
        TradingHoliday("2026-01-26", "Republic Day", "Monday"),
        TradingHoliday("2026-03-03", "Holi", "Tuesday"),
        TradingHoliday("2026-03-20", "Id-Ul-Fitr", "Friday"),
        TradingHoliday("2026-04-03", "Good Friday", "Friday"),
        TradingHoliday("2026-04-14", "Dr. Ambedkar Jayanti", "Tuesday"),
        TradingHoliday("2026-05-01", "Maharashtra Day", "Friday"),
        TradingHoliday("2026-08-15", "Independence Day", "Saturday"),
        TradingHoliday("2026-10-02", "Mahatma Gandhi Jayanti", "Friday"),
        TradingHoliday("2026-10-20", "Dussehra", "Tuesday"),
        TradingHoliday("2026-11-08", "Diwali Laxmi Pujan", "Sunday"),
        TradingHoliday("2026-11-10", "Diwali Balipratipada", "Tuesday"),
        TradingHoliday("2026-11-24", "Guru Nanak Jayanti", "Tuesday"),
        TradingHoliday("2026-12-25", "Christmas", "Friday")
    )

    private var cachedHolidays: List<TradingHoliday>? = null

    /**
     * Loads the trading holidays schedule, prioritizing local asset JSON with offline fallback.
     */
    fun getTradingHolidays(context: Context): List<TradingHoliday> {
        cachedHolidays?.let { return it }

        return try {
            val json = context.assets.open("trading_holidays_2025_2026.json").bufferedReader().use { it.readText() }
            val listType = Types.newParameterizedType(List::class.java, TradingHoliday::class.java)
            val adapter = NetworkClient.moshi.adapter<List<TradingHoliday>>(listType)
            val parsed = adapter.fromJson(json)
            if (!parsed.isNullOrEmpty()) {
                cachedHolidays = parsed
                parsed
            } else {
                fallbackHolidays
            }
        } catch (e: Exception) {
            fallbackHolidays
        }
    }

    /**
     * Computes real-time market open/closed status in IST (Indian Standard Time).
     *
     * Market Rules:
     * - Monday to Friday only.
     * - Pre-market session: 09:00 - 09:15 IST.
     * - Regular Trading session: 09:15 - 15:30 IST.
     * - Closed on Saturdays, Sundays, and official Trading Holidays.
     */
    fun computeMarketStatus(context: Context, nowMillis: Long = System.currentTimeMillis()): MarketStatus {
        val calendar = Calendar.getInstance(istTimeZone, Locale.US).apply {
            timeInMillis = nowMillis
        }

        val todayDateStr = dateFormat.format(Date(nowMillis))
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentMinutesOfDay = hour * 60 + minute

        val holidays = getTradingHolidays(context)
        val todayHoliday = holidays.firstOrNull { it.date == todayDateStr }
        val nextHoliday = holidays.firstOrNull { it.date > todayDateStr }

        // Weekend Check
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return MarketStatus(
                isOpen = false,
                isPreMarket = false,
                statusTitle = "MARKET CLOSED",
                statusSubtitle = "Weekend • Opens Monday 09:15 AM IST",
                nextHoliday = nextHoliday,
                isHolidayToday = false,
                todayHolidayName = null
            )
        }

        // Trading Holiday Check
        if (todayHoliday != null) {
            return MarketStatus(
                isOpen = false,
                isPreMarket = false,
                statusTitle = "EXCHANGE HOLIDAY",
                statusSubtitle = "${todayHoliday.holidayName} • NSE/BSE Closed",
                nextHoliday = nextHoliday,
                isHolidayToday = true,
                todayHolidayName = todayHoliday.holidayName
            )
        }

        // Time of Day Check (09:00 - 09:15 Pre-Market, 09:15 - 15:30 Regular)
        val preMarketStart = 9 * 60          // 09:00 AM
        val regularMarketStart = 9 * 60 + 15 // 09:15 AM
        val marketClose = 15 * 60 + 30       // 03:30 PM

        return when {
            currentMinutesOfDay in preMarketStart until regularMarketStart -> {
                MarketStatus(
                    isOpen = false,
                    isPreMarket = true,
                    statusTitle = "PRE-OPEN SESSION",
                    statusSubtitle = "09:00 - 09:15 IST • Regular opens at 09:15 AM",
                    nextHoliday = nextHoliday,
                    isHolidayToday = false,
                    todayHolidayName = null
                )
            }
            currentMinutesOfDay in regularMarketStart..marketClose -> {
                val minutesLeft = marketClose - currentMinutesOfDay
                val hoursLeft = minutesLeft / 60
                val minsRemaining = minutesLeft % 60
                val closesInText = if (hoursLeft > 0) "${hoursLeft}h ${minsRemaining}m" else "${minsRemaining}m"

                MarketStatus(
                    isOpen = true,
                    isPreMarket = false,
                    statusTitle = "LIVE NSE / BSE",
                    statusSubtitle = "Closes at 15:30 IST (in $closesInText)",
                    nextHoliday = nextHoliday,
                    isHolidayToday = false,
                    todayHolidayName = null
                )
            }
            currentMinutesOfDay < preMarketStart -> {
                MarketStatus(
                    isOpen = false,
                    isPreMarket = false,
                    statusTitle = "MARKET CLOSED",
                    statusSubtitle = "Opens today at 09:15 AM IST",
                    nextHoliday = nextHoliday,
                    isHolidayToday = false,
                    todayHolidayName = null
                )
            }
            else -> {
                val nextDayName = if (dayOfWeek == Calendar.FRIDAY) "Monday" else "Tomorrow"
                MarketStatus(
                    isOpen = false,
                    isPreMarket = false,
                    statusTitle = "MARKET CLOSED",
                    statusSubtitle = "Session ended at 15:30 • Opens $nextDayName 09:15 AM",
                    nextHoliday = nextHoliday,
                    isHolidayToday = false,
                    todayHolidayName = null
                )
            }
        }
    }
}
