package com.funglejunk.stockz.repo.xetra

import arrow.core.Try
import com.funglejunk.stockz.data.XetraDayData
import io.reactivex.Single
import java.time.LocalDate

interface XetraRepo {

    fun getCloseValueFor(isin: String, date: LocalDate): Single<XetraDayData>

}