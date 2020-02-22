package com.funglejunk.stockz.model.portfolio

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

// TODO add id
@Parcelize
data class PortfolioSummary(
    val assets: Set<AssetSummary>
) : Parcelable {

    val currentValueEuroNE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.currentTotalValueNE
    }

    val currentValueEuroWE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.currentTotalValueWE
    }

    val buyPriceEuroNE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.totalBuyPriceNE
    }

    val buyPriceEuroWE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.totalBuyPriceWE
    }

    val profitEuroNE: BigDecimal = currentValueEuroNE - buyPriceEuroNE

    val profitPercentNE: Double =
        (profitEuroNE / (buyPriceEuroNE / 100.0.toBigDecimal())).toDouble()

    val profitEuroWE: BigDecimal = currentValueEuroNE - buyPriceEuroWE

    val profitPercentWE: Double =
        (profitEuroWE / (buyPriceEuroWE / 100.0.toBigDecimal())).toDouble()

    val allocationInfo: Map<AssetSummary, AllocationInfo> =
        assets.map { asset ->
            val actualSharePercNE = asset.currentTotalValueNE / currentValueEuroNE * BigDecimal.valueOf(100.0)
            val diffPercentNE = actualSharePercNE.toDouble() - asset.targetAllocationPercent
            val diffEuroNE = currentValueEuroNE * (diffPercentNE / 100.0).toBigDecimal()

            val actualSharePercWE = asset.currentTotalValueWE / currentValueEuroWE * BigDecimal.valueOf(100.0)
            val diffPercentWE = actualSharePercWE.toDouble() - asset.targetAllocationPercent
            val diffEuroWE = currentValueEuroWE * (diffPercentWE / 100.0).toBigDecimal()

            val info = AllocationInfo(
                diffEuroNE,
                diffPercentNE,
                diffEuroWE,
                diffPercentWE
            )
            asset to info
        }.toMap()

    data class AllocationInfo(
        val differenceEuroNE: BigDecimal,
        val differencePercentNE: Double,
        val differenceEuroWE: BigDecimal,
        val differencePercentWE: Double
    )

    override fun toString(): String {
        return "PortfolioSummary(assets=$assets, currentValueEuroNE=$currentValueEuroNE, currentValueEuroWE=$currentValueEuroWE, buyValueEuroNE=$buyPriceEuroNE, buyValueEuroWE=$buyPriceEuroWE, profitEuroNE=$profitEuroNE, profitPercentNE=$profitPercentNE, allocationInfo=$allocationInfo)"
    }
}