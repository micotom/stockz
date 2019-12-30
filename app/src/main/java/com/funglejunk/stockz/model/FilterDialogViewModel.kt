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

typealias SearchResult = List<String>

class FilterDialogViewModel(private val db: XetraDbInterface) : FViewModel() {

    val benchmarkNamesLiveData: LiveData<SearchResult> = MutableLiveData()
    val publisherNamesLiveData: LiveData<SearchResult> = MutableLiveData()
    val profitUseLiveData: LiveData<SearchResult> = MutableLiveData()
    val replicationLiveData: LiveData<SearchResult> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    private val searchAction: (UiEtfQuery) -> IO<FilteredUiParams> =
        { temporaryQuery ->
            val sqlQueryString = queryInteractor.buildSqlStringFrom(temporaryQuery)
            IO.fx {
                val queryResult = queryInteractor.executeSqlString(sqlQueryString, db).bind()
                FilteredUiParams(
                    publishers = queryResult.map { it.publisherName }.sortedWithPlaceholder(),
                    benchmarks = queryResult.map { it.benchmarkName }.sortedWithPlaceholder(),
                    profitUses = queryResult.map { it.profitUse }.sortedWithPlaceholder(),
                    replicationMethods = queryResult.map { it.replicationMethod }.sortedWithPlaceholder()
                )
            }
        }

    private val onSearchResult: IO<(FilteredUiParams) -> Unit> =
        IO.just { (publishers, benchmarks, profitUses, replicationMethods) ->
            publisherNamesLiveData.mutable().postValue(publishers)
            benchmarkNamesLiveData.mutable().postValue(benchmarks)
            profitUseLiveData.mutable().postValue(profitUses)
            replicationLiveData.mutable().postValue(replicationMethods)
        }

    private val initBenchmarkAction: () -> IO<SearchResult> = {
        IO.fx {
            effect {
                db.benchmarkDao().getAll().map {
                    it.name
                }.alphSorted()
            }.bind()
        }
    }

    private val initPublishersAction: () -> IO<SearchResult> = {
        IO.fx {
            effect {
                db.publisherDao().getAll().map {
                    it.name
                }.alphSorted()
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
    }

    fun onQueryParamsUpdate(temporaryQuery: UiEtfQuery) {
        runIO(
            io = searchAction(temporaryQuery),
            onSuccess = onSearchResult
        )
    }

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
