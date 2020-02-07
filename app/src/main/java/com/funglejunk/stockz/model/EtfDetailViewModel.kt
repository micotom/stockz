package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Option
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.StockDataCacheInterface
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.db.XetraFavourite
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.util.FViewModel
import com.funglejunk.stockz.util.StockData
import com.funglejunk.stockz.util.TimeSpanFilter
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.time.LocalDate

class EtfDetailViewModel(
    private val fBoerseRepo: FBoerseRepo,
    private val db: XetraDbInterface,
    private val historyCache: StockDataCacheInterface
) : FViewModel() {

    sealed class ViewState {
        object Loading : ViewState()
        data class Error(val error: Throwable) : ViewState()
        data class NewChartData(
            val etf: Etf,
            val historyData: FBoerseHistoryData,
            val performanceData: FBoersePerfData
        ) : ViewState()

        data class NewEtfFavouriteState(val isFavourite: Boolean) : ViewState()
    }

    val viewStateData: LiveData<ViewState> = MutableLiveData()

    private val repoCacheInteractor = FBoerseRepoInteractor(fBoerseRepo, historyCache)
    private var etfArg: Etf? = null

    private val showLoadingIO: IO<Unit> = IO.fx {
        continueOn(Dispatchers.Main)
        viewStateData.mutable().value = ViewState.Loading
        continueOn(Dispatchers.IO)
    }

    private val onHistoryFetchedIO: IO<(StockData) -> Unit> = IO.just { (historyData, perfData) ->
        etfArg?.let {
            viewStateData.mutable().value = ViewState.NewChartData(it, historyData, perfData)
        }
    }

    private val onHistoryFetchError: IO<(Throwable) -> Unit> = IO.just { throwable ->
        Timber.e(throwable)
        viewStateData.mutable().value = ViewState.Error(throwable)
    }

    private fun fetchFboerseHistoy(
        isin: String,
        fromDate: LocalDate = LocalDate.of(2010, 1, 1),
        toDate: LocalDate = LocalDate.now()
    ) {
        val action = showLoadingIO.followedBy(
            repoCacheInteractor.fetchHistoryAction.invoke(isin, fromDate, toDate)
        )
        runIO(
            io = action,
            onSuccess = onHistoryFetchedIO,
            onFailure = onHistoryFetchError
        )
    }

    fun getHistory(timespan: TimeSpanFilter): FBoerseHistoryData? =
        etfArg?.let {
            IO.fx {
                val history = historyCache.get(it.isin).bind()
                history.fold(
                    { null },
                    {
                        it.copy(
                            content = it.content.filter {
                                timespan(it)
                            }
                        )
                    }
                )
            }.unsafeRunSync()
        } ?: {
            null
        }()

    fun setEtfArgs(etf: Etf) {
        val receivedNewEtfArg = null == etfArg || etfArg != etf
        if (receivedNewEtfArg) {
            applyFavouriteState(etf)
            fetchFboerseHistoy(etf.isin) // TODO should maybe run combined with favourite state
        }
        etfArg = etf.copy()
    }

    private fun applyFavouriteState(etf: Etf) {
        runIO(
            io = queryIsFavouriteIO(etf),
            onSuccess = onIsFavouriteStateSuccess
        )
    }

    private val saveFavouriteIO: (Etf) -> IO<Option<Etf>> = { etf ->
        IO.fx {
            val dbResult = effect {
                db.favouritesDao().insert(XetraFavourite(etf.isin))
            }.bind()
            when (dbResult) {
                0L -> Option.empty()
                else -> Option.just(etf)
            }
        }
    }

    private val onFavouriteSaved: IO<(Option<Etf>) -> Unit> = IO.just { result ->
        result.fold(
            { Timber.w("favourite saving went wrong without exception") },
            { etf ->
                runIO(
                    io = queryIsFavouriteIO(etf),
                    onSuccess = onIsFavouriteStateSuccess
                )
            }
        )
    }

    private val queryIsFavouriteIO: (Etf) -> IO<Boolean> = { etf ->
        IO.fx {
            val count = effect {
                db.favouritesDao().getRecordCount(etf.isin)
            }.bind()
            count > 0
        }
    }

    private val onIsFavouriteStateSuccess: IO<(Boolean) -> Unit> = IO.just { isFavourite ->
        viewStateData.mutable().value = ViewState.NewEtfFavouriteState(isFavourite)
    }

    fun addToFavourites(etf: Etf) {
        runIO(
            io = saveFavouriteIO(etf),
            onSuccess = onFavouriteSaved
        )
    }

}
