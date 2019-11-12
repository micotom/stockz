package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.stockz.addTo
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.util.RxSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class FilterDialogViewModel(
    private val schedulers: RxSchedulers,
    private val db: XetraDb
) : ViewModel() {

    val benchmarkNamesLiveData: LiveData<List<String>> = MutableLiveData()
    val publisherNamesLiveData: LiveData<List<String>> = MutableLiveData()

    private val disposables = CompositeDisposable()

    init {
        Timber.d("view model init")
        getBenchmarks()
        getPublishers()
    }

    private fun getBenchmarks() {
        val dao = db.benchmarkDao()
        dao.getAll()
            .map {
                it.map { benchmark -> benchmark.name }
            }
            .map {
                it.sortedBy { name -> name.toUpperCase() }
            }
            .subscribeOn(schedulers.ioScheduler)
            .subscribe(
                { benchmarks -> benchmarkNamesLiveData.mutable().postValue(benchmarks) },
                { e -> Timber.e(e) }
            ).addTo(disposables)
    }

    private fun getPublishers() {
        val dao = db.publisherDao()
        dao.getAll()
            .map {
                it.map { publisher -> publisher.name }
            }
            .map {
                it.sortedBy { name -> name.toUpperCase() }
            }
            .subscribeOn(schedulers.ioScheduler)
            .subscribe(
                { publishers -> publisherNamesLiveData.mutable().postValue(publishers) },
                { e -> Timber.e(e) }
            ).addTo(disposables)
    }

    override fun onCleared() {
        Timber.d("view model cleared")
        disposables.clear()
        super.onCleared()
    }
}
