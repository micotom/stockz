package com.funglejunk.stockz.util

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.fx.*
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.async.async
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class FViewModel : ViewModel() {

    private val endPromise: Promise<ForIO, Unit> = Promise.unsafeUncancelable(IO.async())

    fun <A> runIO(
        io: IO<A>,
        onFailure: IO<(Throwable) -> Unit> = IO { logError },
        onSuccess: IO<(A) -> Unit>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) = IO.fx {
        continueOn(dispatcher)
        val (result) = io
        val (successCall) = onSuccess
        successCall.invoke(result)
    }.handleErrorWith {
        IO.fx {
            onFailure.bind().invoke(it)
        }
    }.lifecycleAware().unsafeRunAsyncCancellable { }

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