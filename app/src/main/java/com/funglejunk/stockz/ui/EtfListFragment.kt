package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.model.EtfListViewModel
import com.funglejunk.stockz.ui.adapter.ListInfoAdapter
import kotlinx.android.synthetic.main.etf_list_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.content.Context.SEARCH_SERVICE
import android.app.SearchManager
import androidx.appcompat.widget.SearchView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.filter_sheet.*


class EtfListFragment : Fragment(), SearchView.OnQueryTextListener {

    private val viewModel: EtfListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.etf_list_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initFilterSheet()

        val itemClickListener: (XetraEtfFlattened) -> Unit = { etf ->
            findNavController().navigate(EtfListFragmentDirections.listToDetailAction(etf))
        }

        // TODO apply loading status
        viewModel.etfData.observe(viewLifecycleOwner, Observer {
            recycler_view.adapter =
                ListInfoAdapter(it, itemClickListener, this@EtfListFragment.view?.width ?: 0)
        })

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)

        val searchManager = context?.getSystemService(SEARCH_SERVICE) as SearchManager?
        (menu.findItem(R.id.action_search).actionView as SearchView).apply {
            setSearchableInfo(searchManager!!.getSearchableInfo(activity?.componentName))
            isSubmitButtonEnabled = true
            setOnQueryTextListener(this@EtfListFragment)
        }

    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.length > 2) {
            searchDbFor(newText)
        }
        return false
    }

    private fun initFilterSheet() {

        BottomSheetBehavior.from(filter_sheet)?.let { bsb ->
            bsb.state = BottomSheetBehavior.STATE_HIDDEN
            fab_filter.setOnClickListener {
                when (bsb.state) {
                    BottomSheetBehavior.STATE_HIDDEN -> bsb.state =
                        BottomSheetBehavior.STATE_EXPANDED
                    BottomSheetBehavior.STATE_EXPANDED -> bsb.state =
                        BottomSheetBehavior.STATE_HIDDEN
                }
            }
            bsb.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(view: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        // TODO update filter
                    }
                }

                override fun onSlide(view: View, v: Float) = Unit
            })
        }
    }

    private fun searchDbFor(query: String) {
        viewModel.searchDbFor(query)
    }

}