package com.funglejunk.stockz.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class XetraEtfFlattened(
    val name: String,
    val isin: String,
    val symbol: String,
    val listingDate: String,
    val ter: Double,
    val profitUse: String,
    val replicationMethod: String,
    val fundCurrency: String,
    val tradingCurrency: String,
    val publisherName: String,
    val benchmarkName: String
) : Parcelable
