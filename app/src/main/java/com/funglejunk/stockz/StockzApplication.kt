package com.funglejunk.stockz

import android.app.Application
import com.facebook.stetho.Stetho
import com.funglejunk.stockz.repo.db.XetraDb
import com.github.kittinunf.fuel.core.FuelManager
import java.util.concurrent.TimeUnit
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

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
                listOf(dbModule, repoModule, vmModule, schedulersModule)
            )
        }

        XetraDb.init(this)
    }

}
