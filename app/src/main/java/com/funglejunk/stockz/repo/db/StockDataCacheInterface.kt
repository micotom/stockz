package com.funglejunk.stockz.repo.db

import arrow.core.Option
import arrow.fx.IO
import com.funglejunk.stockz.data.RepoHistoryData

interface StockDataCacheInterface {
    fun persist(data: CacheableData): IO<Boolean>
    fun get(key: String): IO<Option<RepoHistoryData>>
}

data class CacheableData(val key: String, val value: RepoHistoryData)