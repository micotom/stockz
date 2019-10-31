package com.funglejunk.stockz.model

import android.annotation.SuppressLint
import com.funglejunk.stockz.data.dboerse.DeutscheBoerseDayData
import com.funglejunk.stockz.repo.db.XetraPerformanceEntry
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.dboerse.DeutscheBoerseRepo
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Deprecated("Deutsche Boerse API is deprecated")
class DeutscheBoerseRepoInteractor(db: XetraDb, private val repo: DeutscheBoerseRepo) {

    private companion object {
        val maxAgedDate = LocalDate.of(2017, 6, 17)
        @SuppressLint("SimpleDateFormat")
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val dao = db.perfDao()

    fun getData(isin: String): Single<List<XetraPerformanceEntry>> {

        Timber.d("get data: $isin")

        val lastKnownEntry = dao.getNewestEntryForIsin(isin)
            .map {
                it.date
            }
            .switchIfEmpty(Maybe.just(""))
            .toSingle()

        val remoteGetCall = lastKnownEntry.flatMap {

            Timber.d("last known entry for $isin on: $it")

            val fridaysToRetrieve = when (it.isEmpty()) {
                true -> getFridaysBetween(maxAgedDate, LocalDate.now())
                false -> getFridaysBetween(LocalDate.parse(it, dateFormat), LocalDate.now())
            }

            when (fridaysToRetrieve.isNotEmpty()) {
                true -> {
                    Single.just(fridaysToRetrieve).flatMapObservable { dates ->
                        Observable.fromIterable(dates)
                    }.concatMapSingle { date ->
                        repo.getCloseValueFor(isin, date)
                    }.toList()
                }
                false -> Single.just(emptyList<DeutscheBoerseDayData>())
            }
        }

        val dbUpdateCall = remoteGetCall.doOnEvent { newEntries, _ ->
            newEntries.filter {
                it.endPrice != DeutscheBoerseDayData.NAN
            }.map { xetraDayData ->
                Timber.d("try to update db with: $xetraDayData")
                XetraPerformanceEntry(xetraDayData.isin, xetraDayData.date, xetraDayData.endPrice)
            }.map {
                Timber.d("update db with: $it")
                dao.insert(it)
            }
        }

        val dbGetCall = dbUpdateCall.flatMap {
            dao.getAllEntriesForIsin(isin)
        }.doOnEvent { entries, _ ->
            entries.forEach {
                Timber.d("db reports: $it")
            }
        }

        return dbGetCall
    }

    private fun getFridaysBetween(_start: LocalDate, end: LocalDate): List<LocalDate> {
        var start = _start.copy().plusDays(1)
        val fridays = mutableListOf<LocalDate>()

        while (start.dayOfWeek != DayOfWeek.FRIDAY) {
            start = start.plusDays(1)
        }

        if (start > end) {
            return emptyList()
        }

        fridays.add(start.copy())

        while (start < end) {
            start = start.plusDays(7)
            if (start.dayOfWeek == DayOfWeek.FRIDAY && start <= end) {
                fridays.add(start.copy())
            }
        }

        return fridays
    }

    private fun LocalDate.copy() = LocalDate.of(year, month, dayOfMonth)

}