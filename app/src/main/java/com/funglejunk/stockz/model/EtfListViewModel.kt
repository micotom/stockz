package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.EtfList
import com.funglejunk.stockz.util.FViewModel

class EtfListViewModel(
    dbInflater: XetraMasterDataInflater,
    val db: XetraDbInterface
) : FViewModel() {

    val etfData: LiveData<EtfList> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    private val onEtfDataRetrieved: IO<(EtfList) -> Unit> = IO.just { data ->
        etfData.mutable().postValue(data)
    }

    private val loadEtfAction: (XetraMasterDataInflater) -> IO<EtfList> =
        { dbInflater: XetraMasterDataInflater ->
            dbInflater.init().flatMap {
                IO.fx {
                    effect {
                        db.etfFlattenedDao().getAll()
                    }.bind()
                }
            }
        }

    private val searchDbIo: (UiEtfQuery) -> IO<EtfList> = { query ->
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
