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

    sealed class ViewState {
        object Loading : ViewState()
        data class EtfData(val etfs: EtfList) : ViewState()
    }

    val viewStateData: LiveData<ViewState> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    private val showLoading: IO<Unit> = IO.fx {
        viewStateData.mutable().postValue(ViewState.Loading)
    }

    private val onEtfDataRetrieved: IO<(EtfList) -> Unit> = IO.just { data ->
        viewStateData.mutable().postValue(
            ViewState.EtfData(data)
        )
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
        val action = IO.fx {
            showLoading.bind()
            loadEtfAction(dbInflater).bind()
        }
        runIO(
            io = action,
            onSuccess = onEtfDataRetrieved
        )
    }

    fun searchDbFor(query: UiEtfQuery) {
        val action = IO.fx {
            showLoading.bind()
            searchDbIo(query).bind()
        }
        runIO(
            io = action,
            onSuccess = onEtfDataRetrieved
        )
    }

}
