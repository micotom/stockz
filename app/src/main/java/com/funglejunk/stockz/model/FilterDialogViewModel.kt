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
import com.funglejunk.stockz.repo.db.XetraEtfBenchmark
import com.funglejunk.stockz.util.FViewModel

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
        val sqlQueryString = queryInteractor.buildSqlStringFrom(temporaryQuery)
        val action = IO.fx {
            val qResult = queryInteractor.executeSqlString(sqlQueryString, db).bind()
            qResult.map { etfs ->
                val publishers =
                    etfs.map { it.publisherName }.alphSorted().prependPlaceholder()
                val benchmarks =
                    etfs.map { it.benchmarkName }.alphSorted().prependPlaceholder()
                val profitUses =
                    etfs.map { it.profitUse }.alphSorted().prependPlaceholder()
                val replicationMethods =
                    etfs.map { it.replicationMethod }.alphSorted().prependPlaceholder()
                FilteredUiParams(
                    publishers = publishers,
                    benchmarks = benchmarks,
                    profitUses = profitUses,
                    replicationMethods = replicationMethods
                )
            }
        }
        runIO(
            io = action,
            onSuccess = { (publishers, benchmarks, profitUses,
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
                    }.alphSorted()
                }
            }.bind()
        }
        runIO(
            io = action,
            onSuccess = { benchmarks -> benchmarkNamesLiveData.mutable().postValue(benchmarks) }
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
            io = action,
            onSuccess = { publishers -> publisherNamesLiveData.mutable().postValue(publishers) }
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

    private fun <T> Collection<T>.prepend(element: T) = mutableListOf<T>().apply {
        add(element)
        addAll(this@prepend)
    }

    @SuppressLint("DefaultLocale")
    private fun List<String>.alphSorted() = toSet().sortedBy { it.toUpperCase() }

    private fun List<String>.prependPlaceholder() = prepend(UiEtfQuery.ALL_PLACEHOLDER)
}
