package com.funglejunk.stockz.util


class Partial<T: Any, R: Any, S>(private val action: (T) -> (R) -> S) {

    private lateinit var bound: (R) -> S

    fun apply(t: T) {
        bound = action(t)
    }

    operator fun invoke(r: R): S = bound.invoke(r)

}