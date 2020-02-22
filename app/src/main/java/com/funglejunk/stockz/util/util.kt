package com.funglejunk.stockz.util

import android.text.Editable
import android.text.TextWatcher
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.RepoHistoryData
import com.funglejunk.stockz.data.RepoPerformanceData
import timber.log.Timber

typealias EtfList = List<Etf>
typealias StockData = Pair<RepoHistoryData, RepoPerformanceData>

val logError: (Throwable) -> Unit = { throwable -> Timber.e(throwable) }

class SimpleTextWatcher(val f: (String) -> Unit) : TextWatcher {
    override fun afterTextChanged(s: Editable?) = Unit
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = s?.let {
        f(it.toString())
    } ?: Unit
}