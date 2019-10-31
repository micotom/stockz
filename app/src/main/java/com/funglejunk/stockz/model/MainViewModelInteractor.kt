package com.funglejunk.stockz.model

import android.annotation.SuppressLint
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.dboerse.DeutscheBoerseRemoteRepo
import io.reactivex.Single
import java.text.SimpleDateFormat
import java.util.*

@Deprecated("Not used")
class MainViewModelInteractor {

    private companion object {
        @SuppressLint("SimpleDateFormat")
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
    }

    /*
    fun fetchInfoData(symbol: String): Single<InfoData.Data> {
        return repo.getInfo(symbol)
            .flatMap {
                it.fold(
                    { e -> Single.error<InfoData.Data>(e) },
                    { data -> Single.just(data.data[0]) }
                )
            }
    }
     */

    fun fetchHistoryData(from: Date? = null, to: Date? = null, ticker: String): Single<DrawableHistoricData> {

        val deutscheBoerseRepoInteractor = DeutscheBoerseRepoInteractor(XetraDb.get(), DeutscheBoerseRemoteRepo())
        return deutscheBoerseRepoInteractor.getData(ticker)
            .map {
                it.map {
                    ChartValue(dateFormatter.parse(it.date), it.value.toFloat())
                }
            }
            .map {
                DrawableHistoricData(it)
            }
        /*
        return repo.getHistoricData(from, to, ticker)
            .map {
                it.flatMap {
                    Try.invoke {
                        it.history.map { (dateStr, value) ->
                            val date = dateFormatter.parse(dateStr)
                            ChartValue(date!!, value.close.toFloat())
                        }
                    }
                }
            }
            .flatMap {
                it.fold(
                    { e -> Single.error<List<ChartValue>>(e) },
                    { data -> Single.just(data) }
                )
            }
            .map {
                DrawableHistoricData(it)
            }
            */
    }

}