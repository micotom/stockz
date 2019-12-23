package com.funglejunk.stockz.repo.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.funglejunk.stockz.data.Etf
import io.reactivex.Maybe
import io.reactivex.Single
import java.lang.RuntimeException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Database(
    entities = [
        XetraPerformanceEntry::class, XetraDbEtf::class, XetraEtfPublisher::class,
        XetraEtfBenchmark::class
    ],
    version = 4
)
abstract class XetraDb : RoomDatabase(), XetraDbInterface {

    companion object {
        private val initLock = ReentrantLock()
        private var isCreated = false

        fun create(context: Context): XetraDbInterface =
            initLock.withLock {
                if (isCreated) {
                    throw RuntimeException("XetraDb must not be initialized outside di framework")
                }
                isCreated = true
                Room.databaseBuilder(
                    context,
                    XetraDb::class.java, "xetra-db"
                ).build()
            }
    }

    abstract override fun perfDao(): XetraPerformanceEntryDao

    abstract override fun etfDao(): XetraEtfInfoDao

    abstract override fun publisherDao(): XetraEtfPublisherDao

    abstract override fun benchmarkDao(): XetraEtfBenchmarkDao

    abstract override fun etfFlattenedDao(): XetraEtfFlattenedDao
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = XetraEtfPublisher::class,
            childColumns = ["publ_id"],
            parentColumns = ["rowid"]
        ),
        ForeignKey(
            entity = XetraEtfBenchmark::class,
            childColumns = ["bench_id"],
            parentColumns = ["rowid"]
        )
    ]
)
data class XetraDbEtf(
    @ColumnInfo(name = "name") val name: String,
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "isin") val isin: String,
    @ColumnInfo(name = "publ_id") val publisherId: Int,
    @ColumnInfo(name = "symb") val symbol: String,
    @ColumnInfo(name = "listing") val listingDate: String,
    @ColumnInfo(name = "ter") val ter: Double,
    @ColumnInfo(name = "profit_use") val profitUse: String,
    @ColumnInfo(name = "repl_meth") val replicationMethod: String,
    @ColumnInfo(name = "fund_curr") val fundCurrency: String,
    @ColumnInfo(name = "trade_curr") val tradingCurrency: String,
    @ColumnInfo(name = "bench_id") val benchmarkId: Int
) {
    companion object {
        const val TABLE_NAME = "xetradbetf"
    }
}

@Dao
interface XetraEtfFlattenedDao {

    companion object {
        const val MAPPING_SELECT =
            "SELECT " +
                    "${XetraDbEtf.TABLE_NAME}.name AS name, " +
                    "${XetraDbEtf.TABLE_NAME}.isin AS isin, " +
                    "${XetraDbEtf.TABLE_NAME}.symb AS symbol, " +
                    "${XetraDbEtf.TABLE_NAME}.listing AS listingDate, " +
                    "${XetraDbEtf.TABLE_NAME}.ter AS ter, " +
                    "${XetraDbEtf.TABLE_NAME}.profit_use AS profitUse, " +
                    "${XetraDbEtf.TABLE_NAME}.repl_meth AS replicationMethod, " +
                    "${XetraDbEtf.TABLE_NAME}.fund_curr AS fundCurrency, " +
                    "${XetraDbEtf.TABLE_NAME}.trade_curr AS tradingCurrency, " +
                    "xetraetfpublisher.name AS publisherName, " +
                    "xetraetfbenchmark.name AS benchmarkName " +
                    "FROM ${XetraDbEtf.TABLE_NAME} "
    }

    @RawQuery
    suspend fun search(query: SupportSQLiteQuery): List<Etf>

    @Query(
        MAPPING_SELECT +
                "LEFT JOIN xetraetfpublisher ON xetradbetf.publ_id = xetraetfpublisher.rowid " +
                "LEFT JOIN xetraetfbenchmark ON xetradbetf.bench_id = xetraetfbenchmark.rowid"
    )
    suspend fun getAll(): List<Etf>

    @Query(
        MAPPING_SELECT +
                "LEFT JOIN xetraetfpublisher ON xetradbetf.publ_id = xetraetfpublisher.rowid " +
                "LEFT JOIN xetraetfbenchmark ON xetradbetf.bench_id = xetraetfbenchmark.rowid"
    )
    fun getAllDeprecated(): Single<List<Etf>>
}

@Dao
interface XetraEtfInfoDao {

    @Query("SELECT COUNT(*) FROM xetradbetf")
    suspend fun getEntryCount(): Int

    @Insert
    suspend fun insert(vararg publisher: XetraDbEtf): Array<Long>
}

@Entity
data class XetraEtfPublisher(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowid: Int = 0,
    @ColumnInfo(name = "name") val name: String
)

@Dao
interface XetraEtfPublisherDao {

    @Insert
    suspend fun insert(vararg publisher: XetraEtfPublisher): Array<Long>

    @Query("SELECT * from xetraetfpublisher WHERE name LIKE (:name)")
    suspend fun getPublisherByName(name: String): XetraEtfPublisher

    @Query("SELECT * from xetraetfpublisher WHERE rowid LIKE (:id) LIMIT 1")
    fun getPublisherById(id: Int): Single<XetraEtfPublisher>

    @Query("SELECT * from xetraetfpublisher")
    fun getAll(): Single<List<XetraEtfPublisher>>
}

@Entity
data class XetraEtfBenchmark(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowid: Int = 0,
    @ColumnInfo(name = "name") val name: String
)

@Dao
interface XetraEtfBenchmarkDao {

    @Insert
    suspend fun insert(vararg benchmark: XetraEtfBenchmark): Array<Long>

    @Query("SELECT * from xetraetfbenchmark WHERE name LIKE (:name)")
    suspend fun getBenchmarkByName(name: String): XetraEtfBenchmark

    @Query("SELECT * from xetraetfbenchmark WHERE rowid LIKE (:id) LIMIT 1")
    fun getBenchmarkById(id: Int): Single<XetraEtfBenchmark>

    @Query("SELECT * from xetraetfbenchmark")
    fun getAll(): Single<List<XetraEtfBenchmark>>
}

@Entity(primaryKeys = ["isin", "date"])
data class XetraPerformanceEntry(
    @ColumnInfo(name = "isin") val isin: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "closePrice") val value: Double
)

@Dao
interface XetraPerformanceEntryDao {

    @Query("SELECT * FROM xetraperformanceentry WHERE isin LIKE (:isin)")
    fun getAllEntriesForIsin(isin: String): Single<List<XetraPerformanceEntry>>

    @Query("SELECT * FROM xetraperformanceentry WHERE isin LIKE (:isin) ORDER BY date DESC LIMIT 1")
    fun getNewestEntryForIsin(isin: String): Maybe<XetraPerformanceEntry>

    @Query("SELECT COUNT(*) FROM xetraperformanceentry WHERE isin LIKE (:isin) AND date LIKE (:date)")
    fun entryCount(isin: String, date: String): Single<Int>

    @Insert
    fun insert(vararg isinEntries: XetraPerformanceEntry)

    @Delete
    fun delete(isinEntry: XetraPerformanceEntry)
}
