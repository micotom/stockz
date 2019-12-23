package com.funglejunk.stockz.util

import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.Promise
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.async.async
import kotlinx.coroutines.Dispatchers

abstract class FViewModel : ViewModel() {

    private val endPromise: Promise<ForIO, Unit> = Promise.unsafeUncancelable(IO.async())

    fun <A> runIO(
        io: IO<Either<Throwable, A>>,
        onFailure: (Throwable) -> Unit,
        onSuccess: (A) -> Unit
    ) = IO.fx {
        continueOn(Dispatchers.IO)
        when (val result = io.bind()) {
            is Either.Left -> onFailure(result.a)
            is Either.Right -> onSuccess(result.b)
        }
    }.lifecycleAware().unsafeRunSync()

    private fun <A> IO<A>.lifecycleAware() = IO.racePair(
        Dispatchers.IO, this, endPromise.get()
    )

}