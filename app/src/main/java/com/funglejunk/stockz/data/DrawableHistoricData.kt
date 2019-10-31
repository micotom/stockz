package com.funglejunk.stockz.data

import java.time.LocalDate

data class DrawableHistoricData(val data: List<ChartValue>) : Collection<ChartValue> by data

data class ChartValue(val date: LocalDate, val value: Float)