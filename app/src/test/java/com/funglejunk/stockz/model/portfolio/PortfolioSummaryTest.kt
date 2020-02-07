package com.funglejunk.stockz.model.portfolio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PortfolioSummaryTest {

    private fun Int.bd() = BigDecimal.valueOf(toDouble())

    @Test
    fun testSingleBuy() {

        val buy = AssetSummary.AssetBuy(
            date = LocalDate.of(2019, 10, 4),
            shares = 10.0,
            pricePerShare = 10.toBigDecimal(),
            expenses = 2.5.toBigDecimal()
        )

        val assetSummary = AssetSummary(
            isin = "IE00BKX55T58",
            currentSharePrice = 15.toBigDecimal(),
            buys = setOf(buy),
            targetAllocationPercent = 100.0
        )

        val portfolioSummary = PortfolioSummary(
            setOf(assetSummary)
        )

        assertEquals(100.bd(), portfolioSummary.buyPriceEuroNE)
        assertEquals((100 + 2.5).toBigDecimal(), portfolioSummary.buyPriceEuroWE)
        assertEquals(150.bd(), portfolioSummary.currentValueEuroNE)
        assertEquals((150 - 2.5).toBigDecimal(), portfolioSummary.currentValueEuroWE)
        assertEquals(50.bd(), portfolioSummary.profitEuroNE)
        assertEquals((50 - 2.5).toBigDecimal(), portfolioSummary.profitEuroWE)
        assertEquals((150.0 - 100.0) / 10.0 / 10.0 * 100.0, portfolioSummary.profitPercentNE, 0.00)
        assertEquals(
            ((150.0 - 100.0 - 2.5) / 10.0) / 10.0 * 100.0,
            portfolioSummary.profitPercentWE,
            0.00
        )

        val (_, allocationInfo) = portfolioSummary.allocationInfo.entries.first()
        assertEquals(BigDecimal("0.00"), allocationInfo.differenceEuroNE)
        assertEquals(0.0, allocationInfo.differencePercentNE, 0.00)
        assertEquals(BigDecimal("0.00"), allocationInfo.differenceEuroWE)
        assertEquals(0.0, allocationInfo.differencePercentWE, 0.00)
    }

    @Test
    fun `test multiple buy`() {
        val buy1 = AssetSummary.AssetBuy(
            date = LocalDate.of(2019, 10, 4),
            shares = 10.0,
            pricePerShare = 10.toBigDecimal(),
            expenses = 2.5.toBigDecimal()
        )

        val assetSummary1 = AssetSummary(
            isin = "IE00BKX55T58",
            currentSharePrice = 15.toBigDecimal(),
            buys = setOf(buy1),
            targetAllocationPercent = 50.0
        )

        val buy2 = AssetSummary.AssetBuy(
            date = LocalDate.of(2019, 10, 4),
            shares = 10.0,
            pricePerShare = 10.toBigDecimal(),
            expenses = 2.5.toBigDecimal()
        )

        val assetSummary2 = AssetSummary(
            isin = "IE00BKX55T59",
            currentSharePrice = 15.toBigDecimal(),
            buys = setOf(buy2),
            targetAllocationPercent = 50.0
        )

        val portfolioSummary = PortfolioSummary(
            setOf(assetSummary1, assetSummary2)
        )

        portfolioSummary.allocationInfo.entries.forEach { (_, allocationInfo) ->
            assertEquals(BigDecimal("0.00"), allocationInfo.differenceEuroNE)
            assertEquals(0.0, allocationInfo.differencePercentNE, 0.01)
            assertEquals(BigDecimal("0.00"), allocationInfo.differenceEuroWE)
            assertEquals(0.0, allocationInfo.differencePercentWE, 0.01)
        }
    }

}