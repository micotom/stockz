package com.funglejunk.stockz.repo.fboerse

import com.funglejunk.stockz.data.RepoHistoryData
import com.funglejunk.stockz.data.RepoPerformanceData
import java.time.LocalDate

interface FBoerseRepo {
    suspend fun getHistory(isin: String, minDate: LocalDate, maxDate: LocalDate): RepoHistoryData
    suspend fun getHistoryPerfData(isin: String): RepoPerformanceData
}
