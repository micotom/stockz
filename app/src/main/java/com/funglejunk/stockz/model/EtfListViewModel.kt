package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.FViewModel
import timber.log.Timber

class EtfListViewModel(
    dbInflater: XetraMasterDataInflater,
    val db: XetraDbInterface
) : FViewModel() {

    val etfData: LiveData<List<Etf>> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    init {
        runIO(
            loadEtfs(dbInflater),
            { t -> Timber.e("Error inflating db: ${t.message}") },
            { data -> etfData.mutable().postValue(data) }
        )
    }

    private fun loadEtfs(dbInflater: XetraMasterDataInflater) =
        IO.fx {
            when (val inflateResult = dbInflater.init().bind()) {
                is Either.Right -> {
                    val etfs = effect {
                        db.etfFlattenedDao().getAll()
                    }.bind()
                    Either.right(etfs)
                }
                is Either.Left -> Either.left(inflateResult.a)
            }
        }

    fun searchDbFor(query: UiEtfQuery) {
        val action = {
            val sqlQueryString = queryInteractor.buildSqlStringFrom(query)
            queryInteractor.executeSqlString(sqlQueryString, db)
        }()
        runIO(
            action,
            { e -> Timber.e("Error searching db: $e") },
            { data -> etfData.mutable().postValue(data) }
        )
    }

}
