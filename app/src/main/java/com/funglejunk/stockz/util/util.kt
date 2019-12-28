package com.funglejunk.stockz.util

import timber.log.Timber

val logError: (Throwable) -> Unit = { throwable -> Timber.e(throwable) }