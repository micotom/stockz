package com.funglejunk.stockz.model.portfolio

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.time.LocalDate

typealias Isin = String

@Parcelize
data class AssetSummary(
    val isin: Isin,
    val currentSharePrice: BigDecimal,
    val buys: Set<AssetBuy>,
    val targetAllocationPercent: Double
) : Parcelable {

    val shares: Double = buys.fold(0.0) { acc, new ->
        acc + new.shares
    }

    val nrOfOrders: Int = buys.size

    val currentTotalValueNE = shares.toBigDecimal() * currentSharePrice

    val totalExpenses: BigDecimal = buys.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.expenses
    }

    val currentTotalValueWE = currentTotalValueNE - totalExpenses

    val totalBuyPriceNE: BigDecimal = buys.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.shares.toBigDecimal() * new.pricePerShare
    }

    val profitEuroNE = currentTotalValueNE - totalBuyPriceNE

    val totalBuyPriceWE = buys.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.shares.toBigDecimal() * new.pricePerShare + new.expenses
    }

    val profitPercentNE =
        currentTotalValueNE / (totalBuyPriceNE / BigDecimal.valueOf(100.0)) - BigDecimal.valueOf(
            100.0
        )

    val profitEuroWE = currentTotalValueNE - totalBuyPriceWE

    val profitPercentWE =
        currentTotalValueNE / (totalBuyPriceWE / BigDecimal.valueOf(100.0)) - BigDecimal.valueOf(
            100.0
        )

    @Parcelize
    data class AssetBuy(
        val date: LocalDate,
        val shares: Double,
        val pricePerShare: BigDecimal,
        val expenses: BigDecimal
    ) : Parcelable

    override fun toString(): String {
        return "AssetSummary(isin='$isin', currentSharePrice=$currentSharePrice, buys=$buys, targetAllocationPercent=$targetAllocationPercent, shares=$shares, currentTotalValueNE=$currentTotalValueNE, totalBuyPriceNE=$totalBuyPriceNE, profitEuroNE=$profitEuroNE, profitPercentNE=$profitPercentNE, totalExpenses=$totalExpenses, currentTotalValueWE=$currentTotalValueWE, totalBuyPriceWE=$totalBuyPriceWE, profitEuroWE=$profitEuroWE, profitPercentWE=$profitPercentWE)"
    }
}