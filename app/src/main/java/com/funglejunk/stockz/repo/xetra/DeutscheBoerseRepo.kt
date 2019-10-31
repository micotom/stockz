package com.funglejunk.stockz.repo.xetra

import com.funglejunk.stockz.data.dboerse.DeutscheBoerseDayData
import io.reactivex.Single
import java.time.LocalDate

@Deprecated("Deutsche Boerse API is deprecated")
interface DeutscheBoerseRepo {

    fun getCloseValueFor(isin: String, date: LocalDate): Single<DeutscheBoerseDayData>

}