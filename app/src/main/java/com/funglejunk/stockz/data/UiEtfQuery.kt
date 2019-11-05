package com.funglejunk.stockz.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UiEtfQuery(
    val name: String = NAME_EMPTY,
    val ter: Float = TER_MAX
) : Parcelable {

    companion object {
        const val TER_MAX = 1.0f
        const val NAME_EMPTY = ""
    }

    fun isEmpty() = name.isEmpty() && ter == TER_MAX

}