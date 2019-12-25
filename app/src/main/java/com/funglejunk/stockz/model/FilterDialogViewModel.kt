package com.funglejunk.stockz.model

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.FViewModel
import timber.log.Timber

class FilterDialogViewModel(private val db: XetraDbInterface) : FViewModel() {

    val benchmarkNamesLiveData: LiveData<List<String>> = MutableLiveData()
    val publisherNamesLiveData: LiveData<List<String>> = MutableLiveData()
    val profitUseLiveData: LiveData<List<String>> = MutableLiveData()
    val replicationLiveData: LiveData<List<String>> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    init {
        initBenchmarks()
        initPublishers()
    }

    @SuppressLint("DefaultLocale")
    fun onQueryParamsUpdate(temporaryQuery: UiEtfQuery) {
        val action = IO.fx {
            val sqlQueryString = queryInteractor.buildSqlStringFrom(temporaryQuery)
            val qResult = queryInteractor.executeSqlString(sqlQueryString, db).bind()
            qResult.map { etfs ->
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
        }
        runIO(
            action,
            { e -> Timber.e(e) },
            { (publishers, benchmarks, profitUses,
                  replicationMethods) ->
                publisherNamesLiveData.mutable().postValue(publishers.toList())
                benchmarkNamesLiveData.mutable().postValue(benchmarks.toList())
                profitUseLiveData.mutable().postValue(profitUses.toList())
                replicationLiveData.mutable().postValue(replicationMethods.toList())
            }
        )
    }

    @SuppressLint("DefaultLocale")
    private fun initBenchmarks() {
        val action = IO.fx {
            effect {
                Either.catch {
                    db.benchmarkDao().getAll().map {
                        it.name
                    }.sortedBy { it.toUpperCase() }
                }
            }.bind()
        }
        runIO(
            action,
            { e -> Timber.e(e) },
            { benchmarks -> benchmarkNamesLiveData.mutable().postValue(benchmarks) }
        )
    }

    @SuppressLint("DefaultLocale")
    private fun initPublishers() {
        val action = IO.fx {
            effect {
                Either.catch {
                    db.publisherDao().getAll().map {
                        it.name
                    }.sortedBy { it.toUpperCase() }
                }
            }.bind()
        }
        runIO(
            action,
            { e -> Timber.e(e) },
            { publishers -> publisherNamesLiveData.mutable().postValue(publishers) }
        )
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
