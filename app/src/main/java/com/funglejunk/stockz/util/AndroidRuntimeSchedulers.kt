package com.funglejunk.stockz.util

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AndroidRuntimeSchedulers : RxSchedulers {
    override val mainThreadScheduler: Scheduler = AndroidSchedulers.mainThread()
    override val ioScheduler: Scheduler = Schedulers.io()
}