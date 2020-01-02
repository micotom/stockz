package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Option
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.db.XetraFavourite
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.toLocalDate
import com.funglejunk.stockz.util.FViewModel
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.time.LocalDate

typealias StockData = Pair<DrawableHistoricData, FBoersePerfData>

class EtfDetailViewModel(
    private val fBoerseRepo: FBoerseRepo,
    private val db: XetraDbInterface
) : FViewModel() {

    sealed class ViewState {
        object Loading : ViewState()
        data class Error(val error: Throwable) : ViewState()
        data class NewChartData(
            val drawableHistoricValues: DrawableHistoricData,
            val performanceData: FBoersePerfData
        ) : ViewState()
        data class NewEtfFavouriteState(val isFavourite: Boolean) : ViewState()
    }

    val viewStateData: LiveData<ViewState> = MutableLiveData()
    private var etfArg: Etf? = null

    private val fetchHistoryAction: (String, LocalDate, LocalDate) -> IO<StockData> =
        { isin, fromDate, toDate ->
            IO.fx {
                val chartDataIO = effect {
                    val history = fBoerseRepo.getHistory(isin, fromDate, toDate)
                    val chartData = history.content
                        .map { dayHistory ->
                            ChartValue(dayHistory.date.toLocalDate(), dayHistory.close.toFloat())
                        }
                        .sortedBy { it.date }
                    DrawableHistoricData(chartData)
                }
                val historyDataIO = effect {
                    fBoerseRepo.getHistoryPerfData(isin)
                }
                IO.parMapN(
                    Dispatchers.IO,
                    chartDataIO,
                    historyDataIO
                ) { chartData, historyData ->
                    chartData to historyData
                }.bind()
            }
        }

    private val showLoadingIO: () -> IO<Unit> = {
        IO.fx {
            viewStateData.mutable().postValue(ViewState.Loading)
        }
    }

    private val onHistoryFetchedIO: IO<(StockData) -> Unit> = IO.just { (drawableData, perfData) ->
        viewStateData.mutable().postValue(ViewState.NewChartData(drawableData, perfData))
    }

    private val onHistoryFetchError: IO<(Throwable) -> Unit> = IO.just { throwable ->
        Timber.e(throwable)
        viewStateData.mutable().postValue(
            ViewState.Error(throwable)
        )
    }

    private fun fetchFboerseHistoy(
        isin: String,
        fromDate: LocalDate = LocalDate.of(2010, 1, 1),
        toDate: LocalDate = LocalDate.now()
    ) {
        val action = IO.fx {
            showLoadingIO().followedBy(
                fetchHistoryAction(isin, fromDate, toDate)
            ).bind()
        }
        runIO(
            io = action,
            onSuccess = onHistoryFetchedIO,
            onFailure = onHistoryFetchError
        )
    }

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
        viewStateData.mutable().postValue(
            ViewState.NewEtfFavouriteState(isFavourite)
        )
    }

    fun addToFavourites(etf: Etf) {
        runIO(
            io = saveFavouriteIO(etf),
            onSuccess = onFavouriteSaved
        )
    }

}
