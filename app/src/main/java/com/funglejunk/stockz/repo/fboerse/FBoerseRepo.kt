package com.funglejunk.stockz.repo.fboerse

import arrow.core.Either
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import io.reactivex.Single
import java.time.LocalDate

interface FBoerseRepo {
    suspend fun getHistory(isin: String, minDate: LocalDate, maxDate: LocalDate):
            Either<Throwable, FBoerseHistoryData>

    suspend fun getHistoryPerfData(isin: String): Either<Throwable, FBoersePerfData>
}
