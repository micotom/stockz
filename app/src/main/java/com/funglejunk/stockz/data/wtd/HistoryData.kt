package com.funglejunk.stockz.data.wtd

import kotlinx.serialization.Serializable

@Serializable
data class HistoryData(
    val name: String,
    val history: Map<String, HistoryDataPoint>
) {

    @Serializable
    data class HistoryDataPoint(
        val `open`: String,
        val close: String,
        val high: String,
        val low: String,
        val volume: String
    )

}