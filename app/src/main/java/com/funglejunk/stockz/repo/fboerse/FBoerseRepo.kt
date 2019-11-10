package com.funglejunk.stockz.repo.fboerse

import arrow.core.Try
import com.funglejunk.stockz.data.fboerse.FBoerseData
import io.reactivex.Single
import java.time.LocalDate

interface FBoerseRepo {
    fun getHistory(isin: String, minDate: LocalDate, maxDate: LocalDate): Single<Try<FBoerseData>>
}
