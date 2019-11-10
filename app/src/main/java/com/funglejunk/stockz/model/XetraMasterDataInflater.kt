package com.funglejunk.stockz.model

import android.content.Context
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraEtf
import com.funglejunk.stockz.repo.db.XetraEtfBenchmark
import com.funglejunk.stockz.repo.db.XetraEtfPublisher
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

class XetraMasterDataInflater(private val context: Context, private val db: XetraDb) {

    private companion object {
        const val NAME_INDEX = 1
        const val ISIN_INDEX = 2
        const val PUBLISHER_INDEX = 3
        const val SYMBOL_INDEX = 4
        const val LISTING_DATE_INDEX = 7
        const val TER_INDEX = 10
        const val PROF_USE_INDEX = 11
        const val REPL_METHOD_INDEX = 12
        const val FUND_CURR_INDEX = 13
        const val TRADE_CURR_INDEX = 14
        const val BENCH_INDEX = 21
    }

    fun init(): Completable {
        return isInflated().flatMapCompletable { isInflated ->
            when (isInflated) {
                true -> Completable.complete()
                false -> readFromDisk().flatMapCompletable { (etfs, publishers, benchmarks) ->
                    inflateBenchmarks(benchmarks)
                        .doOnEvent { insertCount, _ ->
                            Timber.w("insert count for benchmarks: ${insertCount.size} vs. ${benchmarks.size}")
                        }
                        .flatMap {
                            inflatePublishers(publishers)
                        }
                        .doOnEvent { insertCount, _ ->
                            Timber.w("insert count for publishers: ${insertCount.size} vs. ${publishers.size}")
                        }
                        .flatMap {
                            inflateEtfs(etfs)
                        }
                        .doOnEvent { insertCount, _ ->
                            Timber.w("insert count for etfs: ${insertCount.size} vs. ${etfs.size}")
                        }
                        .flatMapCompletable {
                            Completable.complete()
                        }
                }
            }
        }
    }

    private fun isInflated(): Single<Boolean> {
        val etfDao = db.etfDao()
        return etfDao.getEntryCount().map { it > 0 }
    }

    private fun readFromDisk(): Single<Triple<List<XetraEtfFlattened>, Set<XetraEtfPublisher>, Set<XetraEtfBenchmark>>> {
        return Single.fromCallable {
            context.assets.open("xetra_etf_datasheet.csv").bufferedReader().use {
                it.readText()
            }
        }.map { fileContent ->
            fileContent.split("\n")
        }.flatMap { lines ->
            parseLines(lines)
        }
    }

    private fun parseLines(lines: List<String>):
            Single<Triple<List<XetraEtfFlattened>, Set<XetraEtfPublisher>, Set<XetraEtfBenchmark>>> {
        val publishers = mutableSetOf<XetraEtfPublisher>()
        val benchmarks = mutableSetOf<XetraEtfBenchmark>()
        val etfs = mutableListOf<XetraEtfFlattened>()
        return Observable.fromIterable(lines).filter {
            it.isNotEmpty()
        }.map { line ->
            line.split(";")
        }.map { columns ->
            val publisher = XetraEtfPublisher(name = columns[PUBLISHER_INDEX])
            val benchmark = XetraEtfBenchmark(name = columns[BENCH_INDEX])
            val etf = XetraEtfFlattened(
                name = columns[NAME_INDEX],
                isin = columns[ISIN_INDEX],
                symbol = columns[SYMBOL_INDEX],
                listingDate = columns[LISTING_DATE_INDEX],
                ter = columns[TER_INDEX].formatPercentage().toDouble(),
                profitUse = columns[PROF_USE_INDEX],
                replicationMethod = columns[REPL_METHOD_INDEX],
                fundCurrency = columns[FUND_CURR_INDEX],
                tradingCurrency = columns[TRADE_CURR_INDEX],
                publisherName = publisher.name,
                benchmarkName = benchmark.name
            )
            Triple(etf, publisher, benchmark)
        }.toList().flatMap { allContent ->
            Observable.fromIterable(allContent)
                .doOnNext { (etf, publisher, benchmark) ->
                    etfs.add(etf)
                    publishers.add(publisher)
                    benchmarks.add(benchmark)
                }
                .toList()
                .map { _ ->
                    Triple(etfs, publishers, benchmarks)
                }
        }
    }

    private fun inflatePublishers(publishers: Collection<XetraEtfPublisher>): Single<Array<Long>> {
        return Single.fromCallable {
            db.publisherDao().insert(*(publishers.toTypedArray()))
        }
    }

    private fun inflateBenchmarks(benchmarks: Collection<XetraEtfBenchmark>): Single<Array<Long>> {
        return Single.fromCallable {
            db.benchmarkDao().insert(*(benchmarks.toTypedArray()))
        }
    }

    private fun inflateEtfs(etfs: Collection<XetraEtfFlattened>): Single<Array<Long>> {
        val benchmarkDao = db.benchmarkDao()
        val publisherDao = db.publisherDao()
        return Observable.fromIterable(etfs)
            .flatMap { etf ->
                benchmarkDao.getBenchmarkByName(etf.benchmarkName).toObservable().map { benchmark ->
                    etf.publisherName to XetraEtf(
                        name = etf.name,
                        isin = etf.isin,
                        publisherId = -1,
                        symbol = etf.symbol,
                        listingDate = etf.listingDate,
                        ter = etf.ter,
                        profitUse = etf.profitUse,
                        replicationMethod = etf.replicationMethod,
                        fundCurrency = etf.fundCurrency,
                        tradingCurrency = etf.tradingCurrency,
                        benchmarkId = benchmark.rowid
                    )
                }
            }
            .flatMap { (publisherName, etf) ->
                publisherDao.getPublisherByName(publisherName).toObservable()
                    .map {
                        etf.copy(publisherId = it.rowid)
                    }
            }
            .toList()
            .flatMap { finalEtfs ->
                Single.fromCallable {
                    db.etfDao().insert(*(finalEtfs.toTypedArray()))
                }
            }
    }

    private fun String.formatPercentage() =
        replace(",", ".").replace("%", "").trim()
}
