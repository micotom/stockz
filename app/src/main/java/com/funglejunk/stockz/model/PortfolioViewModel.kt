package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.None
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.fx.*
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.async.async
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.PortfolioEntry
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.round3
import com.funglejunk.stockz.util.FViewModel
import timber.log.Timber
import java.time.LocalDate

typealias PortfolioEntries = List<PortfolioViewModel.PortfolioViewEntry>

// TODO proper error handling
class PortfolioViewModel(private val db: XetraDbInterface, private val fBoerseRepo: FBoerseRepo) :
    FViewModel() {

    data class PortfolioPerformance(
        val totalValue: Double, val totalPerformance: Float, val performanceSinceTwoWeeksBefore: Float
    )

    data class PortfolioViewEntry(
        val isin: String, val etfName: String, val currentValue: Double, val twoWeeksAgoValue: Double,
        val amount: Double, val buyPrice: Double, val performance: Float
    )

    sealed class ViewState {
        data class PortfolioRead(
            val entries: PortfolioEntries,
            val performance: PortfolioPerformance
        ) : ViewState()

        data class NewAddButtonEnabledState(val enabled: Boolean) : ViewState()
        data class PortfolioEntrySaved(val entry: PortfolioViewEntry) : ViewState()
    }

    val viewState: LiveData<ViewState> = MutableLiveData<ViewState>()

    private var newEtf: Etf? = null

    private val priceCacheFactory = MVar.factoryUncancelable(IO.async())
    private val amountCacheFactory = MVar.factoryUncancelable(IO.async())

    private var priceCache = priceCacheFactory.empty<Double>()
    private var amountCache = amountCacheFactory.empty<Double>()

    private val onPortfolioRetrievalSuccess: IO<(Pair<PortfolioEntries, PortfolioPerformance>) -> Unit> =
        IO.just { data ->
            viewState.mutable().value = ViewState.PortfolioRead(data.first, data.second)
        }

    private val allPortfolioEntriesIo: () -> IO<Pair<PortfolioEntries, PortfolioPerformance>> = {
        IO.fx {
            effect {
                db.portfolioDao().getAll()
            }.bind().map { dbEntry ->
                effect {
                    fBoerseRepo.getHistory(
                        dbEntry.isin, LocalDate.now().minusDays(14), LocalDate.now()
                    )
                }.map {
                    dbEntry to it
                }
            }.map {
                val (dbEntry, repoEntry) = it.bind()
                val twoWeeksAgoPrice = repoEntry.content.first().close
                val currentPrice = repoEntry.content.last().openValue
                effect {
                    PortfolioViewEntry(
                        isin = dbEntry.isin,
                        etfName = dbEntry.name,
                        currentValue = currentPrice,
                        amount = dbEntry.amount,
                        buyPrice = dbEntry.price.round3(),
                        twoWeeksAgoValue = twoWeeksAgoPrice,
                        performance = calculateValuePerformance(currentPrice, dbEntry.price)
                    )
                }
            }.parSequence().map { allEntries ->
                allEntries to calculatePortfolioPerformance(allEntries)
            }.bind()
        }
    }

    private fun calculatePortfolioPerformance(allEntries: List<PortfolioViewEntry>) =
        with(allEntries) {
            val totalValue = sumByDouble { it.amount * it.currentValue }
            val totalMoneySpent = sumByDouble { it.amount * it.buyPrice }
            val totalValueTwoWeeksAgo = sumByDouble { it.amount * it.twoWeeksAgoValue }
            val performance = calculateValuePerformance(totalValue, totalMoneySpent)
            val performanceSinceTwoWeeksBefore = calculateValuePerformance(totalValue,
                totalValueTwoWeeksAgo)
            PortfolioPerformance(
                totalValue.round3(), performance.round3(), performanceSinceTwoWeeksBefore
            )
        }

    private fun calculateValuePerformance(currentValue: Double, buyValue: Double): Float =
        ((currentValue - buyValue) / buyValue * 100).toFloat().round3()

    private val addDbEntryIo: () -> IO<PortfolioViewEntry?> = {
        IO.fx {
            newEtf?.let { etf ->
                val price = priceCache.fix().flatMap {
                    it.take()
                }.unsafeRunSync()
                val amount = amountCache.fix().flatMap {
                    it.take()
                }.unsafeRunSync()
                val newEntry = PortfolioEntry(
                    name = etf.name,
                    isin = etf.isin,
                    price = price,
                    amount = amount
                )
                val updatedEntry = effect {
                    when (db.portfolioDao().getEntryCountForIsin(newEntry.isin)) {
                        0L -> {
                            db.portfolioDao().insert(newEntry)
                            newEntry
                        }
                        else -> {
                            val existingEntry =
                                db.portfolioDao().getEntryWithIsin(newEntry.isin).first()
                            val newExpenses = newEntry.price * newEntry.amount
                            val existingExpenses = existingEntry.price * existingEntry.amount
                            val newAmount = existingEntry.amount + newEntry.amount
                            val newPrice =
                                (newExpenses + existingExpenses) / (existingEntry.amount + newEntry.amount)
                            val updatedEntry = newEntry.copy(
                                price = newPrice, amount = newAmount
                            )
                            db.portfolioDao().insert(updatedEntry)
                            updatedEntry
                        }
                    }
                }.bind()
                val repoEntry = IO.effect {
                    fBoerseRepo.getHistory(
                        etf.isin, LocalDate.now().minusDays(14), LocalDate.now()
                    )
                }.bind()
                val twoWeeksAgoPrice = repoEntry.content.first().close
                val currentPrice = repoEntry.content.last().openValue
                PortfolioViewEntry(
                    isin = etf.isin,
                    etfName = newEntry.name,
                    currentValue = currentPrice,
                    amount = updatedEntry.amount,
                    buyPrice = updatedEntry.price.round3(),
                    twoWeeksAgoValue = twoWeeksAgoPrice,
                    performance = calculateValuePerformance(currentPrice, updatedEntry.price)
                )
            }
        }
    }

    private val onDbEntryAddSuccess: IO<(PortfolioViewEntry?) -> Unit> =
        IO.just { entry ->
            if (entry != null) {
                viewState.mutable().value =
                    ViewState.PortfolioEntrySaved(entry)
            } else {
                Timber.e("db entry is null!")
            }
        }

    fun init() {
        runIO(
            io = allPortfolioEntriesIo(),
            onSuccess = onPortfolioRetrievalSuccess
        )
    }

    fun newAddInformationSet(priceStr: String, amountStr: String) {
        val cleanPrice = priceStr.clean()
        val priceOption = when (cleanPrice.isNotEmpty()) {
            true -> Option.just(cleanPrice.toDouble())
            false -> None
        }
        val cleanAmount = amountStr.clean()
        val amountOption = when (amountStr.isNotEmpty()) {
            true -> Option.just(cleanAmount.toDouble())
            false -> None
        }
        val buttonValidate = IO {
            { enable: Boolean ->
                viewState.mutable().postValue(
                    ViewState.NewAddButtonEnabledState(enable)
                )
            }
        }
        Option.fx {
            val price = priceOption.bind()
            val amount = amountOption.bind()
            price to amount
        }.fold(
            {
                runIO(
                    io = IO.just(false),
                    onSuccess = buttonValidate
                )
            },
            { (price, amount) ->
                runIO(
                    io = {
                        priceCache = priceCacheFactory.just(price)
                        amountCache = amountCacheFactory.just(amount)
                        IO.just(true)
                    }(),
                    onSuccess = buttonValidate
                )
            }
        )
    }

    val removeFromDbIo: (PortfolioViewEntry) -> IO<Unit> = { entry ->
        IO.fx {
            val asRecord = PortfolioEntry(
                entry.isin, entry.etfName, entry.amount, entry.buyPrice
            )
            effect {
                db.portfolioDao().removeItem(asRecord)
            }.bind()
            Unit
        }
    }

    fun removeFromPortfolio(etf: PortfolioViewEntry) {
        runIO(
            io = removeFromDbIo(etf).followedBy(allPortfolioEntriesIo()),
            onSuccess = onPortfolioRetrievalSuccess
        )
    }

    private fun String.clean() =
        replace(",", ".").replace("[^\\d.]+", "")

    fun addButtonPressed() {
        runIO(
            io = addDbEntryIo().followedBy(allPortfolioEntriesIo()),
            onSuccess = onDbEntryAddSuccess.followedBy(onPortfolioRetrievalSuccess)
        )
    }

    fun setEtfArgs(args: Etf) {
        newEtf = args
    }
}