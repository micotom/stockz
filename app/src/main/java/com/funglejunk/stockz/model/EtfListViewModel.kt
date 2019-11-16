package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.stockz.addTo
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraDbInterface
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class EtfListViewModel(dbInflater: XetraMasterDataInflater, val db: XetraDbInterface) : ViewModel() {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private val queryInteractor = UiQueryDbInteractor()

    val etfData: LiveData<List<Etf>> = MutableLiveData()

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
                db.etfFlattenedDao().getAll()
            }
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    etfData.mutable().postValue(it)
                },
                { e -> Timber.e(e) }
            ).addTo(disposables)
    }

    fun searchDbFor(query: UiEtfQuery) {
        queryInteractor.executeSqlString(queryInteractor.buildSqlStringFrom(query), db)
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
}
