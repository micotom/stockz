package com.funglejunk.stockz

import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rxResponseString
import java.io.File
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern


fun main() {

    val countDownLatch = CountDownLatch(1)

    FBoerseRepo().getHistory(
        "IE00BKX55T58",
        LocalDate.of(2014, 1, 1),
        LocalDate.of(2018, 1, 1)
    ).doFinally {
        countDownLatch.countDown()
    }.subscribe { data -> println(data) }

    countDownLatch.await()
}