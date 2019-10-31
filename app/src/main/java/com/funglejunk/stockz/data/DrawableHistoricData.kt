package com.funglejunk.stockz.data

import java.util.*

data class DrawableHistoricData(val data: List<ChartValue>) : Collection<ChartValue> by data

// TODO move from java.util.Date to LocalDate
data class ChartValue(val date: Date, val value: Float)