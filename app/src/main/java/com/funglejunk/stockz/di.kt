package com.funglejunk.stockz

import com.funglejunk.stockz.model.EtfDetailViewModel
import com.funglejunk.stockz.model.EtfListViewModel
import com.funglejunk.stockz.repo.wtd.WtdRemoteRepo
import com.funglejunk.stockz.repo.wtd.WtdRepo
import com.funglejunk.stockz.util.AndroidRuntimeSchedulers
import com.funglejunk.stockz.util.RxSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val repoModule = module {
    single<WtdRepo> { WtdRemoteRepo() }
}

val vmModule = module {
    viewModel { EtfDetailViewModel(get()) }
    viewModel { EtfListViewModel() }
}

val schedulersModule = module {
    single<RxSchedulers> { AndroidRuntimeSchedulers() }
}