package com.funglejunk.stockz.model

import androidx.sqlite.db.SimpleSQLiteQuery
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraEtf
import io.reactivex.Single
import timber.log.Timber

class UiQueryDbInteractor(private val db: XetraDb) {

    fun dispatchQuery(query: UiEtfQuery): Single<List<XetraEtf>> {

        return if (query.isEmpty()) {
            db.etfDao().getAll()
        } else {
            val criteria = mutableListOf<String>()
            if (query.name.isNotEmpty()) {
                criteria.add("name LIKE '%${query.name}%'")
            }
            if (query.ter != UiEtfQuery.TER_MAX) {
                criteria.add("ter <= ${query.ter}")
            }
            val queryString = "SELECT * FROM xetraetf WHERE " +
                    criteria.joinToString(separator = " AND ")
            Timber.d("sql query: $queryString")
            db.etfDao().search(
                SimpleSQLiteQuery(queryString)
            )
        }

    }

}