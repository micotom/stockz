package com.funglejunk.stockz.model

import com.funglejunk.stockz.data.UiEtfQuery
import org.junit.Test

class UiQueryDbInteractorTest {

    @Test
    fun buildSqlString() {

        val query = UiEtfQuery(
            name = "Vanguard", ter = UiEtfQuery.TER_MAX, replicationMethod = UiEtfQuery.REPLICATION_METHOD_EMPTY,
            profitUse = UiEtfQuery.PROFIT_USE_EMPTY, publisher = "iShares", benchmark = UiEtfQuery.BENCHMARK_EMPTY
        )

        val sqlString = UiQueryDbInteractor().buildSqlStringFrom(query)

        println(sqlString)
    }
}
