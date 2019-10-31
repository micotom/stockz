package com.funglejunk.stockz.model

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.data.fboerse.FBoerseData
import com.funglejunk.stockz.data.wtd.InfoData
import com.funglejunk.stockz.repo.AssetReader
import com.funglejunk.stockz.repo.XetraSymbols
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.repo.wdd.WtdRepo
import com.funglejunk.stockz.util.RxSchedulers
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class EtfDetailViewModel(
    repo: WtdRepo, private val assetReader: AssetReader, private val schedulers: RxSchedulers,
    private val xetraSymbolsReader: XetraSymbols
) :
    ViewModel() {

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

    val chartData: LiveData<Array<String>> = MutableLiveData()
    private val mutableChartData = chartData as MutableLiveData

    val infoData: LiveData<InfoData.Data> = MutableLiveData()
    private val mutableInfoData = infoData as MutableLiveData

    private val interactor = MainViewModelInteractor(repo)
    private val disposables: CompositeDisposable = CompositeDisposable()

    fun fetchFboerseHistoy(
        isin: String, fromDate: LocalDate = LocalDate.of(2010, 1, 1),
        toDate: LocalDate = LocalDate.now()
    ) {
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
        }.subscribeOn(Schedulers.io()).subscribe(
            { drawableDate -> mutableViewStateData.postValue(ViewState.NewChartData(drawableDate)) },
            { e -> Timber.e(e) }
        ).addTo(disposables)
    }

    fun fetchTickers() {
        /*
        assetReader.getTickerSymbols()
            .subscribeOn(schedulers.ioScheduler)
            .subscribe(
                { tickers -> mutableChartData.postValue(tickers) },
                { e -> mutableViewStateData.postValue(ViewState.Error(e)) }
            ).addTo(disposables)
            */
        xetraSymbolsReader.get()
            .map {
                Timber.d("received all symbols")
                it.map {
                    listOf(it.instrument, it.isin, it.symbol, it.wkn)
                }
            }
            .map {
                it.flatten()
            }
            .map {
                it.toTypedArray()
            }
            .subscribeOn(schedulers.ioScheduler)
            .subscribe(
                { tickers -> mutableChartData.postValue(tickers) },
                { e -> mutableViewStateData.postValue(ViewState.Error(e)) }
            ).addTo(disposables)
    }

    fun fetchInfo(symbol: String) {
        mutableViewStateData.postValue(ViewState.Loading)
        interactor.fetchInfoData(symbol)
            .subscribeOn(schedulers.ioScheduler)
            .subscribe(
                { data ->
                    mutableInfoData.postValue(data)
                },
                { e -> mutableViewStateData.postValue(ViewState.Error(e)) }
            ).addTo(disposables)
    }

    fun fetchHistory(from: Date? = null, to: Date? = null, ticker: String) {
        mutableViewStateData.postValue(ViewState.Loading)
        interactor.fetchHistoryData(from, to, ticker)
            .subscribeOn(schedulers.ioScheduler)
            .subscribe(
                { drawableData ->
                    mutableViewStateData.postValue(ViewState.NewChartData(drawableData))
                },
                { e ->
                    mutableViewStateData.postValue(ViewState.Error(e))
                }
            ).addTo(disposables)
    }

    fun getIsinForTerm(term: String) = xetraSymbolsReader.findEntryForTerm(term).isin

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }

    private fun Disposable.addTo(compositeDisposable: CompositeDisposable) =
        compositeDisposable.add(this)

}