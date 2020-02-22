package com.funglejunk.stockz.data

import android.os.Parcelable
import com.funglejunk.stockz.data.UiEtfQuery.Companion.empty
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UiEtfQuery(
    val name: String,
    val ter: Float,
    val profitUse: String,
    val replicationMethod: String,
    val publisher: String,
    val benchmark: String
) : Parcelable {

    companion object {

        private const val STRING_EMPTY = ""
        const val ALL_PLACEHOLDER = "- All -"
        const val TER_MAX = 1.0f
        const val NAME_EMPTY = STRING_EMPTY
        const val PROFIT_USE_EMPTY = STRING_EMPTY
        const val REPLICATION_METHOD_EMPTY = STRING_EMPTY
        const val PUBLISHER_EMPTY = STRING_EMPTY
        const val BENCHMARK_EMPTY = STRING_EMPTY

        val empty = UiEtfQuery(
            name = NAME_EMPTY,
            ter = TER_MAX,
            profitUse = PROFIT_USE_EMPTY,
            replicationMethod = REPLICATION_METHOD_EMPTY,
            publisher = PUBLISHER_EMPTY,
            benchmark = BENCHMARK_EMPTY
        )
    }

}

fun UiEtfQuery.isEmpty() = this == empty
