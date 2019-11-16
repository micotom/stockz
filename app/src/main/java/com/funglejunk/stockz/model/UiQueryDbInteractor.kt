package com.funglejunk.stockz.model

import androidx.sqlite.db.SimpleSQLiteQuery
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraDbEtf
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.db.XetraEtfFlattenedDao
import io.reactivex.Single

class UiQueryDbInteractor {

    fun buildSqlStringFrom(query: UiEtfQuery): String {

        return if (query.isEmpty()) {
            ""
        } else {
            val joinClause = mutableListOf<String>()
            val whereClause = mutableListOf<String>()
            joinClause.add(
                "LEFT JOIN xetraetfpublisher ON ${XetraDbEtf.TABLE_NAME}.publ_id = xetraetfpublisher.rowid"
            )
            joinClause.add(
                "LEFT JOIN xetraetfbenchmark ON ${XetraDbEtf.TABLE_NAME}.bench_id = xetraetfbenchmark.rowid"
            )

            if (query.publisher != UiEtfQuery.PUBLISHER_EMPTY) {
                whereClause.add("xetraetfpublisher.name LIKE '%${query.publisher}%'")
            }
            if (query.benchmark != UiEtfQuery.BENCHMARK_EMPTY) {
                whereClause.add("xetraetfbenchmark.name LIKE '%${query.benchmark}%'")
            }

            if (query.name != UiEtfQuery.NAME_EMPTY) {
                whereClause.add("${XetraDbEtf.TABLE_NAME}.name LIKE '%${query.name}%'")
            }
            if (query.ter != UiEtfQuery.TER_MAX) {
                whereClause.add("${XetraDbEtf.TABLE_NAME}.ter <= ${query.ter}")
            }
            if (query.profitUse != UiEtfQuery.PROFIT_USE_EMPTY) {
                whereClause.add("${XetraDbEtf.TABLE_NAME}.profit_use LIKE '${query.profitUse}'")
            }
            if (query.replicationMethod != UiEtfQuery.REPLICATION_METHOD_EMPTY) {
                whereClause.add("${XetraDbEtf.TABLE_NAME}.repl_meth LIKE '${query.replicationMethod}'")
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

    fun executeSqlString(query: String, db: XetraDbInterface): Single<List<Etf>> {
        return if (query.isEmpty()) {
            db.etfFlattenedDao().getAll()
        } else {
            db.etfFlattenedDao().search(SimpleSQLiteQuery(query))
        }
    }
}
