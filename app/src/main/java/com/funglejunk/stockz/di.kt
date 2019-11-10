package com.funglejunk.stockz

import com.funglejunk.stockz.model.EtfDetailViewModel
import com.funglejunk.stockz.model.EtfListViewModel
import com.funglejunk.stockz.model.XetraMasterDataInflater
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.fboerse.FBoerseRepo
import com.funglejunk.stockz.repo.fboerse.FBoerseRepoImpl
import com.funglejunk.stockz.repo.wtd.WtdRemoteRepo
import com.funglejunk.stockz.repo.wtd.WtdRepo
import com.funglejunk.stockz.util.AndroidRuntimeSchedulers
import com.funglejunk.stockz.util.RxSchedulers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val repoModule = module {
    single<WtdRepo> { WtdRemoteRepo() }
    single<FBoerseRepo> { FBoerseRepoImpl() }
}

val vmModule = module {
    viewModel { EtfDetailViewModel(get(), get()) }
    viewModel { EtfListViewModel(XetraMasterDataInflater(get(), XetraDb.get())) }
}

val schedulersModule = module {
    single<RxSchedulers> { AndroidRuntimeSchedulers() }
}
