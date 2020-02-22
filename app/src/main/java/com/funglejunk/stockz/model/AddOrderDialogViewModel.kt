package com.funglejunk.stockz.model

import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.repo.db.Buys
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.util.FViewModel

class AddOrderDialogViewModel(private val db: XetraDbInterface) : FViewModel() {

    fun submit(buy: Buys) {
        val io = IO.fx {
            !effect { db.buysDao().insert(buy) }
        }
        val success = IO.just { count: Long -> Unit } // TODO display success
        runIO(
            io = io,
            onSuccess = success
        )
    }

}