package com.funglejunk.stockz.repo.fboerse

import arrow.core.Try
import com.funglejunk.stockz.data.fboerse.FBoerseData
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rxResponseString
import io.reactivex.Single
import java.time.LocalDate
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
class FBoerseRepoImpl : FBoerseRepo {

    private companion object {
        const val BASE_URL = "https://api.boerse-frankfurt.de/data/price_history"
        const val ISIN_PARAM_ID = "isin"
        const val MIN_DATE_ID = "minDate"
        const val MAX_DATE_ID = "maxDate"
        val OFFSET_PARAM = "offset" to 0
        val LIMIT_PARAM = "limit" to 505
        val MIC_PARAM = "mic" to "XETR"
    }

    override fun getHistory(isin: String, minDate: LocalDate, maxDate: LocalDate): Single<Try<FBoerseData>> {
        return BASE_URL.httpGet(
            listOf(
                OFFSET_PARAM,
                LIMIT_PARAM,
                MIC_PARAM,
                ISIN_PARAM_ID to isin,
                MIN_DATE_ID to minDate,
                MAX_DATE_ID to maxDate
            )
        ).rxResponseString().map {
            Try.invoke {
                Json.nonstrict.parse(FBoerseData.serializer(), it)
            }.map {
                it.copy(content = it.content.sortedBy { it.date })
            }
        }
    }
}
