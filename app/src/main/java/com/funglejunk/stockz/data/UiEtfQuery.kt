package com.funglejunk.stockz.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UiEtfQuery(
    val name: String,
    val ter: Float,
    val profitUse: String,
    val replicationMethod: String
) : Parcelable {

    companion object {
        private const val STRING_EMPTY = ""

        const val TER_MAX = 1.0f
        const val NAME_EMPTY = STRING_EMPTY
        const val PROFIT_USE_EMPTY = STRING_EMPTY
        const val REPLICATION_METHOD_EMPTY = STRING_EMPTY
    }

    fun isEmpty() = name == NAME_EMPTY &&
            ter == TER_MAX &&
            profitUse == PROFIT_USE_EMPTY &&
            replicationMethod == REPLICATION_METHOD_EMPTY
}
