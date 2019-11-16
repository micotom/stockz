package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.funglejunk.stockz.addTo
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.RxSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class FilterDialogViewModel(
    private val schedulers: RxSchedulers,
    private val db: XetraDbInterface
) : ViewModel() {

    val benchmarkNamesLiveData: LiveData<List<String>> = MutableLiveData()
    val publisherNamesLiveData: LiveData<List<String>> = MutableLiveData()
    val profitUseLiveData: LiveData<List<String>> = MutableLiveData()
    val replicationLiveData: LiveData<List<String>> = MutableLiveData()

    private val disposables = CompositeDisposable()
    private val queryInteractor = UiQueryDbInteractor()

    init {
        initBenchmarks()
        initPublishers()
    }

    fun onQueryParamsUpdate(temporaryQuery: UiEtfQuery) {
        queryInteractor.executeSqlString(queryInteractor.buildSqlStringFrom(temporaryQuery), db)
            .map { etfs ->
                val publishers = etfs.map { it.publisherName }.toSet()
                    .sortedBy { it.toUpperCase() }
                val benchmarks = etfs.map { it.benchmarkName }.toSet()
                    .sortedBy { it.toUpperCase() }
                val profitUses = etfs.map { it.profitUse }.toSet()
                    .sortedBy { it.toUpperCase() }
                val replicationMethods = etfs.map { it.replicationMethod }.toSet()
                    .sortedBy { it.toUpperCase() }
                FilteredUiParams(
                    publishers = publishers.prepend(UiEtfQuery.ALL_PLACEHOLDER),
                    benchmarks = benchmarks.prepend(UiEtfQuery.ALL_PLACEHOLDER),
                    profitUses = profitUses.prepend(UiEtfQuery.ALL_PLACEHOLDER),
                    replicationMethods = replicationMethods.prepend(UiEtfQuery.ALL_PLACEHOLDER)
                )
            }
            .subscribeOn(schedulers.ioScheduler)
            .subscribe(
                { (publishers, benchmarks, profitUses,
                      replicationMethods) ->
                    publisherNamesLiveData.mutable().postValue(publishers.toList())
                    benchmarkNamesLiveData.mutable().postValue(benchmarks.toList())
                    profitUseLiveData.mutable().postValue(profitUses.toList())
                    replicationLiveData.mutable().postValue(replicationMethods.toList())
                },
                { e -> Timber.e(e) }
            ).addTo(disposables)
    }

    private fun initBenchmarks() {
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

    private fun initPublishers() {
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
        disposables.clear()
        super.onCleared()
    }

    private data class FilteredUiParams(
        val publishers: Collection<String>,
        val benchmarks: Collection<String>,
        val profitUses: Collection<String>,
        val replicationMethods: Collection<String>
    )

    private operator fun <T> List<T>.plus(other: T) =
        mutableListOf<T>().apply {
            addAll(this)
            add(other)
        }

    private operator fun <T> T.plus(collection: Collection<T>) =
        mutableListOf<T>().apply {
            add(this@plus)
            addAll(collection)
        }

    private fun <T> List<T>.prepend(element: T) = mutableListOf<T>().apply {
        add(element)
        addAll(this@prepend)
    }

}
