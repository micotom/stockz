package com.funglejunk.stockz.model

import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.mapToDrawableData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AlgorithmsKtTest {

    private val json = Json(JsonConfiguration.Stable)

    private val data: FBoerseHistoryData

    init {
        println(System.getProperty("user.dir"))
        val dataSetRaw = File("./src/test/res/sample_repo_data.json").readText()
        data = json.parse(FBoerseHistoryData.serializer(), dataSetRaw)
    }

    @Test
    fun `calculate sma`() {
        val templateEntry = FBoerseHistoryData.Data(
            "2018-01-01", -1.0, -1.0, -1.0, -1.0, -1, -1.0
        )
        val content = listOf<FBoerseHistoryData.Data>(
            templateEntry.copy(date = "2018-01-01", close = 50.0),
            templateEntry.copy(date = "2018-01-15", close = 10.0), // 30
            templateEntry.copy(date = "2018-02-01", close = 20.0),
            templateEntry.copy(date = "2018-02-15", close = 40.0), // 30
            templateEntry.copy(date = "2018-03-05", close = 0.0),
            templateEntry.copy(date = "2018-03-15", close = 10.0) // 5
        )
        val fooData = FBoerseHistoryData(
            "foo", content, content.size, true
        )
        val sma = sma(fooData.mapToDrawableData(), Period.DAYS_30, 1)
        assertEquals(3, sma.size)
        assertEquals(30f, sma[0].value)
        assertEquals(30f, sma[1].value)
        assertEquals(5f, sma[2].value)
    }

}