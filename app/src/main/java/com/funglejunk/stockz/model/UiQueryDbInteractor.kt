package com.funglejunk.stockz.model

import androidx.sqlite.db.SimpleSQLiteQuery
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraEtf
import com.funglejunk.stockz.repo.db.XetraEtfFlattenedDao
import io.reactivex.Single
import timber.log.Timber
import java.lang.UnsupportedOperationException

class UiQueryDbInteractor() {

    fun buildSqlStringFrom(query: UiEtfQuery): String {

        return if (query.isEmpty()) {
            ""
        } else {
            val joinClause = mutableListOf<String>()
            val whereClause = mutableListOf<String>()

            /*
            joinClause.add(
                if (query.publisher != UiEtfQuery.PUBLISHER_EMPTY) {
                    "LEFT JOIN xetraetfpublisher ON xetraetf.publ_id = xetraetfpublisher.rowid AND " +
                            "xetraetfpublisher.name LIKE '%${query.publisher}%'"
                } else {
                    "LEFT JOIN xetraetfpublisher ON xetraetf.publ_id = xetraetfpublisher.rowid"
                }
            )

            joinClause.add(
                if (query.benchmark != UiEtfQuery.BENCHMARK_EMPTY) {
                    "LEFT JOIN xetraetfbenchmark ON xetraetf.bench_id = xetraetfbenchmark.rowid AND " +
                            "xetraetfbenchmark.name LIKE '%${query.benchmark}%'"
                } else {
                    "LEFT JOIN xetraetfbenchmark ON xetraetf.bench_id = xetraetfbenchmark.rowid"
                }
            )
             */
            joinClause.add(
                "LEFT JOIN xetraetfpublisher ON xetraetf.publ_id = xetraetfpublisher.rowid"
            )
            joinClause.add(
                "LEFT JOIN xetraetfbenchmark ON xetraetf.bench_id = xetraetfbenchmark.rowid"
            )

            if (query.publisher != UiEtfQuery.PUBLISHER_EMPTY) {
                whereClause.add("xetraetfpublisher.name LIKE '%${query.publisher}%'")
            }
            if (query.benchmark != UiEtfQuery.BENCHMARK_EMPTY) {
                whereClause.add("xetraetfbenchmark.name LIKE '%${query.benchmark}%'")
            }

            if (query.name != UiEtfQuery.NAME_EMPTY) {
                whereClause.add("xetraetf.name LIKE '%${query.name}%'")
            }
            if (query.ter != UiEtfQuery.TER_MAX) {
                whereClause.add("xetraetf.ter <= ${query.ter}")
            }
            if (query.profitUse != UiEtfQuery.PROFIT_USE_EMPTY) {
                whereClause.add("xetraetf.profit_use LIKE '${query.profitUse}'")
            }
            if (query.replicationMethod != UiEtfQuery.REPLICATION_METHOD_EMPTY) {
                whereClause.add("xetraetf.repl_meth LIKE '${query.replicationMethod}'")
            }

            val queryString = XetraEtfFlattenedDao.MAPPING_SELECT +
                    joinClause.joinToString(" ") +
                    when (whereClause.isNotEmpty()) {
                        true -> whereClause.joinToString(prefix = " WHERE ", separator = " AND ")
                        false -> ""
                    }
            queryString
        }

    }

    fun executeSqlString(query: String, db: XetraDb): Single<List<XetraEtfFlattened>> {
        return if (query.isEmpty()) {
            Timber.d("Getting all.")
            db.etfFlattenedDao().getAll()
        } else {
            Timber.d("sql query: $query")
            db.etfFlattenedDao().search(SimpleSQLiteQuery(query))
        }
    }
}
