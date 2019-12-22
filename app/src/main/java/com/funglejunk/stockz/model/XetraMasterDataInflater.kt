package com.funglejunk.stockz.model

import android.content.Context
import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.applicativeError.handleErrorWith
import arrow.fx.extensions.io.dispatchers.dispatchers
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.not
import com.funglejunk.stockz.repo.db.XetraDbEtf
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.db.XetraEtfBenchmark
import com.funglejunk.stockz.repo.db.XetraEtfPublisher

typealias ReadFromDiskResult = Triple<List<Etf>, Set<XetraEtfPublisher>, Set<XetraEtfBenchmark>>

class XetraMasterDataInflater(private val context: Context, private val db: XetraDbInterface) {

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

    class DbInflateException(t: Throwable) : Throwable("Error inflating: ${t.localizedMessage}")

    fun init(): IO<Either<DbInflateException, Unit>> = IO.fx {
        val needToInflate = not(isInflated()).bind()
        if (needToInflate) {
            val diskResult = effect {
                readFromDisk()
            }.bind()
            val (etfs, publishers, benchmarks) = diskResult
            inflateBenchmarks(benchmarks)
                .followedBy(
                    inflatePublishers(publishers)
                )
                .followedBy(
                    inflateEtfs(etfs)
                )
                .followedBy(
                    IO.just(Either.Right(Unit))
                ).bind()
        } else {
            Either.right(Unit)
        }
    }.handleErrorWith { t -> IO.just(Either.left(DbInflateException(t))) }

    private fun isInflated(): IO<Boolean> = IO { db.etfDao().getEntryCount() > 0 }

    private fun readFromDisk(): ReadFromDiskResult {
        val inputStream = context.assets.open("xetra_etf_datasheet.csv")
        val fileContent = inputStream.bufferedReader().use {
            it.readText()
        }
        val lines = fileContent.split("\n")
        return parseLines(lines)
    }

    private fun parseLines(lines: List<String>): ReadFromDiskResult {
        val cleanLines = lines.filter { it.isNotEmpty() }
        val rowsAndColumns = cleanLines.map { it.split(";") }
        val entries = rowsAndColumns.map { columns ->
            val publisher = XetraEtfPublisher(name = columns[PUBLISHER_INDEX])
            val benchmark = XetraEtfBenchmark(name = columns[BENCH_INDEX])
            val etf = Etf(
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
        }
        val etfs = entries.map { it.first }
        val publishers = entries.map { it.second }.toSet()
        val benchmarks = entries.map { it.third }.toSet()
        return Triple(etfs, publishers, benchmarks)
    }

    private fun inflatePublishers(publishers: Collection<XetraEtfPublisher>): IO<Array<Long>> =
        IO {
            db.publisherDao().insert(*(publishers.toTypedArray()))
        }

    private fun inflateBenchmarks(benchmarks: Collection<XetraEtfBenchmark>): IO<Array<Long>> =
        IO {
            db.benchmarkDao().insert(*(benchmarks.toTypedArray()))
        }

    private fun inflateEtfs(etfs: Collection<Etf>): IO<Array<Long>> = IO {
        val benchmarkDao = db.benchmarkDao()
        val publisherDao = db.publisherDao()
        val dbEtfs = etfs.map { etf ->
            val etfBenchmark = benchmarkDao.getBenchmarkByName(etf.benchmarkName)
            val publisher = publisherDao.getPublisherByName(etf.publisherName)
            XetraDbEtf(
                name = etf.name,
                isin = etf.isin,
                publisherId = publisher.rowid,
                symbol = etf.symbol,
                listingDate = etf.listingDate,
                ter = etf.ter,
                profitUse = etf.profitUse,
                replicationMethod = etf.replicationMethod,
                fundCurrency = etf.fundCurrency,
                tradingCurrency = etf.tradingCurrency,
                benchmarkId = etfBenchmark.rowid
            )
        }
        db.etfDao().insert(*(dbEtfs.toTypedArray()))
    }

    private fun String.formatPercentage() =
        replace(",", ".").replace("%", "").trim()
}
