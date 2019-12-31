package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.EtfList
import com.funglejunk.stockz.util.FViewModel

class FavouritesViewModel(private val db: XetraDbInterface) : FViewModel() {

    val favouritesData: LiveData<EtfList> = MutableLiveData()

    private val getFavouritesIO: () -> IO<EtfList> = {
        IO {
            db.favouritesDao().getAll()
        }
    }

    private val onFavouritesRetrievedIO: IO<(EtfList) -> Unit> = IO.just { etfList ->
        favouritesData.mutable().postValue(etfList)
    }

    init {
        runIO(
            io = getFavouritesIO(),
            onSuccess = onFavouritesRetrievedIO
        )
    }
}