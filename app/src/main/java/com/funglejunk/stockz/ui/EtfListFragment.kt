package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
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
import timber.log.Timber

class EtfListFragment : Fragment() {

    private val viewModel: EtfListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.etf_list_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initFilterSheet()

        val itemClickListener: (Etf) -> Unit = { etf ->
            findNavController().navigate(EtfListFragmentDirections.listToDetailAction(etf))
        }

        // TODO apply loading status
        viewModel.etfData.observe(viewLifecycleOwner, Observer {
            // TODO change args order and beautify lambda passing
            recycler_view.adapter =
                ListInfoAdapter(it, itemClickListener, this@EtfListFragment.view?.width ?: 0)
        })
    }

    private fun initFilterSheet() {

        fab_filter.setOnClickListener {
            FilterDialog.newInstance { query ->
                Timber.d("execute query: $query")
                viewModel.searchDbFor(query)
            }.also {
                activity?.let { safeActivity ->
                    it.show(safeActivity.supportFragmentManager, "some_tag")
                }
            }
        }
    }
}
