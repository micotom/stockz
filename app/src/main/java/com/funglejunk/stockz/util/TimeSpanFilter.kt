package com.funglejunk.stockz.util

import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.toLocalDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class TimeSpanFilter {
    object Max : TimeSpanFilter() {
        override fun invoke(dataPoint: FBoerseHistoryData.Data): Boolean = true
    }
    object Year : TimeSpanFilter() {
        override fun invoke(dataPoint: FBoerseHistoryData.Data): Boolean =
            ChronoUnit.YEARS.between(dataPoint.date.toLocalDate(), now) == 0L
    }
    object Months3 : TimeSpanFilter() {
        override fun invoke(dataPoint: FBoerseHistoryData.Data): Boolean =
            ChronoUnit.MONTHS.between(dataPoint.date.toLocalDate(), now) < 3L
    }
    object Month : TimeSpanFilter() {
        override fun invoke(dataPoint: FBoerseHistoryData.Data): Boolean =
            ChronoUnit.MONTHS.between(dataPoint.date.toLocalDate(), now) == 0L
    }
    object Week : TimeSpanFilter() {
        override fun invoke(dataPoint: FBoerseHistoryData.Data): Boolean =
            ChronoUnit.WEEKS.between(dataPoint.date.toLocalDate(), now) == 0L
    }

    protected val now: LocalDate =
        LocalDate.now()

    abstract operator fun invoke(dataPoint: FBoerseHistoryData.Data): Boolean
}