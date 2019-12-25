package com.funglejunk.stockz

import com.funglejunk.stockz.model.EtfDetailViewModel
import com.funglejunk.stockz.model.EtfListViewModel
import com.funglejunk.stockz.model.FilterDialogViewModel
import com.funglejunk.stockz.model.XetraMasterDataInflater
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.repo.fboerse.FBoerseRepoImpl
import com.funglejunk.stockz.util.AndroidRuntimeSchedulers
import com.funglejunk.stockz.util.RxSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dbModule = module {
    single { XetraDb.create(get()) }
    single { XetraMasterDataInflater(get(), get()) }
}

val repoModule = module {
    single<FBoerseRepo> { FBoerseRepoImpl() }
}

val vmModule = module {
    viewModel { EtfDetailViewModel(get()) }
    viewModel { EtfListViewModel(XetraMasterDataInflater(get(), get()), get()) }
    viewModel { FilterDialogViewModel(get()) }
}

val schedulersModule = module {
    single<RxSchedulers> { AndroidRuntimeSchedulers() }
}
