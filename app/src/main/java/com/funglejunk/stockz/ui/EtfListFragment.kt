package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.model.EtfListViewModel
import com.funglejunk.stockz.ui.adapter.ListInfoAdapter
import kotlinx.android.synthetic.main.etf_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.content.Context.SEARCH_SERVICE
import android.app.SearchManager
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import timber.log.Timber


class EtfListFragment : Fragment(), SearchView.OnQueryTextListener {

    private val viewModel: EtfListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.etf_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val itemClickListener: (XetraEtfFlattened) -> Unit = { etf ->
            findNavController().navigate(EtfListFragmentDirections.listToDetailAction(etf))
        }

        // TODO apply loading status
        viewModel.etfData.observe(viewLifecycleOwner, Observer {
            recycler_view.adapter = ListInfoAdapter(it, itemClickListener, this@EtfListFragment.view?.width ?: 0)
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
        // Timber.d("submit query: $query")
        // searchDbFor(query)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.length > 2) {
            searchDbFor(newText)
        }
        return false
    }

    private fun searchDbFor(query: String) {
        viewModel.searchDbFor(query)
    }



}