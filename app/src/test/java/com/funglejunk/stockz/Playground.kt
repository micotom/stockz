package com.funglejunk.stockz

import com.funglejunk.stockz.repo.fboerse.FBoerseRepoImpl
import java.time.LocalDate
import java.util.concurrent.CountDownLatch


fun main() {

    val countDownLatch = CountDownLatch(1)

    FBoerseRepoImpl().getHistory(
        "IE00BKX55T58",
        LocalDate.of(2014, 1, 1),
        LocalDate.of(2018, 1, 1)
    ).doFinally {
        countDownLatch.countDown()
    }.subscribe { data -> println(data) }

    countDownLatch.await()
}