package com.funglejunk.stockz.repo.db

interface XetraDbInterface {

    fun perfDao(): XetraPerformanceEntryDao

    fun etfDao(): XetraEtfInfoDao

    fun publisherDao(): XetraEtfPublisherDao

    fun benchmarkDao(): XetraEtfBenchmarkDao

    fun etfFlattenedDao(): XetraEtfFlattenedDao
}
