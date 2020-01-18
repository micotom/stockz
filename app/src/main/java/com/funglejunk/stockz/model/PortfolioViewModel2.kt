package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.model.portfolio.AssetSummary
import com.funglejunk.stockz.model.portfolio.PortfolioSummary
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.Buys
import com.funglejunk.stockz.repo.db.Portfolio
import com.funglejunk.stockz.repo.db.TargetAllocation
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.util.FViewModel
import java.time.LocalDate

class PortfolioViewModel2(private val db: XetraDbInterface, private val fBoerseRepo: FBoerseRepo) :
    FViewModel() {

    sealed class ViewState {
        data class NewPortfolioData(val portfolioSummary: PortfolioSummary) : ViewState()
    }

    val liveData: LiveData<ViewState> = MutableLiveData()

    fun addFooData() {
        val action = IO.fx {
            val pid = !effect {
                val allPortfolios = db.portfolioDao2().getAll()
                if (allPortfolios.isEmpty()) {
                    db.portfolioDao2().insert(Portfolio(name = "Foo Portfolio"))
                    val pid = db.portfolioDao2().getAll().first().rowid
                    val allTargetAllocations = db.targetAllocationsDao().getForPortfolioId(pid)
                    if (allTargetAllocations.isEmpty()) {
                        val alloc = TargetAllocation(
                            isin = "IE00BKX55T58", target = 60.0, portfolioId = pid
                        )
                        db.targetAllocationsDao().insert(alloc)
                    }
                    pid
                } else {
                    allPortfolios.first().rowid
                }
            }
            !effect {
                val allBuys = db.buysDao().getBuysForPortfolio(pid)
                if (allBuys.isEmpty()) {
                    db.buysDao().insert(
                        Buys(
                            isin = "IE00BKX55T58",
                            portfolioId = pid,
                            date = LocalDate.of(2019, 10, 4),
                            shares = 12.0,
                            pricePerShare = 56.840.toBigDecimal(),
                            expenses = 6.4.toBigDecimal()
                        )
                    )
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
                    AssetSummary(
                        isin = alloc.isin,
                        currentSharePrice = 63.64.toBigDecimal(),
                        buys = allBuys.filter { it.first == alloc.isin }.map { it.second }.toSet(),
                        targetAllocationPercent = allAllocs.first { it.isin == alloc.isin }.target
                    )
                }.toSet()
                PortfolioSummary(
                    assets = assetSummaries
                )
            }
        }

        val onSuccess = IO.just { summary: PortfolioSummary ->
            liveData.mutable().value = ViewState.NewPortfolioData(summary)
        }

        runIO(
            io = action,
            onSuccess = onSuccess
        )
    }

}