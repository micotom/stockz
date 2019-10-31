package com.funglejunk.stockz.model

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.data.fboerse.FBoerseData
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.util.RxSchedulers
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.LocalDate

class EtfDetailViewModel(private val schedulers: RxSchedulers) : ViewModel() {

    private companion object {
        @SuppressLint("SimpleDateFormat")
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
    }

    sealed class ViewState {
        object Loading : ViewState()
        data class Error(val error: Throwable) : ViewState()
        data class NewChartData(val drawableHistoricValues: DrawableHistoricData) : ViewState()
    }

    val viewStateData: LiveData<ViewState> = MutableLiveData()
    private val mutableViewStateData = viewStateData as MutableLiveData

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun fetchFboerseHistoy(
        isin: String, fromDate: LocalDate = LocalDate.of(2010, 1, 1),
        toDate: LocalDate = LocalDate.now()
    ) {
        mutableViewStateData.postValue(ViewState.Loading)
        FBoerseRepo().getHistory(isin, fromDate, toDate).flatMap {
            it.fold(
                { e -> Single.error<FBoerseData>(e) },
                { Single.just(it) }
            )
        }.map {
            it.content.map { dayData ->
                ChartValue(dateFormatter.parse(dayData.date), dayData.close.toFloat())
            }
        }.map {
            DrawableHistoricData(it)
        }.subscribeOn(schedulers.ioScheduler).subscribe(
            { drawableDate -> mutableViewStateData.postValue(ViewState.NewChartData(drawableDate)) },
            { e ->
                Timber.e(e)
                mutableViewStateData.postValue(ViewState.Error(e))
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