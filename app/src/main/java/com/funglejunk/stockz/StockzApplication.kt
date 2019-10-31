package com.funglejunk.stockz

import android.app.Application
import com.facebook.stetho.Stetho
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.repo.xetra.XetraMasterDataInflator
import com.github.kittinunf.fuel.core.FuelManager
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


@Suppress("unused")
class StockzApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        }

        startKoin {
            androidLogger()
            androidContext(this@StockzApplication)
            modules(
                listOf(repoModule, readerModule, vmModule, schedulersModule)
            )
        }

        XetraDb.init(this)

        XetraMasterDataInflator(this, XetraDb.get()).init()
            .subscribeOn(Schedulers.io())
            .subscribe {
                Timber.d("db master complete")
            }

        FuelManager.instance.timeoutReadInMillisecond = TimeUnit.SECONDS.toMillis(30).toInt()

    }

}