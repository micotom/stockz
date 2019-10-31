package com.funglejunk.stockz.data

import java.util.*

data class DrawableHistoricData(val data: List<ChartValue>) : Collection<ChartValue> by data

data class ChartValue(val date: Date, val value: Float)