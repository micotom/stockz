package com.funglejunk.stockz

import com.funglejunk.stockz.model.*
import com.funglejunk.stockz.repo.db.StockDataCache
import com.funglejunk.stockz.repo.db.StockDataCacheInterface
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.db.XetraDbInterface
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.repo.fboerse.FBoerseRepoImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dbModule = module {
    single { XetraDb.create(get()) }
    single { XetraMasterDataInflater(get(), get()) }
    single<StockDataCacheInterface> { StockDataCache(get()) }
}

val repoModule = module {
    single<FBoerseRepo> { FBoerseRepoImpl() }
}

val vmModule = module {
    viewModel { EtfDetailViewModel(get(), get(), get()) }
    viewModel { EtfListViewModel(XetraMasterDataInflater(get(), get()), get()) }
    viewModel { FilterDialogViewModel(get()) }
    viewModel { FavouritesViewModel(get()) }
    viewModel { PortfolioViewModel(get(), get()) }
    viewModel { PortfolioViewModel2(get(), get(), get()) }

}