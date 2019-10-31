package com.funglejunk.stockz.repo.dboerse

import arrow.core.Try
import com.funglejunk.stockz.BuildConfig
import com.funglejunk.stockz.data.dboerse.DeutscheBoerseDayData
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rxResponseString
import io.reactivex.Single
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import timber.log.Timber
import java.time.LocalDate

@Deprecated("Deutsche Boerse API is deprecated")
class DeutscheBoerseRemoteRepo : DeutscheBoerseRepo {

    private companion object {
        const val BASE_URL = "https://api.developer.deutsche-boerse.com/prod/xetra-public-data-set/1.0.0/xetra"
        const val API_KEY_PARAM_NAME = "X-DBP-APIKEY"
        const val API_KEY_VALUE = BuildConfig.XETRA_API_KEY
        const val ISIN_PARAM_NAME = "isin"
        const val DATE_PARAM_NAME = "date"
    }

    override fun getCloseValueFor(isin: String, date: LocalDate): Single<DeutscheBoerseDayData> {
        Timber.d("remote call for $isin on $date")
        return Single.fromCallable {
            BASE_URL.httpGet(
                listOf(ISIN_PARAM_NAME to isin, DATE_PARAM_NAME to "$date")
            ).header(
                mapOf(API_KEY_PARAM_NAME to API_KEY_VALUE)
            )
        }.flatMap { req ->
            req.rxResponseString()
        }.map { response ->
            Try.invoke {
                Json.nonstrict.parse(DeutscheBoerseDayData.serializer().list, response)
            }.fold(
                { _ -> listOf(DeutscheBoerseDayData.INVALID) },
                { data -> data }
            )
        }.map { dayData->
            dayData.maxBy {
                it.time
            } ?: DeutscheBoerseDayData.INVALID
        }
    }

}