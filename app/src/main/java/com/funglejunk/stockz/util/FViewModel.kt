package com.funglejunk.stockz.util

import androidx.annotation.VisibleForTesting
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
        onFailure: IO<(Throwable) -> Unit> = IO.invoke { logError },
        onSuccess: IO<(A) -> Unit>
    ) = IO.fx {
        continueOn(Dispatchers.IO)
        io.bind().fold(
            {
                val failureIO = onFailure.bind()
                failureIO.invoke(it)
            },
            {
                val successIO = onSuccess.bind()
                successIO.invoke(it)
            }
        )
    }.lifecycleAware().unsafeRunAsync {  }

    private fun <A> IO<A>.lifecycleAware() = IO.racePair(
        Dispatchers.IO, this, endPromise.get()
    )

    @VisibleForTesting
    fun cancelIO() {
        endPromise.complete(Unit).fix().unsafeRunAsync { }
    }

    override fun onCleared() {
        cancelIO()
        super.onCleared()
    }

}