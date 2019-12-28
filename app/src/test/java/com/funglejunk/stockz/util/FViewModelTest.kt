package com.funglejunk.stockz.util

import arrow.core.Either
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.handleError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FViewModelTest {

    @Test
    fun `io is cancelled on view model cleared`() {
        var successCalled = false
        var failureCalled = false
        val nothingCalled = { !successCalled && !failureCalled }
        val vm = object: FViewModel() {}
        val longRunningOp = IO.fx {
                @Suppress("DeferredResultUnused")
                effect {
                    withContext(Dispatchers.Default) {
                        delay(8000)
                    }
                }.bind()
            Either.right(Unit)
            }.handleError {
                Either.left(RuntimeException())
            }
        vm.runIO(
            io = longRunningOp,
            onFailure = IO.just { _ ->
                failureCalled = true
            },
            onSuccess = IO.just { _ ->
                successCalled = true
            }
        )

        runBlocking {
            delay(1000)
            vm.cancelIO()
        }

        assertTrue(nothingCalled())
    }

    @Test
    fun `io is successful in time`() {
        var successCalled = false
        var failureCalled = false
        val vm = object: FViewModel() {}
        val longRunningOp = IO.fx {
            @Suppress("DeferredResultUnused")
            effect {
                withContext(Dispatchers.Default) {
                    delay(500)
                }
            }.bind()
            Either.right(Unit)
        }.handleError {
            Either.left(RuntimeException())
        }
        vm.runIO(
            io = longRunningOp,
            onFailure = IO.just { _ ->
                failureCalled = true
            },
            onSuccess = IO.just { _ ->
                successCalled = true
            }
        )

        runBlocking {
            delay(1000)
            vm.cancelIO()
        }

        assertTrue(successCalled)
        assertFalse(failureCalled)
    }

    @Suppress("UNREACHABLE_CODE", "IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
    @Test
    fun `error handler is called on error`() {
        var successCalled = false
        var failureCalled = false
        val vm = object: FViewModel() {}
        val longRunningOp = IO.fx {
            @Suppress("DeferredResultUnused")
            effect {
                withContext(Dispatchers.Default) {
                    delay(500)
                    throw RuntimeException()
                }
            }.bind()
            Either.right(Unit)
        }.handleError {
            Either.left(RuntimeException())
        }
        vm.runIO(
            io = longRunningOp,
            onFailure = IO.just { _ ->
                failureCalled = true
            },
            onSuccess = IO.just { _ ->
                successCalled = true
            }
        )

        runBlocking {
            delay(1000)
            vm.cancelIO()
        }

        assertTrue(failureCalled)
        assertFalse(successCalled)
    }

}