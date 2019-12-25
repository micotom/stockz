package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.flatMap
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.*
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.util.FViewModel
import com.funglejunk.stockz.util.RxSchedulers
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.time.LocalDate
import timber.log.Timber

class EtfDetailViewModel(private val fBoerseRepo: FBoerseRepo) : FViewModel() {

    sealed class ViewState {
        object Loading : ViewState()
        data class Error(val error: Throwable) : ViewState()
        data class NewChartData(
            val drawableHistoricValues: DrawableHistoricData,
            val performanceData: FBoersePerfData
        ) : ViewState()
    }

    val viewStateData: LiveData<ViewState> = MutableLiveData()
    private var etfArg: Etf? = null

    fun setEtfArgs(etf: Etf) {
        val receivedNewEtfArg = null == etfArg || etfArg != etf
        if (receivedNewEtfArg) {
            fetchFboerseHistoy(etf.isin)
        }
        etfArg = etf.copy()
    }

    private fun fetchFboerseHistoy(
        isin: String,
        fromDate: LocalDate = LocalDate.of(2010, 1, 1),
        toDate: LocalDate = LocalDate.now()
    ) {
        IO.fx {
            viewStateData.mutable().postValue(ViewState.Loading)
        }.unsafeRunSync()

        val action = IO.fx {
            val chartData = effect {
                fBoerseRepo.getHistory(isin, fromDate, toDate).map {
                    it.content.map { dayData ->
                        ChartValue(dayData.date.toLocalDate(), dayData.close.toFloat())
                    }
                }.map {
                    DrawableHistoricData(it)
                }
            }.bind()
            val historyData = effect {
                fBoerseRepo.getHistoryPerfData(isin)
            }.bind()
            chartData.flattenWith(historyData)
        }

        runIO(
            action,
            { e ->
                Timber.e(e)
                viewStateData.mutable().postValue(ViewState.Error(e))
            },
            { (drawableData, perfData) ->
                viewStateData.mutable().postValue(ViewState.NewChartData(drawableData, perfData))
            }
        )
    }

    private fun <E, A, B> Either<E, A>.flattenWith(other: Either<E, B>) =
        Either.fx<E, Pair<A, B>> {
            val a = bind()
            val b = other.bind()
            a to b
        }

}
