package com.funglejunk.stockz.model

import android.content.Context
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.not
import com.funglejunk.stockz.repo.db.XetraDbEtf
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.db.XetraEtfBenchmark
import com.funglejunk.stockz.repo.db.XetraEtfPublisher

typealias ReadFromDiskResult = Triple<List<Etf>, Set<XetraEtfPublisher>, Set<XetraEtfBenchmark>>
typealias DbInsertionResult = Array<Long>

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

    val init: () -> IO<Unit> = {
        IO.fx {
            val needToInflate = not(isInflated()).bind()
            if (needToInflate) {
                val diskResult = readFromDisk().bind()
                inflateDiskReadings(diskResult).bind()
            }
        }
    }

    private val inflateDiskReadings: (ReadFromDiskResult) -> IO<Unit> =
        { diskResult ->
            IO.fx {
                val (etfs, publishers, benchmarks) = diskResult
                inflateBenchmarks(benchmarks) // TODO validate
                    .followedBy(
                        inflatePublishers(publishers) // TODO validate
                    )
                    .followedBy(
                        inflateEtfs(etfs) // TODO validate
                    ).map {
                        it.map { Unit }
                    }.bind()
                Unit
            }
        }

    private val isInflated: () -> IO<Boolean> = {
        IO { db.etfDao().getEntryCount() > 0 }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private val readFromDisk: () -> IO<ReadFromDiskResult> = {
        IO {
            context.assets.open("xetra_etf_datasheet.csv")
        }.bracket(
            release = { IO.invoke { it.close() } },
            use = { inputStream ->
                IO {
                    inputStream.bufferedReader()
                }.bracket(
                    release = { IO.invoke { it.close() } },
                    use = { reader ->
                        val fileContent = reader.readText()
                        val lines = fileContent.split("\n")
                        IO.fx {
                            effect {
                                parseLines(lines)
                            }.bind()
                        }
                    }
                )
            }
        )
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

    private val inflatePublishers: (Collection<XetraEtfPublisher>) -> IO<DbInsertionResult> =
        { publishers ->
            IO {
                db.publisherDao().insert(*(publishers.toTypedArray()))
            }
        }

    private val inflateBenchmarks: (Collection<XetraEtfBenchmark>) -> IO<DbInsertionResult> =
        { benchmarks ->
            IO {
                db.benchmarkDao().insert(*(benchmarks.toTypedArray()))
            }
        }

    private val inflateEtfs: (Collection<Etf>) -> IO<DbInsertionResult> = { etfs ->
        IO {
            val dbEtfs = etfs.map { etf ->
                val etfBenchmark = db.benchmarkDao().getBenchmarkByName(etf.benchmarkName)
                val publisher = db.publisherDao().getPublisherByName(etf.publisherName)
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
    }

    private fun String.formatPercentage() =
        replace(",", ".").replace("%", "").trim()
}
