package com.funglejunk.stockz.repo.fboerse

import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import java.time.LocalDate

interface FBoerseRepo {
    suspend fun getHistory(isin: String, minDate: LocalDate, maxDate: LocalDate): FBoerseHistoryData
    suspend fun getHistoryPerfData(isin: String): FBoersePerfData
}
