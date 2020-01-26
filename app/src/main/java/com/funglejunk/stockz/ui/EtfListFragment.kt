package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.model.EtfListViewModel
import com.funglejunk.stockz.ui.adapter.ListInfoAdapter
import kotlinx.android.synthetic.main.etf_list_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class EtfListFragment : Fragment() {

    private val viewModel: EtfListViewModel by viewModel()

    private val itemClickListener: (Etf) -> Unit = { etf ->
        findNavController().navigate(EtfListFragmentDirections.listToDetailAction(etf))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.etf_list_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initFilterSheet()

        viewModel.viewStateData.observe(viewLifecycleOwner, Observer {
            renderViewState(it)
        })
    }

    private fun renderViewState(state: EtfListViewModel.ViewState) {
        when (state) {
            EtfListViewModel.ViewState.Loading -> {
                progressbar.visibility = View.VISIBLE
            }
            is EtfListViewModel.ViewState.EtfData -> {
                progressbar.visibility = View.INVISIBLE
                recycler_view.adapter =
                    ListInfoAdapter(state.etfs, itemClickListener, view?.width ?: 0)
            }
        }
    }

    private fun initFilterSheet() {
        fab_filter.setOnClickListener {
            FilterDialog.newInstance { query ->
                viewModel.searchDbFor(query)
            }.also {
                activity?.let { safeActivity ->
                    it.show(safeActivity.supportFragmentManager, "some_tag")
                }
            }
        }
    }
}
