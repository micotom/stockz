package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.dispatchers.dispatchers
import com.funglejunk.stockz.addTo
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.RxSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class EtfListViewModel(
    dbInflater: XetraMasterDataInflater,
    val db: XetraDbInterface,
    val schedulers: RxSchedulers
) : ViewModel() {

    companion object {
        private val background = IO.dispatchers().io()
        private val main = IO.dispatchers().default()
    }

    private val disposables: CompositeDisposable = CompositeDisposable()
    private val queryInteractor = UiQueryDbInteractor()

    val etfData: LiveData<List<Etf>> = MutableLiveData()

    init {
        loadEtfs(dbInflater)
    }

    private fun loadEtfs(dbInflater: XetraMasterDataInflater) {
        IO.fx {
            continueOn(background)
            dbInflater.init().bind().fold(
                { e ->
                    continueOn(main)
                    Timber.e("Error inflating db: $e")
                },
                { _ ->
                    Timber.d("db inflation complete.")
                    val etfs = effect {
                        db.etfFlattenedDao().getAll()
                    }.bind()
                    continueOn(main)
                    etfData.mutable().postValue(etfs)
                }
            )
        }.unsafeRunSync()
    }

    fun searchDbFor(query: UiEtfQuery) {
        queryInteractor.executeSqlString(queryInteractor.buildSqlStringFrom(query), db)
            .subscribeOn(schedulers.ioScheduler)
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
