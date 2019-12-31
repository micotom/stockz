package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.model.FavouritesViewModel
import com.funglejunk.stockz.ui.adapter.FavouritesAdapter
import kotlinx.android.synthetic.main.favourites_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavouritesFragment : Fragment() {

    private val viewModel: FavouritesViewModel by viewModel()

    private val itemClickListener: (Etf) -> Unit = { etf ->
        findNavController().navigate(FavouritesFragmentDirections.favToDetailAction(etf))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.favourites_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.favouritesData.observe(viewLifecycleOwner, Observer { viewState ->
            when (viewState) {
                is FavouritesViewModel.ViewState.FavouriteList -> {
                    val adapter = FavouritesAdapter(
                        viewState.content.toMutableList(),
                        itemClickListener
                    )
                    recycler_view.adapter = adapter
                    val swipeHelper = object : LeftSwipeHelper() {
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            val itemPosition = viewHolder.adapterPosition
                            val swipedEtf = adapter.getItemAt(itemPosition)
                            viewModel.removeFromFavs(swipedEtf, itemPosition)
                        }
                    }
                    ItemTouchHelper(swipeHelper).attachToRecyclerView(recycler_view)
                }
                is FavouritesViewModel.ViewState.EntryRemoved -> {
                    recycler_view.adapter?.let { adapter ->
                        (adapter as FavouritesAdapter).removeItem(viewState.listPosition)
                    }
                }
            }
        })
    }

    private abstract class LeftSwipeHelper :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false
    }
}