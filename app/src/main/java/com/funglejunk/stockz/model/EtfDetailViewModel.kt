package com.funglejunk.stockz.model

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoerseData
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.toLocalDate
import com.funglejunk.stockz.util.RxSchedulers
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.text.SimpleDateFormat
import java.time.LocalDate
import timber.log.Timber

class EtfDetailViewModel(
    private val schedulers: RxSchedulers,
    private val fBoerseRepo: FBoerseRepo
) : ViewModel() {

    sealed class ViewState {
        object Loading : ViewState()
        data class Error(val error: Throwable) : ViewState()
        data class NewChartData(val drawableHistoricValues: DrawableHistoricData) : ViewState()
    }

    val viewStateData: LiveData<ViewState> = MutableLiveData()

    private var etfArg: Etf? = null

    private val disposables: CompositeDisposable = CompositeDisposable()

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
        viewStateData.mutable().postValue(ViewState.Loading)
        fBoerseRepo.getHistory(isin, fromDate, toDate).flatMap {
            it.fold(
                { e -> Single.error<FBoerseData>(e) },
                { Single.just(it) }
            )
        }.map {
            it.content.map { dayData ->
                ChartValue(dayData.date.toLocalDate(), dayData.close.toFloat())
            }
        }.map {
            DrawableHistoricData(it)
        }.subscribeOn(schedulers.ioScheduler).subscribe(
            { drawableDate -> viewStateData.mutable().postValue(ViewState.NewChartData(drawableDate)) },
            { e ->
                Timber.e(e)
                viewStateData.mutable().postValue(ViewState.Error(e))
            }
        ).addTo(disposables)
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }

    private fun Disposable.addTo(compositeDisposable: CompositeDisposable) =
        compositeDisposable.add(this)
}
