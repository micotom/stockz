package com.funglejunk.stockz.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepoHistoryData(
    val isin: String,
    @SerialName("data")
    val content: List<Data>,
    val totalCount: Int,
    val tradedInPercent: Boolean
) {
    @Serializable
    data class Data(
        val date: String,
        @SerialName("open")
        val openValue: Double,
        val close: Double,
        val high: Double,
        val low: Double,
        val turnoverPieces: Double,
        val turnoverEuro: Double
    )
}
