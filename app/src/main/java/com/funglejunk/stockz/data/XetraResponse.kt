package com.funglejunk.stockz.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XetraResponse(
    val data: List<XetraDayData>
)

@Serializable
data class XetraDayData(
    @SerialName("Isin")
    val isin: String,
    @SerialName("Mnemonic")
    val mnemonic: String,
    @SerialName("SecurityDesc")
    val securityDesc: String,
    @SerialName("SecurityType")
    val securityType: String,
    @SerialName("Currency")
    val currency: String,
    @SerialName("SecurityID")
    val securityID: Int,
    @SerialName("Date")
    val date: String,
    @SerialName("Time")
    val time: String,
    @SerialName("StartPrice")
    val startPrice: Double,
    @SerialName("MaxPrice")
    val maxPrice: Double,
    @SerialName("MinPrice")
    val minPrice: Double,
    @SerialName("EndPrice")
    val endPrice: Double,
    @SerialName("TradedVolume")
    val tradedVolume: Int,
    @SerialName("NumberOfTrades")
    val numberOfTrades: Int
) {

    companion object {
        const val NAN = -1.0
        val INVALID = XetraDayData(
            isin = "", mnemonic = "", securityDesc = "",
            securityType = "", currency = "", securityID = -1,
            date = "", time = "-1", startPrice = NAN,
            maxPrice = NAN, minPrice = NAN,
            endPrice = NAN, tradedVolume = -1,
            numberOfTrades = -1
        )
    }

}