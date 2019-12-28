package com.funglejunk.stockz.util

import arrow.fx.IO
import timber.log.Timber

val logError: (Throwable) -> Unit = { throwable -> Timber.e(throwable) }