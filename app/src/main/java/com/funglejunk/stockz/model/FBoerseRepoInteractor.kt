package com.funglejunk.stockz.model

import arrow.Kind
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.typeclasses.ConcurrentSyntax
import com.funglejunk.stockz.data.RepoHistoryData
import com.funglejunk.stockz.data.RepoPerformanceData
import com.funglejunk.stockz.repo.db.CacheableData
import com.funglejunk.stockz.repo.db.StockDataCacheInterface
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.util.StockData
import com.funglejunk.stockz.util.TimeSpanFilter
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

class FBoerseRepoInteractor(
    private val fBoerseRepo: FBoerseRepo,
    private val historyCache: StockDataCacheInterface
) {

    val fetchHistoryAction: (String, TimeSpanFilter) -> IO<RepoHistoryData> =
        { isin, timeFilter ->
            IO.fx {
                val cachedData = historyCache.get(isin).bind()
                cachedData.fold(
                    {
                        fetchRawRepoData(isin, timeFilter.startDate, timeFilter.now)
                    },
                    {
                        it.copy(content = it.content.filter { timeFilter(it) })
                    }
                )
            }
        }

    val fetchHistoryAndPerfAction: (String, LocalDate, LocalDate) -> IO<StockData> =
        { isin, fromDate, toDate ->
            IO.fx {
                val chartDataIO = IO.fx {
                    val history = fetchRawRepoData(isin, fromDate, toDate)
                    val fullHistory = persistAndMergeWithCacheData(isin, history).bind()
                    fullHistory
                }
                val historyDataIO = effect {
                    fBoerseRepo.getHistoryPerfData(isin)
                }
                createParallelIoActionFrom(chartDataIO, historyDataIO).bind()
            }
        }

    private fun createParallelIoActionFrom(
        chartDataIO: IO<RepoHistoryData>,
        historyDataIO: Kind<ForIO, RepoPerformanceData>
    ): IO<Pair<RepoHistoryData, RepoPerformanceData>> =
        IO.parMapN(
            Dispatchers.IO,
            chartDataIO,
            historyDataIO
        ) { chartData, historyData ->
            chartData to historyData
        }

    private fun persistAndMergeWithCacheData(
        isin: String,
        history: RepoHistoryData
    ): IO<RepoHistoryData> =
        IO.fx {
            val cacheSuccess =
                historyCache.persist(CacheableData(isin, history)).bind()
            when (cacheSuccess) {
                true -> historyCache.get(isin).bind().fold(
                    { history },
                    { it }
                )
                false -> history
            }
        }

    private suspend fun ConcurrentSyntax<ForIO>.fetchRawRepoData(
        isin: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): RepoHistoryData = effect {
        val data = fBoerseRepo.getHistory(isin, fromDate, toDate)
        data.copy(
            content = data.content.sortedBy { it.date }
        )
    }.bind()
}