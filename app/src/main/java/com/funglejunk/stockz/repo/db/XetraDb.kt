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
import com.funglejunk.stockz.data.XetraEtfFlattened
import io.reactivex.Maybe
import io.reactivex.Single
import java.lang.RuntimeException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Database(
    entities = [
        XetraPerformanceEntry::class, XetraEtf::class, XetraEtfPublisher::class,
        XetraEtfBenchmark::class
    ],
    version = 4
)
abstract class XetraDb : RoomDatabase() {

    companion object {

        private val lock = ReentrantLock()
        private var db: XetraDb? = null

        fun init(context: Context) {
            return lock.withLock {
                if (db == null) {
                    db = Room.databaseBuilder(
                        context,
                        XetraDb::class.java, "xetra-db"
                    ).build()
                }
            }
        }

        fun get() = lock.withLock {
            db?.let {
                it
            } ?: {
                throw RuntimeException("Database not initialized")
            }()
        }
    }

    abstract fun perfDao(): XetraPerformanceEntryDao

    abstract fun etfDao(): XetraEtfInfoDao

    abstract fun publisherDao(): XetraEtfPublisherDao

    abstract fun benchmarkDao(): XetraEtfBenchmarkDao

    abstract fun etfFlattenedDao(): XetraEtfFlattenedDao
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
data class XetraEtf(
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
)

@Dao
interface XetraEtfFlattenedDao {

    companion object {
        const val MAPPING_SELECT =
            "SELECT xetraetf.name AS name, xetraetf.isin AS isin, xetraetf.symb AS symbol, " +
                    "xetraetf.listing AS listingDate, xetraetf.ter AS ter, xetraetf.profit_use AS profitUse, " +
                    "xetraetf.repl_meth AS replicationMethod, xetraetf.fund_curr AS fundCurrency, " +
                    "xetraetf.trade_curr AS tradingCurrency, xetraetfpublisher.name AS publisherName, " +
                    "xetraetfbenchmark.name AS benchmarkName " +
                    "FROM xetraetf "
    }

    @RawQuery
    fun search(query: SupportSQLiteQuery): Single<List<XetraEtfFlattened>>

    @Query(
        MAPPING_SELECT +
            "LEFT JOIN xetraetfpublisher ON xetraetf.publ_id = xetraetfpublisher.rowid " +
            "LEFT JOIN xetraetfbenchmark ON xetraetf.bench_id = xetraetfbenchmark.rowid"
    )
    fun getAll(): Single<List<XetraEtfFlattened>>

}

@Dao
interface XetraEtfInfoDao {

    @Query("SELECT COUNT(*) FROM xetraetf")
    fun getEntryCount(): Single<Int>

    @Insert
    fun insert(vararg publisher: XetraEtf): Array<Long>

}

@Entity
data class XetraEtfPublisher(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowid: Int = 0,
    @ColumnInfo(name = "name") val name: String
)

@Dao
interface XetraEtfPublisherDao {

    @Insert
    fun insert(vararg publisher: XetraEtfPublisher): Array<Long>

    @Query("SELECT * from xetraetfpublisher WHERE name LIKE (:name)")
    fun getPublisherByName(name: String): Single<XetraEtfPublisher>

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
    fun insert(vararg benchmark: XetraEtfBenchmark): Array<Long>

    @Query("SELECT * from xetraetfbenchmark WHERE name LIKE (:name)")
    fun getBenchmarkByName(name: String): Single<XetraEtfBenchmark>

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
