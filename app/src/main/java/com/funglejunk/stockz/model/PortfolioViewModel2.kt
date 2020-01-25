package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.concurrent.parSequence
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.model.portfolio.AssetSummary
import com.funglejunk.stockz.model.portfolio.PortfolioSummary
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.Buys
import com.funglejunk.stockz.repo.db.Portfolio
import com.funglejunk.stockz.repo.db.TargetAllocation
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.util.FViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.LocalDate

typealias PortfolioSummaryViewModel = Pair<PortfolioSummary, List<Etf>>

class PortfolioViewModel2(
    private val db: XetraDbInterface,
    private val dbInflater: XetraMasterDataInflater,
    private val fBoerseRepo: FBoerseRepo
) :
    FViewModel() {

    sealed class ViewState {
        data class NewPortfolioData(val portfolioSummary: PortfolioSummaryViewModel) : ViewState()
        data class NewChartData(val history: FBoerseHistoryData) : ViewState()
    }

    val liveData: LiveData<ViewState> = MutableLiveData()

    private val loadEtfAction: (XetraMasterDataInflater) -> IO<Unit> =
        { dbInflater: XetraMasterDataInflater ->
            dbInflater.init().flatMap {
                IO.fx {
                    !effect {
                        db.etfFlattenedDao().getAll()
                    }
                    Unit
                }
            }
        }

    fun addFooData() {
        val action = IO.fx {
            val pid = !effect {
                val allPortfolios = db.portfolioDao2().getAll()
                if (allPortfolios.isEmpty()) {
                    db.portfolioDao2().insert(Portfolio(name = "Foo Portfolio"))
                    val pid = db.portfolioDao2().getAll().first().rowid
                    val allTargetAllocations = db.targetAllocationsDao().getForPortfolioId(pid)
                    if (allTargetAllocations.isEmpty()) {
                        val alloc1 = TargetAllocation(
                            isin = "IE00BKX55T58", target = 60.0, portfolioId = pid
                        )
                        val alloc2 = TargetAllocation(
                            isin = "LU1737652823", target = 5.0, portfolioId = pid
                        )
                        /*
                        val alloc3 = TargetAllocation(
                            isin = "DE000EWG2LD7", target = 5.0, portfolioId = pid
                        )
                         */
                        val alloc4 = TargetAllocation(
                            isin = "IE00BZ163L38", target = 15.0, portfolioId = pid
                        )
                        val alloc5 = TargetAllocation(
                            isin = "IE00B3VVMM84", target = 20.0, portfolioId = pid
                        )
                        db.targetAllocationsDao().insert(alloc1)
                        db.targetAllocationsDao().insert(alloc2)
                        // db.targetAllocationsDao().insert(alloc3)
                        db.targetAllocationsDao().insert(alloc4)
                        db.targetAllocationsDao().insert(alloc5)
                    }
                    pid
                } else {
                    allPortfolios.first().rowid
                }
            }
            !effect {
                val allBuys = db.buysDao().getBuysForPortfolio(pid)
                if (allBuys.isEmpty()) {
                    allBuys(pid).forEach {
                        db.buysDao().insert(it)
                    }
                }
            }
            !effect {
                val allBuys = db.buysDao().getBuysForPortfolio(pid).map {
                    it.isin to AssetSummary.AssetBuy(
                        date = it.date,
                        shares = it.shares,
                        pricePerShare = it.pricePerShare,
                        expenses = it.expenses
                    )
                }
                val allAllocs = db.targetAllocationsDao().getForPortfolioId(pid)
                val assetSummaries = allAllocs.map { alloc ->
                    val history = fBoerseRepo.getHistory(
                        alloc.isin,
                        LocalDate.now().minusDays(7),
                        LocalDate.now()
                    ).content.maxBy { it.date }
                    AssetSummary(
                        isin = alloc.isin,
                        currentSharePrice = BigDecimal.valueOf(history?.close ?: -1.0),
                        buys = allBuys.filter { it.first == alloc.isin }.map { it.second }.toSet(),
                        targetAllocationPercent = allAllocs.first { it.isin == alloc.isin }.target
                    )
                }.toSet()
                val summary = PortfolioSummary(assets = assetSummaries)
                val etfs = allAllocs.map { alloc ->
                    db.etfFlattenedDao().getEtfWithIsin(alloc.isin)
                }
                Pair(summary, etfs)
            }
        }

        val onSuccess = IO.just { summary: PortfolioSummaryViewModel ->
            liveData.mutable().value = ViewState.NewPortfolioData(summary)
        }

        runIO(
            io = loadEtfAction(dbInflater).followedBy(action),
            onSuccess = onSuccess
        )
    }

    fun loadChart(portfolio: PortfolioSummary) {
        val historyIo = IO.fx {
            val historyData = portfolio.assets.map {
                effect {
                    delay(500)
                    it to fBoerseRepo.getHistory(it.isin, LocalDate.of(2010, 1, 1), LocalDate.now())
                }
            }.parSequence().bind()
            val scaledList = historyData.map { (assetSummary, data) ->
                assetSummary to data.content.map {
                    it.copy(close = it.close * assetSummary.shares)
                }
            }
            val assets = scaledList.map { it.first }
            val histories = scaledList.map { it.second }

            val totalData = histories
                .flatten()
                .groupBy { it.date }.filter {
                    it.value.size == assets.size
                }
            val congregatedTotalData = totalData.map { (date, data) ->
                date to data.sumByDouble { it.close }
            }
            FBoerseHistoryData(
                isin = "n/a",
                totalCount = portfolio.assets.size,
                tradedInPercent = false,
                content = congregatedTotalData.map { (dateStr, value) ->
                    FBoerseHistoryData.Data(
                        date = dateStr, openValue = -1.0, close = value, high = -1.0, low = -1.0,
                        turnoverPieces = -1.0, turnoverEuro = -1.0
                    )
                }.sortedBy { it.date }
            )
        }
        /*
        val scaledList = list.map { (assetSummary, data) ->
            assetSummary to data.content.map {
                it.copy(close = it.close * assetSummary.shares)
            }
        }
        val assets = scaledList.map { it.first }
        val histories = scaledList.map { it.second }

        val totalData = scaledList.map { it.second }
            .flatten()
            .groupBy { it.date }.filter {
                it.value.size == assets.size
            }
        val congregatedTotalData = totalData.map { (date, data) ->
            date to data.sumByDouble { it.close }
        }
        FBoerseHistoryData(
            isin = "n/a",
            totalCount = portfolio.assets.size,
            tradedInPercent = false,
            content = congregatedTotalData.map { (dateStr, value) ->
                FBoerseHistoryData.Data(
                    date = dateStr, openValue = -1.0, close = value, high = -1.0, low = -1.0,
                    turnoverPieces = -1.0, turnoverEuro = -1.0
                )
            }.sortedBy { it.date }
        )
         */
        runIO(
            io = historyIo,
            onSuccess = IO.just { chartData ->
                liveData.mutable().value = ViewState.NewChartData(chartData)
            }
        )
    }

    private fun allBuys(pid: Int) = setOf(
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 12.0,
            pricePerShare = 56.840.toBigDecimal(),
            expenses = 6.4.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 6.0,
            pricePerShare = 49.820.toBigDecimal(),
            expenses = 6.4.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 17.0,
            pricePerShare = 58.380.toBigDecimal(),
            expenses = 6.4.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 8.0,
            pricePerShare = 51.060.toBigDecimal(),
            expenses = 6.4.toBigDecimal()
        ),
        Buys(
            isin = "LU1737652823",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 8.0,
            pricePerShare = 61.990.toBigDecimal(),
            expenses = 9.49.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 11.0,
            pricePerShare = 60.510.toBigDecimal(),
            expenses = 12.59.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 6.0,
            pricePerShare = 52.0.toBigDecimal(),
            expenses = 12.17.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 22.0,
            pricePerShare = 62.2.toBigDecimal(),
            expenses = 9.3.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 10.0,
            pricePerShare = 55.500.toBigDecimal(),
            expenses = 9.3.toBigDecimal()
        ),
        Buys(
            isin = "DE000EWG2LD7",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 11.0,
            pricePerShare = 46.260.toBigDecimal(),
            expenses = 9.51.toBigDecimal()
        ),
        Buys(
            isin = "IE00BZ163L38",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 11.0,
            pricePerShare = 47.407.toBigDecimal(),
            expenses = 9.3.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 21.0,
            pricePerShare = 62.810.toBigDecimal(),
            expenses = 9.3.toBigDecimal()
        ),
        Buys(
            isin = "IE00BZ163L38",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 8.0,
            pricePerShare = 47.507.toBigDecimal(),
            expenses = 9.3.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 0.614,
            pricePerShare = 56.139.toBigDecimal(),
            expenses = 0.52.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 1.799,
            pricePerShare = 62.960.toBigDecimal(),
            expenses = 1.7.toBigDecimal()
        ),
        Buys(
            isin = "IE00BZ163L38",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 0.527,
            pricePerShare = 47.364.toBigDecimal(),
            expenses = 0.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 0.872,
            pricePerShare = 50.830.toBigDecimal(),
            expenses = 0.67.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 1.776,
            pricePerShare = 58.230.toBigDecimal(),
            expenses = 1.55.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 0.851,
            pricePerShare = 52.070.toBigDecimal(),
            expenses = 0.67.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 1.708,
            pricePerShare = 60.550.toBigDecimal(),
            expenses = 1.55.toBigDecimal()
        ),
        Buys(
            isin = "IE00B3VVMM84",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 0.824,
            pricePerShare = 53.760.toBigDecimal(),
            expenses = 0.67.toBigDecimal()
        ),
        Buys(
            isin = "IE00BKX55T58",
            portfolioId = pid,
            date = LocalDate.of(2019, 10, 4),
            shares = 1.681,
            pricePerShare = 61.520.toBigDecimal(),
            expenses = 1.55.toBigDecimal()
        )
    )

}