package com.funglejunk.stockz.repo.db

import android.content.Context
import android.content.SharedPreferences
import arrow.core.Option
import arrow.core.toOption
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

class StockDataCache(context: Context) : StockDataCacheInterface {

    private companion object {
        const val PREFS_NAME = "com.funglejunk.stockz.historydata"
        val json = Json(JsonConfiguration.Stable)
    }

    private val dataLayer: IO<SharedPreferences> by lazy {
        IO.invoke(Dispatchers.IO) {
            context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE
            )
        }
    }

    override fun persist(data: CacheableData): IO<Boolean> = IO.fx {
        continueOn(Dispatchers.IO)
        val sharedPref = dataLayer.bind()
        with(sharedPref.edit()) {
            when (val staleValue = sharedPref.getString(data.key, null)) {
                null -> putNewValue(data)
                else -> updateOldValue(staleValue, data)
            }
            commit()
        }
    }

    private fun SharedPreferences.Editor.updateOldValue(
        staleValue: String,
        data: CacheableData
    ) {
        val staleData = json.parse(FBoerseHistoryData.serializer(), staleValue)
        val mergedData = staleData.merge(data.value)
        if (mergedData.content.size != staleData.content.size) {
            putString(
                data.key,
                json.stringify(FBoerseHistoryData.serializer(), mergedData)
            )
        }
    }

    private fun SharedPreferences.Editor.putNewValue(data: CacheableData) {
        putString(
            data.key,
            json.stringify(FBoerseHistoryData.serializer(), data.value)
        )
    }

    override fun get(key: String): IO<Option<FBoerseHistoryData>> = IO.fx {
        continueOn(Dispatchers.IO)
        val sharedPref = dataLayer.bind()
        sharedPref.getString(key, null).toOption().map {
            json.parse(FBoerseHistoryData.serializer(), it)
        }
    }

    private fun FBoerseHistoryData.merge(other: FBoerseHistoryData): FBoerseHistoryData {
        val thisContent = this.content
        val otherContent = other.content
        val merged = thisContent.mergeWith(otherContent)
        return this.copy(
            content = merged,
            totalCount = merged.size
        )
    }

    private fun List<FBoerseHistoryData.Data>.mergeWith(other: List<FBoerseHistoryData.Data>):
            List<FBoerseHistoryData.Data> =
        mutableListOf<FBoerseHistoryData.Data>().apply {
            addAll(this@mergeWith)
            addAll(
                other.filter { !this.contains(it) }
            )
        }

}