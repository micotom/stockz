package com.funglejunk.stockz.data.wtd

import kotlinx.serialization.Serializable

@Deprecated("Worldtradingdata API is deprecated")
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