package com.funglejunk.stockz

import com.funglejunk.stockz.model.EtfListViewModel
import com.funglejunk.stockz.model.EtfDetailViewModel
import com.funglejunk.stockz.repo.AndroidAssetReader
import com.funglejunk.stockz.repo.AssetReader
import com.funglejunk.stockz.repo.XetraSymbols
import com.funglejunk.stockz.repo.wdd.WtdRemoteRepo
import com.funglejunk.stockz.repo.wdd.WtdRepo
import com.funglejunk.stockz.util.AndroidRuntimeSchedulers
import com.funglejunk.stockz.util.RxSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val repoModule = module {
    single<WtdRepo> { WtdRemoteRepo() }
}

val readerModule = module {
    single<AssetReader> { AndroidAssetReader(get()) }
}

val vmModule = module {
    viewModel { EtfDetailViewModel(get(), get(), get(), XetraSymbols(get())) }
    viewModel { EtfListViewModel() }
}

val schedulersModule = module {
    single<RxSchedulers> { AndroidRuntimeSchedulers() }
}