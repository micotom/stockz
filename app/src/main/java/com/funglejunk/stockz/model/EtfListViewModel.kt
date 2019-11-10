package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraEtf
import com.funglejunk.stockz.repo.db.XetraEtfBenchmark
import com.funglejunk.stockz.repo.db.XetraEtfPublisher
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class EtfListViewModel(dbInflater: XetraMasterDataInflater) : ViewModel() {

    private val db = XetraDb.get()
    private val disposables: CompositeDisposable = CompositeDisposable()

    val etfData: LiveData<List<XetraEtfFlattened>> = MutableLiveData()

    init {
        loadEtfs(dbInflater)
    }

    private fun loadEtfs(dbInflater: XetraMasterDataInflater) {
        dbInflater.init()
            .doOnEvent {
                Timber.d("db inflation complete.")
            }
            .toSingleDefault(true)
            .flatMap {
                db.etfDao().getAll().flattenInfo()
            }
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    Timber.d("posting new etf data: ${it.size} entries")
                    etfData.mutable().postValue(it)
                },
                { e -> Timber.e(e) }
            ).addTo(disposables)
    }

    fun searchDbFor(query: UiEtfQuery) {

        UiQueryDbInteractor(db).createSqlQuery(query).flattenInfo()
            .doOnEvent { data, _ ->
                Timber.d("received ${data.size} results from search")
            }
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    etfData.mutable().postValue(it)
                },
                { e -> Timber.e(e) }
            ).addTo(disposables)
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }

    // TODO replace with proper join query
    private fun Single<List<XetraEtf>>.flattenInfo() =
        flatMap {
            Observable.fromIterable(it)
                .flatMapSingle { etf ->
                    db.benchmarkDao().getBenchmarkById(etf.benchmarkId).zipWith(
                        db.publisherDao().getPublisherById(etf.publisherId),
                        BiFunction<XetraEtfBenchmark, XetraEtfPublisher, XetraEtfFlattened> { benchmark, publisher ->
                            XetraEtfFlattened(
                                name = etf.name,
                                isin = etf.isin,
                                symbol = etf.symbol,
                                listingDate = etf.listingDate,
                                ter = etf.ter,
                                profitUse = etf.profitUse,
                                replicationMethod = etf.replicationMethod,
                                fundCurrency = etf.fundCurrency,
                                tradingCurrency = etf.tradingCurrency,
                                publisherName = publisher.name,
                                benchmarkName = benchmark.name
                            )
                        }
                    )
                }.toList()
        }

    private fun Disposable.addTo(compositeDisposable: CompositeDisposable) =
        compositeDisposable.add(this)

}