package com.funglejunk.stockz.util

import com.funglejunk.stockz.data.Etf
import timber.log.Timber

typealias EtfList = List<Etf>

val logError: (Throwable) -> Unit = { throwable -> Timber.e(throwable) }