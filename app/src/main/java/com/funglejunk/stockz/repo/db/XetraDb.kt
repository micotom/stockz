package com.funglejunk.stockz.repo.db

import android.content.Context
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import androidx.sqlite.db.SupportSQLiteQuery
import com.funglejunk.stockz.data.Etf
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

//region Db
@Database(
    entities = [
        XetraDbEtf::class, XetraEtfPublisher::class, XetraEtfBenchmark::class, XetraFavourite::class,
        PortfolioEntry::class
    ],
    version = 5
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

    abstract override fun etfDao(): XetraEtfInfoDao

    abstract override fun publisherDao(): XetraEtfPublisherDao

    abstract override fun benchmarkDao(): XetraEtfBenchmarkDao

    abstract override fun etfFlattenedDao(): XetraEtfFlattenedDao

    abstract override fun favouritesDao(): XetraFavouriteDao

    abstract override fun portfolioDao(): PortfolioEntriesDao
}
//endregion

//region Favourites
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = XetraDbEtf::class,
            childColumns = ["isin"],
            parentColumns = ["isin"]
        )
    ]
)
data class XetraFavourite(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "isin") val isin: String
)

@Dao
interface XetraFavouriteDao {
    @Insert
    suspend fun insert(favourite: XetraFavourite): Long

    @Query(
        "${XetraEtfFlattenedDao.MAPPING_SELECT} " +
                "LEFT JOIN xetraetfpublisher ON xetradbetf.publ_id = xetraetfpublisher.rowid " +
                "LEFT JOIN xetraetfbenchmark ON xetradbetf.bench_id = xetraetfbenchmark.rowid " +
                "WHERE xetradbetf.isin IN xetrafavourite"
    )
    suspend fun getAll(): List<Etf>

    @Query("SELECT COUNT(*) FROM xetrafavourite WHERE isin = :etfIsin")
    suspend fun getRecordCount(etfIsin: String): Long

    @Delete
    suspend fun removeItem(record: XetraFavourite): Int
}
//endregion


//region Etf
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

}

@Dao
interface XetraEtfInfoDao {

    @Query("SELECT COUNT(*) FROM xetradbetf")
    suspend fun getEntryCount(): Int

    @Insert
    suspend fun insert(vararg publisher: XetraDbEtf): Array<Long>
}
//endregion

//region Publisher
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

    @Query("SELECT * from xetraetfpublisher")
    suspend fun getAll(): List<XetraEtfPublisher>
}
//endregion

//region Benchmark
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

    @Query("SELECT * from xetraetfbenchmark")
    suspend fun getAll(): List<XetraEtfBenchmark>
}
//endregion

//region portfolio
@Entity
data class PortfolioEntry(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "isin") val isin: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amnt") val amount: Double,
    @ColumnInfo(name = "price") val price: Double
)

@Dao
interface PortfolioEntriesDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(entry: PortfolioEntry): Long

    @Query("SELECT * from portfolioentry")
    suspend fun getAll(): List<PortfolioEntry>

    @Query("SELECT * from portfolioentry WHERE isin LIKE (:isin)")
    suspend fun getEntryWithIsin(isin: String): List<PortfolioEntry>

    @Query("SELECT COUNT(*) from portfolioentry WHERE isin LIKE (:isin)")
    suspend fun getEntryCountForIsin(isin: String): Long
}
//endregion