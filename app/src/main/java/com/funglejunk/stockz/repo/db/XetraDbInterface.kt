package com.funglejunk.stockz.repo.db

interface XetraDbInterface {

    fun etfDao(): XetraEtfInfoDao

    fun publisherDao(): XetraEtfPublisherDao

    fun benchmarkDao(): XetraEtfBenchmarkDao

    fun etfFlattenedDao(): XetraEtfFlattenedDao

    fun favouritesDao(): XetraFavouriteDao

    fun portfolioDao(): PortfolioEntriesDao

    fun portfolioDao2(): PortfolioDao

    fun buysDao(): BuysDao

    fun targetAllocationsDao(): TargetAllocationsDao
}
