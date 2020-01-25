package com.funglejunk.stockz.repo.fboerse

import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import com.github.kittinunf.fuel.coroutines.awaitObject
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.LocalDate

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

    override suspend fun getHistory(
        isin: String,
        minDate: LocalDate,
        maxDate: LocalDate
    ): FBoerseHistoryData = (BASE_URL + PRICE_HISTORY_EP).httpGet(
            listOf(
                OFFSET_PARAM,
                LIMIT_PARAM,
                MIC_PARAM,
                ISIN_PARAM_ID to isin,
                MIN_DATE_ID to minDate,
                MAX_DATE_ID to maxDate
            )
        ).awaitObject(kotlinxDeserializerOf(FBoerseHistoryData.serializer()))

    override suspend fun getHistoryPerfData(isin: String): FBoersePerfData =
        (BASE_URL + PERFORMANCE_EP).httpGet(
            listOf(
                MIC_PARAM,
                ISIN_PARAM_ID to isin
            )
        ).awaitObject(kotlinxDeserializerOf(FBoersePerfData.serializer()))

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

}
// https://query1.finance.yahoo.com/v7/finance/quote?symbols=VFEM.de