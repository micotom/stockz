package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.model.FavouritesViewModel
import com.funglejunk.stockz.ui.adapter.FavouritesAdapter
import kotlinx.android.synthetic.main.etf_list_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class FavouritesFragment : Fragment() {

    private val viewModel: FavouritesViewModel by viewModel()

    private val itemClickListener: (Etf) -> Unit = { etf ->
        Timber.d("clicked: ${etf.name}") // TODO navigate
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.favourites_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.favouritesData.observe(viewLifecycleOwner, Observer { etfList ->
            recycler_view.adapter =
                FavouritesAdapter(etfList, itemClickListener)
        })
    }

}