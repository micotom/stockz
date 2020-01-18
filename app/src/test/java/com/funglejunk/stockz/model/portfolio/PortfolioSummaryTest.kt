package com.funglejunk.stockz.model.portfolio

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PortfolioSummaryTest {

    @Test
    fun test() {

        val buy = AssetSummary.AssetBuy(
            date = LocalDate.of(2019, 10, 4),
            shares = 12.0,
            pricePerShare = 56.840.toBigDecimal(),
            expenses = 6.4.toBigDecimal()
        )

        val assetSummary = AssetSummary(
            isin = "IE00BKX55T58",
            currentSharePrice = 63.64.toBigDecimal(),
            buys = setOf(buy),
            targetAllocationPercent = 60.0
        )

        val portfolioSummary = PortfolioSummary(
            setOf(assetSummary)
        )

        val expBuyValueNE = buy.shares.toBigDecimal() * buy.pricePerShare
        val expBuyValueWE = buy.shares.toBigDecimal() * buy.pricePerShare + buy.expenses
        val expCurrentValueEuroNE = buy.shares.toBigDecimal() * assetSummary.currentSharePrice
        val expCurrentValueEuroWE =
            buy.shares.toBigDecimal() * assetSummary.currentSharePrice - assetSummary.totalExpenses
        val expProfitEuroNE = expCurrentValueEuroNE - expBuyValueNE
        val expProfitEuroWE = expCurrentValueEuroWE - expBuyValueWE
        val expProfitPercNE = expProfitEuroNE / (expBuyValueNE / BigDecimal.valueOf(100.0))
        val expProfitPercWE = expProfitEuroWE / (expBuyValueWE / BigDecimal.valueOf(100.0))

        assertEquals(expBuyValueNE, portfolioSummary.buyValueEuroNE)
        assertEquals(expBuyValueWE, portfolioSummary.buyValueEuroWE)
        assertEquals(expCurrentValueEuroNE, portfolioSummary.currentValueEuroNE)
        assertEquals(expCurrentValueEuroWE, portfolioSummary.currentValueEuroWE)
        assertEquals(expProfitEuroNE, portfolioSummary.profitEuroNE)
        assertEquals(expProfitEuroWE, portfolioSummary.profitEuroWE)
        assertEquals(expProfitPercNE, portfolioSummary.profitPercentNE)
        assertEquals(expProfitPercWE, portfolioSummary.profitPercentWE)

        assertEquals(1, portfolioSummary.allocationInfo.size)
        val (assetSummary2, allocationInfo) = portfolioSummary.allocationInfo.entries.first()

        val actualShareEuroNE = buy.shares.toBigDecimal() * assetSummary2.currentSharePrice
        val actualSharePercNE =
            assetSummary2.currentTotalValueNE / portfolioSummary.currentValueEuroNE * BigDecimal.valueOf(
                100.0
            )
        val targetShareEuroNE = portfolioSummary.currentValueEuroNE *
                (assetSummary2.targetAllocationPercent / 100.0).toBigDecimal()

        val actualShareEuroWE =
            buy.shares.toBigDecimal() * assetSummary2.currentSharePrice - assetSummary2.totalExpenses
        val actualSharePercWE =
            assetSummary2.currentTotalValueWE / portfolioSummary.currentValueEuroWE * BigDecimal.valueOf(
                100.0
            )
        val targetShareEuroWE = portfolioSummary.currentValueEuroWE *
                (assetSummary2.targetAllocationPercent / 100.0).toBigDecimal()

        val expEuroDiffNE = actualShareEuroNE - targetShareEuroNE
        val expPercDiffNE = actualSharePercNE.toDouble() - assetSummary2.targetAllocationPercent
        val expEuroDiffWE = actualShareEuroWE - targetShareEuroWE
        val expPercDiffWE = actualSharePercWE.toDouble() - assetSummary2.targetAllocationPercent

        assertEquals(expEuroDiffNE, allocationInfo.differenceEuroNE)
        assertEquals(expPercDiffNE, allocationInfo.differencePercentNE, 0.01)
        assertEquals(expEuroDiffWE, allocationInfo.differenceEuroWE)
        assertEquals(expPercDiffWE, allocationInfo.differencePercentWE, 0.01)
    }

}