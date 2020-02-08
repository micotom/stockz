package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.FViewModel

class AssetDetailViewModel(private val db: XetraDbInterface) : FViewModel() {

    sealed class ViewState {
        data class EtfInfoRetrieved(val etf: Etf) : ViewState()
    }

    val liveData: LiveData<ViewState> = MutableLiveData()

    private val dbInfoIO: (String) -> IO<Etf> = { isin ->
        IO.fx {
            effect { db.etfFlattenedDao().getEtfWithIsin(isin) }.bind()
        }
    }

    private val onDbInfoRetrieved: IO<(Etf) -> Unit> = IO.just { etf ->
        liveData.mutable().value = ViewState.EtfInfoRetrieved(etf)
    }

    fun requestDbInfo(isin: String) {
        runIO(
            io = dbInfoIO(isin),
            onSuccess = onDbInfoRetrieved
        )
    }

}