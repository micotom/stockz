package com.funglejunk.stockz.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import com.funglejunk.stockz.R
import com.funglejunk.stockz.repo.XetraSymbols

class StockzActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        XetraSymbols(this).get()
        // Tickers.get(this)
    }

    override fun onSupportNavigateUp() = findNavController(R.id.list_fragment).navigateUp()

}
