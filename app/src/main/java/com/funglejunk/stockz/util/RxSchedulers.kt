package com.funglejunk.stockz.util

import io.reactivex.Scheduler

interface RxSchedulers {
    val mainThreadScheduler: Scheduler
    val ioScheduler: Scheduler
}
