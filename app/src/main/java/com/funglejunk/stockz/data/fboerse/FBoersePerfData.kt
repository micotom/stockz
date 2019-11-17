package com.funglejunk.stockz.data.fboerse

import kotlinx.serialization.Serializable

@Serializable
data class FBoersePerfData(
    @Transient val isin: String? = null,
    val months1: Performance,
    val months3: Performance,
    val months6: Performance,
    val years1: Performance,
    val years2: Performance,
    val years3: Performance
) {
    @Serializable
    data class Performance(
        val changeInPercent: Double,
        val high: Double,
        val low: Double
    )
}