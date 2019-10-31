package com.funglejunk.stockz.repo

import android.content.Context
import io.reactivex.Single
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AndroidAssetReader(private val context: Context) : AssetReader {

    private val lock = ReentrantLock()
    private var cachedTickers: Array<String>? = null

    override fun getTickerSymbols(): Single<Array<String>> {
        return Single.fromCallable {
            lock.withLock {
                if (cachedTickers == null) {
                    val tickers = context.assets.open("wtd_tickers.txt").bufferedReader().use {
                        it.readText()
                    }
                    val split = tickers.split(",").toSet()
                        .map { it.replace("\"", "") }.map { it.trim() }.toTypedArray()
                    cachedTickers = split
                }
                cachedTickers!!
            }
        }
    }

}