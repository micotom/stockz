package com.funglejunk.stockz

import android.app.Application
import com.facebook.stetho.Stetho
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber
import com.funglejunk.stockz.repo.db.XetraDb
import com.funglejunk.stockz.model.XetraMasterDataInflater
import com.github.kittinunf.fuel.core.FuelManager
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


@Suppress("unused")
class StockzApplication : Application() {

    private var disposable: Disposable? = null

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
                listOf(repoModule, vmModule, schedulersModule)
            )
        }

        XetraDb.init(this)

        FuelManager.instance.timeoutReadInMillisecond = TimeUnit.SECONDS.toMillis(30).toInt()

    }

    override fun onTerminate() {
        disposable?.dispose()
        super.onTerminate()
    }

}