package com.funglejunk.stockz.util

import com.funglejunk.stockz.data.DrawableHistoricData
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import timber.log.Timber

typealias EtfList = List<Etf>
typealias StockData = Pair<FBoerseHistoryData, FBoersePerfData>

val logError: (Throwable) -> Unit = { throwable -> Timber.e(throwable) }