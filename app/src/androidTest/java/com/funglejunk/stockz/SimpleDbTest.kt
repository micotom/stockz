package com.funglejunk.stockz

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.repo.db.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SimpleDbTest {

    private lateinit var etfDao: XetraEtfInfoDao
    private lateinit var etfFlatDao: XetraEtfFlattenedDao
    private lateinit var benchDao: XetraEtfBenchmarkDao
    private lateinit var pubDao: XetraEtfPublisherDao
    private lateinit var db: XetraDb

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, XetraDb::class.java
        ).build()
        etfDao = db.etfDao()
        etfFlatDao = db.etfFlattenedDao()
        benchDao = db.benchmarkDao()
        pubDao = db.publisherDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun run() {
        val bench =
            TestUtil.createBenchmark()
        val pub = TestUtil.createPublisher()
        IO.fx {
            !effect {
                val pubId = pubDao.insert(pub).first()
                val benchId = benchDao.insert(bench).first()
                val isin = "IE00B3VVMM84"
                val etf = TestUtil.createEtf(isin, pubId.toInt(), benchId.toInt())
                etfDao.insert(etf)
                assertEquals(1, etfDao.getEntryCount())
                val fetchedEtf = etfFlatDao.getEtfWithIsin(isin)
                assertNotNull(fetchedEtf)
            }
        }.unsafeRunSync()
    }

    object TestUtil {

        fun createEtf(isin: String, publId: Int, benchId: Int): XetraDbEtf = XetraDbEtf(
            name = "MyEtf",
            isin = isin,
            publisherId = publId,
            symbol = "SYMB",
            listingDate = "2019-01-01",
            ter = 0.5,
            profitUse = "Accumulating",
            replicationMethod = "Physical",
            fundCurrency = "EUR",
            tradingCurrency = "EUR",
            benchmarkId = benchId
        )

        fun createBenchmark(): XetraEtfBenchmark = XetraEtfBenchmark(
            name = "MSCI World"
        )

        fun createPublisher(): XetraEtfPublisher = XetraEtfPublisher(
            name = "iShares"
        )

    }

}