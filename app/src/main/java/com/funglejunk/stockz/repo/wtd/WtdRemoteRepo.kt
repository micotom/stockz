package com.funglejunk.stockz.repo.wtd

import arrow.core.Try
import com.funglejunk.stockz.BuildConfig
import com.funglejunk.stockz.data.wtd.HistoryData
import com.funglejunk.stockz.data.wtd.InfoData
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rxResponseString
import io.reactivex.Single
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@Deprecated("Worldtradingdata API is deprecated")
class WtdRemoteRepo : WtdRepo {

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        private const val BASE_URL = "https://api.worldtradingdata.com/api/v1"
    }

    @UnstableDefault
    private val json = Json.nonstrict

    override fun getInfo(ticker: String): Single<Try<InfoData>> {
        return ("$BASE_URL/stock?symbol=$ticker&api_token=${BuildConfig.WDT_API_KEY}").httpGet()
            .rxResponseString()
            .map {
                Try.invoke {
                    json.parse(InfoData.serializer(), it)
                }
            }
    }

    override fun getHistoricData(from: Date?, to: Date?, vararg tickers: String): Single<Try<HistoryData>> {
        val tickersParam = tickers.joinToString(separator = ",", prefix = "", postfix = "")
        Timber.d("get historics for symbol(s): $tickersParam")
        val dateParam = when {
            from != null && to != null -> "&date_from=${dateFormat.format(from)}&date_to=${dateFormat.format(to)}"
            from != null -> "&date_from=${dateFormat.format(from)}"
            to != null -> "&date_to=${dateFormat.format(to)}"
            else -> ""
        }
        return ("$BASE_URL/history?symbol=$tickersParam" +
                "$dateParam&sort=asc&api_token=${BuildConfig.WDT_API_KEY}").httpGet()
            .rxResponseString()
            .map {
                Try.invoke { json.parse(HistoryData.serializer(), it) }
            }
    }

}