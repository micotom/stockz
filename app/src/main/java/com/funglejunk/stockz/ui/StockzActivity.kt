package com.funglejunk.stockz.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.funglejunk.stockz.R
import kotlinx.android.synthetic.main.activity_main.*

class StockzActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNavigation.setupWithNavController(
            Navigation.findNavController(this, R.id.nav_host)
        )
    }

    override fun onSupportNavigateUp() = findNavController(R.id.portfolio2_fragment).navigateUp()
}
