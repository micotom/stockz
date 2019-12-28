package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import arrow.core.extensions.fx
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.handleErrorWith
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.FViewModel
import com.funglejunk.stockz.util.logError

typealias EtfRetrievalResult = Either<Throwable, List<Etf>>

class EtfListViewModel(
    dbInflater: XetraMasterDataInflater,
    val db: XetraDbInterface
) : FViewModel() {

    val etfData: LiveData<List<Etf>> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    private val onEtfDataRetrieved: IO<(List<Etf>) -> Unit> = IO.just { data ->
        etfData.mutable().postValue(data)
    }

    private val loadEtfAction: (XetraMasterDataInflater) -> IO<EtfRetrievalResult> =
        { dbInflater: XetraMasterDataInflater ->
                dbInflater.init().flatMap {
                    IO.fx {
                        effect {
                            db.etfFlattenedDao().getAll()
                        }.map {
                            Either.right(it)
                        }.bind()
                    }
                }
        }

    private val searchDbIo: (UiEtfQuery) -> IO<EtfRetrievalResult> = { query ->
        val sqlQueryString = queryInteractor.buildSqlStringFrom(query)
        queryInteractor.executeSqlString(sqlQueryString, db)
    }

    init {
        runIO(
            io = loadEtfAction(dbInflater),
            onSuccess = onEtfDataRetrieved
        )
    }

    fun searchDbFor(query: UiEtfQuery) {
        runIO(
            io = searchDbIo(query),
            onSuccess = onEtfDataRetrieved
        )
    }

}
