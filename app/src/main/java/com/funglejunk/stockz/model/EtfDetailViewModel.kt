package com.funglejunk.stockz.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.ChartValue
import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import com.funglejunk.stockz.mutable
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.toLocalDate
import com.funglejunk.stockz.util.FViewModel
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

typealias StockData = Pair<DrawableHistoricData, FBoersePerfData>

class EtfDetailViewModel(private val fBoerseRepo: FBoerseRepo) : FViewModel() {

    sealed class ViewState {
        object Loading : ViewState()
        data class Error(val error: Throwable) : ViewState()
        data class NewChartData(
            val drawableHistoricValues: DrawableHistoricData,
            val performanceData: FBoersePerfData
        ) : ViewState()
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

    private val showLoadingIO: IO<Unit> = IO.fx {
        viewStateData.mutable().postValue(ViewState.Loading)
    }

    private val onHistoryFetchedIO: IO<(StockData) -> Unit> = IO.just { (drawableData, perfData) ->
        viewStateData.mutable().postValue(ViewState.NewChartData(drawableData, perfData))
    }

    private fun fetchFboerseHistoy(
        isin: String,
        fromDate: LocalDate = LocalDate.of(2010, 1, 1),
        toDate: LocalDate = LocalDate.now()
    ) {
        val action = IO.fx {
            showLoadingIO.bind()
            fetchHistoryAction(isin, fromDate, toDate).bind()
        }
        runIO(
            io = action,
            onSuccess = onHistoryFetchedIO
        )
    }

    fun setEtfArgs(etf: Etf) {
        val receivedNewEtfArg = null == etfArg || etfArg != etf
        if (receivedNewEtfArg) {
            fetchFboerseHistoy(etf.isin)
        }
        etfArg = etf.copy()
    }

}
