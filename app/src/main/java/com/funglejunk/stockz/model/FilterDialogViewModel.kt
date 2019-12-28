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

typealias SearchResult = List<String>

class FilterDialogViewModel(private val db: XetraDbInterface) : FViewModel() {

    val benchmarkNamesLiveData: LiveData<SearchResult> = MutableLiveData()
    val publisherNamesLiveData: LiveData<SearchResult> = MutableLiveData()
    val profitUseLiveData: LiveData<SearchResult> = MutableLiveData()
    val replicationLiveData: LiveData<SearchResult> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    private val searchAction: (UiEtfQuery) -> IO<Either<Throwable, FilteredUiParams>> =
        { temporaryQuery ->
            val sqlQueryString = queryInteractor.buildSqlStringFrom(temporaryQuery)
            IO.fx {
                val queryResult = queryInteractor.executeSqlString(sqlQueryString, db).bind()
                queryResult.map {
                    FilteredUiParams(
                        publishers = it.map { it.publisherName }.sortedWithPlaceholder(),
                        benchmarks = it.map { it.benchmarkName }.sortedWithPlaceholder(),
                        profitUses = it.map { it.profitUse }.sortedWithPlaceholder(),
                        replicationMethods = it.map { it.replicationMethod }.sortedWithPlaceholder()
                    )
                }
            }
        }

    private val onSearchResult: IO<(FilteredUiParams) -> Unit> =
        IO.just { (publishers, benchmarks, profitUses, replicationMethods) ->
            publisherNamesLiveData.mutable().postValue(publishers)
            benchmarkNamesLiveData.mutable().postValue(benchmarks)
            profitUseLiveData.mutable().postValue(profitUses)
            replicationLiveData.mutable().postValue(replicationMethods)
        }

    private val initBenchmarkAction: () -> IO<Either<Throwable, SearchResult>> = {
        IO.fx {
            effect {
                Either.catch {
                    db.benchmarkDao().getAll().map {
                        it.name
                    }.alphSorted()
                }
            }.bind()
        }
    }

    private val initPublishersAction: () -> IO<Either<Throwable, SearchResult>> = {
        IO.fx {
            effect {
                Either.catch {
                    db.publisherDao().getAll().map {
                        it.name
                    }.alphSorted()
                }
            }.bind()
        }
    }

    init {
        runIO(
            io = initBenchmarkAction(),
            onSuccess = IO.just { _ -> Unit }
        )
        runIO(
            io = initPublishersAction(),
            onSuccess = IO.just { _ -> Unit }
        )
        /*
        initBenchmarks()
        initPublishers()
         */
    }

    fun onQueryParamsUpdate(temporaryQuery: UiEtfQuery) {
        runIO(
            io = searchAction(temporaryQuery),
            onSuccess = onSearchResult
        )
    }

    /*

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

     */

    private data class FilteredUiParams(
        val publishers: SearchResult,
        val benchmarks: SearchResult,
        val profitUses: SearchResult,
        val replicationMethods: SearchResult
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
    private fun SearchResult.alphSorted() = toSet().sortedBy { it.toUpperCase() }

    private fun SearchResult.prependPlaceholder() = prepend(UiEtfQuery.ALL_PLACEHOLDER)

    private fun SearchResult.sortedWithPlaceholder() = alphSorted().prependPlaceholder()

}
