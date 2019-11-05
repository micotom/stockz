package com.funglejunk.stockz.model

import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraEtf
import io.reactivex.Single
import timber.log.Timber

class UiQueryDbInteractor(private val db: XetraDb) {

    fun dispatchQuery(query: UiEtfQuery): Single<List<XetraEtf>> {

        return when (query.isEmpty()) {
            true -> db.etfDao().getAll()
            false -> {
                when (query.name.isNotEmpty() && query.ter != UiEtfQuery.TER_MAX) {
                    true -> db.etfDao().queryNameAndTer(query.name, query.ter.toDouble())
                    false -> when (query.name.isEmpty() && query.ter != UiEtfQuery.TER_MAX) {
                        true -> db.etfDao().queryTer(query.ter.toDouble())
                        false -> {
                            Timber.e("Invalid query")
                            Single.just(emptyList())
                        }
                    }
                }
            }
        }

    }

}