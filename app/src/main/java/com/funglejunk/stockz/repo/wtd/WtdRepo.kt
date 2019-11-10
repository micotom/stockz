package com.funglejunk.stockz.repo.wtd

import arrow.core.Try
import com.funglejunk.stockz.data.wtd.HistoryData
import com.funglejunk.stockz.data.wtd.InfoData
import io.reactivex.Single
import java.util.Date

@Deprecated("Worldtradingdata API is deprecated")
interface WtdRepo {
    fun getHistoricData(from: Date? = null, to: Date? = null, vararg tickers: String):
            Single<Try<HistoryData>>
    fun getInfo(ticker: String): Single<Try<InfoData>>
}
