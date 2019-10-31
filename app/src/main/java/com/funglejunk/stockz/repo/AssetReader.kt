package com.funglejunk.stockz.repo

import io.reactivex.Single

interface AssetReader {

    fun getTickerSymbols(): Single<Array<String>>

}