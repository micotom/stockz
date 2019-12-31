package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.db.XetraFavourite
import com.funglejunk.stockz.util.EtfList
import com.funglejunk.stockz.util.FViewModel

class FavouritesViewModel(private val db: XetraDbInterface) : FViewModel() {

    sealed class ViewState {
        data class FavouriteList(val content: EtfList): ViewState()
        data class EntryRemoved(val listPosition: Int): ViewState()
    }

    val favouritesData: LiveData<ViewState> = MutableLiveData()

    private val getFavouritesIO: () -> IO<EtfList> = {
        IO {
            db.favouritesDao().getAll()
        }
    }

    private val onFavouritesRetrievedIO: IO<(EtfList) -> Unit> = IO.just { etfList ->
        favouritesData.mutable().postValue(
            ViewState.FavouriteList(etfList)
        )
    }

    private val removeItemIO: (Etf, Int) -> IO<Int> = { etf, listPosition ->
        IO.fx {
            effect {
                db.favouritesDao().removeItem(XetraFavourite(etf.isin))
            }.map {
                listPosition
            }.bind()
        }
    }

    private val onFavouriteRemovedIO: IO<(Int) -> Unit> = IO.just { removedListPosition ->
        favouritesData.mutable().postValue(
            ViewState.EntryRemoved(removedListPosition)
        )
    }

    init {
        runIO(
            io = getFavouritesIO(),
            onSuccess = onFavouritesRetrievedIO
        )
    }

    fun removeFromFavs(etf: Etf, index: Int) {
        runIO(
            io = removeItemIO(etf, index),
            onSuccess = onFavouriteRemovedIO
        )
    }
}