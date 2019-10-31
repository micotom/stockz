package com.funglejunk.stockz.repo

import android.content.Context
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class XetraSymbols(private val context: Context) {

    private val lock = ReentrantLock()
    private var cache: Array<XetraSymbol>? = null

    fun get(): Single<Array<XetraSymbol>> = Single.fromCallable {
        lock.withLock {
            if (cache == null) {
                val fileContent = context.assets.open("t7-xetr-allTradableInstruments.csv")
                    .bufferedReader().use {
                        it.readText()
                    }
                val entries = fileContent.split("\n")
                    .filter {
                        it.isNotEmpty()
                    }.map { entry ->
                        val fields = entry.split(";")
                        XetraSymbol(fields[0], fields[1], fields[2], fields[3], fields[4])
                    }
                cache = entries.toTypedArray()
            }
            cache
        }
    }

    fun findEntryForTerm(term: String): XetraSymbol {
        Timber.d("Lookup ISIN for $term")
        return cache?.let {
            it.find {
                it.instrument.equals(term) || it.isin.equals(term) || it.product.equals(term)
                        || it.symbol.equals(term) || it.wkn.equals(term)
            } ?: {
                throw RuntimeException("No Xetra symbol for $term")
            }()
        } ?: {
            throw RuntimeException("Cache not initialized")
        }()
    }

}

data class XetraSymbol(
    val instrument: String,
    val isin: String,
    val wkn: String,
    val symbol: String,
    val product: String
)