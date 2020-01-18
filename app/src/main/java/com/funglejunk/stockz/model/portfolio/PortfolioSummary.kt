package com.funglejunk.stockz.model.portfolio

import java.math.BigDecimal

data class PortfolioSummary(
    val assets: Set<AssetSummary>
) {
    val currentValueEuroNE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.currentTotalValueNE
    }

    val currentValueEuroWE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.currentTotalValueWE
    }

    val buyValueEuroNE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.totalBuyPriceNE
    }

    val buyValueEuroWE: BigDecimal = assets.fold(BigDecimal.ZERO) { acc, new ->
        acc + new.totalBuyPriceWE
    }

    val profitEuroNE: BigDecimal = currentValueEuroNE - buyValueEuroNE

    val profitPercentNE: BigDecimal =
        profitEuroNE / (buyValueEuroNE / BigDecimal.valueOf(100.0))

    val profitEuroWE: BigDecimal = currentValueEuroWE - buyValueEuroWE

    val profitPercentWE: BigDecimal =
        profitEuroWE / (buyValueEuroWE / BigDecimal.valueOf(100.0))

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
        return "PortfolioSummary(assets=$assets, currentValueEuroNE=$currentValueEuroNE, currentValueEuroWE=$currentValueEuroWE, buyValueEuroNE=$buyValueEuroNE, buyValueEuroWE=$buyValueEuroWE, profitEuroNE=$profitEuroNE, profitPercentNE=$profitPercentNE, allocationInfo=$allocationInfo)"
    }
}