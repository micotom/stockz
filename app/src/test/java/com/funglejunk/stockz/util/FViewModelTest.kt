package com.funglejunk.stockz.util

import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.handleError
import kotlinx.coroutines.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FViewModelTest {

    @Test
    fun `io is cancelled on view model cleared`() {
        var successCalled = false
        var failureCalled = false
        val nothingCalled = { !successCalled && !failureCalled }
        val vm = object : FViewModel() {}
        val longRunningOp = IO.fx {
            @Suppress("DeferredResultUnused")
            effect {
                withContext(Dispatchers.Default) {
                    delay(8000)
                }
            }.bind()
        }.handleError {
            RuntimeException()
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
        val vm = object : FViewModel() {}
        val longRunningOp = IO.fx {
            @Suppress("DeferredResultUnused")
            effect {
                withContext(Dispatchers.Default) {
                    delay(500)
                }
            }.bind()
        }.handleError {
            RuntimeException()
        }
        vm.runIO(
            io = longRunningOp,
            successDispatcher = Dispatchers.Default,
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

    @Test
    fun `error handler is called on error`() {
        var successCalled = false
        var failureCalled = false
        val vm = object : FViewModel() {}
        val op = IO.fx {
            throw RuntimeException()
        }
        vm.runIO(
            io = op,
            onFailure = IO.just { _ ->
                failureCalled = true
            },
            onSuccess = IO.just { _ ->
                successCalled = true
            }
        )

        runBlocking {
            delay(1000)
        }

        assertTrue(failureCalled)
        assertFalse(successCalled)
    }

}