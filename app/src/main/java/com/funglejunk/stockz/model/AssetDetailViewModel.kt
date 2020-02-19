package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.StockDataCacheInterface
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.util.FViewModel
import com.funglejunk.stockz.util.StockData
import java.time.LocalDate

class AssetDetailViewModel(
    private val db: XetraDbInterface,
    fBoerseRepo: FBoerseRepo,
    historyCache: StockDataCacheInterface
) : FViewModel() {

    sealed class ViewState {
        data class EtfInfoRetrieved(val etf: Etf, val stockData: StockData) : ViewState()
    }

    val liveData: LiveData<ViewState> = MutableLiveData()

    private val repo = FBoerseRepoInteractor(fBoerseRepo, historyCache)

    private val dbInfoIO: (String) -> IO<Pair<Etf, StockData>> = { isin ->
        IO.fx {
            val basicEtfData = effect { db.etfFlattenedDao().getEtfWithIsin(isin) }.bind()
            val stockData =
                repo.fetchHistoryAndPerfAction(isin, LocalDate.of(2010, 1, 1), LocalDate.now())
                    .bind()
            basicEtfData to stockData
        }
    }

    private val onDbInfoRetrieved: IO<(Pair<Etf, StockData>) -> Unit> =
        IO.just { (etf, stockData) ->
            liveData.mutable().value = ViewState.EtfInfoRetrieved(etf, stockData)
        }

    fun requestDbInfo(isin: String) {
        runIO(
            io = dbInfoIO(isin),
            onSuccess = onDbInfoRetrieved
        )
    }

}