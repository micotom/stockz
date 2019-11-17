package com.funglejunk.stockz.repo.fboerse

import arrow.core.Either
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rxResponseString
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
class FBoerseRepoImpl : FBoerseRepo {

    private companion object {
        const val BASE_URL = "https://api.boerse-frankfurt.de/data"
        const val PRICE_HISTORY_EP = "/price_history"
        const val PERFORMANCE_EP = "/performance"
        const val ISIN_PARAM_ID = "isin"
        const val MIN_DATE_ID = "minDate"
        const val MAX_DATE_ID = "maxDate"
        val OFFSET_PARAM = "offset" to 0
        val LIMIT_PARAM = "limit" to 505
        val MIC_PARAM = "mic" to "XETR"
    }

    override fun getHistory(isin: String, minDate: LocalDate, maxDate: LocalDate):
            Single<Either<Throwable, FBoerseHistoryData>> {
        return (BASE_URL + PRICE_HISTORY_EP).httpGet(
            listOf(
                OFFSET_PARAM,
                LIMIT_PARAM,
                MIC_PARAM,
                ISIN_PARAM_ID to isin,
                MIN_DATE_ID to minDate,
                MAX_DATE_ID to maxDate
            )
        ).rxResponseString().map { response ->
            runBlocking {
                parseFBoerseDataString(response)
                    .map { data ->
                        data.copy(content = data.content.sortedBy { it.date })
                    }
            }
        }
    }

    private suspend fun parseFBoerseDataString(content: String) = Either.catch {
        Json.nonstrict.parse(FBoerseHistoryData.serializer(), content)
    }

    override fun getHistoryPerfData(isin: String): Single<Either<Throwable, FBoersePerfData>> {
        return (BASE_URL + PERFORMANCE_EP).httpGet(
            listOf(
                MIC_PARAM,
                ISIN_PARAM_ID to isin
            )
        ).rxResponseString().map { response ->
            runBlocking {
                parseFBoersePerfString(response)
            }
        }
    }

    private suspend fun parseFBoersePerfString(content: String) = Either.catch {
        Json.nonstrict.parse(FBoersePerfData.serializer(), content)
    }

    /*
    All endpoints reverse engineered
    https://api.boerse-frankfurt.de/data/bid_ask_overview?isin=IE00BKX55T58&mic=XETR
    https://api.boerse-frankfurt.de/data/price_information?isin=DE0008469008&mic=XETR
    https://api.boerse-frankfurt.de/data/quote_box?isin=IE00BKX55T58&mic=XETR *
    https://api.boerse-frankfurt.de/data/xetra_trading_parameter?isin=IE00BKX55T58
    https://api.boerse-frankfurt.de/data/performance?isin=IE00BKX55T58&mic=XETR *
    https://api.boerse-frankfurt.de/data/etp_master_data?isin=IE00BKX55T58
    https://api.boerse-frankfurt.de/data/asset_under_management?isin=IE00BKX55T58
    https://api.boerse-frankfurt.de/data/investment_focus?isin=IE00BKX55T58
    https://api.boerse-frankfurt.de/data/fees_etp?isin=IE00BKX55T58
    https://api.boerse-frankfurt.de/data/benchmark?isin=IE00BKX55T58
    https://api.boerse-frankfurt.de/data/data_sheet_header?isin=IE00BKX55T58
    https://api.boerse-frankfurt.de/data/instrument_information?slug=ishares-msci-em-latin-america-ucits-etf-usd-dist&instrumentType=ETP
     */

    // STOXX Europe 50 Index

}
