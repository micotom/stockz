package com.funglejunk.stockz.util

import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.Promise
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.async.async
import arrow.fx.fix
import kotlinx.coroutines.Dispatchers

abstract class FViewModel : ViewModel() {

    private val endPromise: Promise<ForIO, Unit> = Promise.unsafeUncancelable(IO.async())

    fun <A> runIO(
        io: IO<Either<Throwable, A>>,
        onFailure: (Throwable) -> Unit = logError,
        onSuccess: (A) -> Unit
    ) = IO.fx {
        continueOn(Dispatchers.IO)
        io.bind().fold(
            { e -> onFailure(e) },
            { result -> onSuccess(result) }
        )
    }.lifecycleAware().unsafeRunSync()

    private fun <A> IO<A>.lifecycleAware() = IO.racePair(
        Dispatchers.IO, this, endPromise.get()
    )

    override fun onCleared() {
        endPromise.complete(Unit).fix().unsafeRunAsync { }
        super.onCleared()
    }

}