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
import com.funglejunk.stockz.util.logError

class EtfListViewModel(
    dbInflater: XetraMasterDataInflater,
    val db: XetraDbInterface
) : FViewModel() {

    val etfData: LiveData<List<Etf>> = MutableLiveData()

    private val queryInteractor = UiQueryDbInteractor()

    init {
        runIO(
            io = loadEtfs(dbInflater),
            onSuccess = { data -> etfData.mutable().postValue(data) }
        )
    }

    private fun loadEtfs(dbInflater: XetraMasterDataInflater) =
        IO.fx {
            dbInflater.init().bind().fold(
                { Either.left(it) },
                {
                    effect {
                        Either.catch {
                            db.etfFlattenedDao().getAll()
                        }
                    }.bind()
                }
            )
        }

    fun searchDbFor(query: UiEtfQuery) {
        val action = {
            val sqlQueryString = queryInteractor.buildSqlStringFrom(query)
            queryInteractor.executeSqlString(sqlQueryString, db)
        }()
        runIO(
            io = action,
            onSuccess = { data -> etfData.mutable().postValue(data) }
        )
    }

}
