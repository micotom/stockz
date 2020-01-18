package com.funglejunk.stockz.ui

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.toOption
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.PortfolioViewModel
import com.funglejunk.stockz.ui.adapter.PortfolioEntryAdapter
import kotlinx.android.synthetic.main.portfolio_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class PortfolioFragment : Fragment() {

    private val viewModel: PortfolioViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.portfolio_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        arguments?.let { bundle ->
            val etf = PortfolioFragmentArgs.fromBundle(bundle).etf
            etf?.let {
                viewModel.setEtfArgs(etf)
                new_etf_name.text = etf.name
                showEntryAddViews()
                bundle.clear()
            }
        }

        viewModel.init()

        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            handleNewViewState(state)
        })

        refresh_layout.setOnRefreshListener {
            viewModel.init()
        }

        save_to_portfolio_button.setOnClickListener {
            progressbar.visibility = View.VISIBLE
            viewModel.addButtonPressed()
        }

        new_etf_price.addTextChangedListener(object : SimpleTextListener() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Option.fx {
                    val priceStr = s.toOption().bind().toString()
                    val amountStr = new_etf_amount.text.toOption().bind().toString()
                    priceStr to amountStr
                }.fold(
                    {},
                    { viewModel.newAddInformationSet(it.first, it.second) }
                )
            }
        })

        new_etf_amount.addTextChangedListener(object : SimpleTextListener() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Option.fx {
                    val priceStr = new_etf_price.text.toOption().bind().toString()
                    val amountStr = s.toOption().bind().toString()
                    priceStr to amountStr
                }.fold(
                    {},
                    { viewModel.newAddInformationSet(it.first, it.second) }
                )
            }
        })

        val editTextsFocusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                activity?.hideKeyboard(v)
            }
        }
        new_etf_price.onFocusChangeListener = editTextsFocusListener
        new_etf_amount.onFocusChangeListener = editTextsFocusListener
    }

    private fun handleNewViewState(viewState: PortfolioViewModel.ViewState) {
        when (viewState) {
            is PortfolioViewModel.ViewState.PortfolioRead -> {
                refresh_layout.isRefreshing = false
                hideProgressBar()
                initPortfolioList(viewState)
                initPerformanceInfo(viewState)
            }
            is PortfolioViewModel.ViewState.NewAddButtonEnabledState ->
                save_to_portfolio_button.isEnabled = viewState.enabled
            is PortfolioViewModel.ViewState.PortfolioEntrySaved -> {
                portfolio_list.adapter?.let {
                    it as PortfolioEntryAdapter
                    it.addNewEntry(viewState.entry)
                    it.notifyDataSetChanged()
                    hideProgressBar()
                    hideEntryAddViews()
                }
            }
        }
    }

    private fun initPerformanceInfo(viewState: PortfolioViewModel.ViewState.PortfolioRead) {
        val performance = viewState.performance
        total_value_text.text = "TOTAL VALUE: ${performance.totalValue}"
        total_perf_text.text = "TOTAL PERF: ${performance.totalPerformance}%"
        if (viewState.performance.history.content.isNotEmpty()) {
            portfolio_chart.draw(viewState.performance.history)
        }
    }

    private fun initPortfolioList(viewState: PortfolioViewModel.ViewState.PortfolioRead) {
        with (PortfolioEntryAdapter(viewState.entries.toMutableList())) {
            portfolio_list.adapter = this
            val swipeHelper = object : LeftSwipeHelper() {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val swipedEtf = getItemAt(viewHolder.adapterPosition)
                    viewModel.removeFromPortfolio(swipedEtf)
                }
            }

            ItemTouchHelper(swipeHelper).attachToRecyclerView(portfolio_list)
        }
    }

    private fun hideProgressBar() {
        progressbar.visibility = View.GONE
    }

    private fun showEntryAddViews() {
        save_layout.visibility = View.VISIBLE
        new_etf_name.visibility = View.VISIBLE
        entry_add_divider.visibility = View.VISIBLE
    }

    private fun hideEntryAddViews() {
        save_layout.visibility = View.GONE
        new_etf_name.visibility = View.GONE
        entry_add_divider.visibility = View.GONE
    }

    private abstract class SimpleTextListener : TextWatcher {
        override fun afterTextChanged(s: Editable?) = Unit
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    }

    // TODO also used in favourites fragment
    private abstract class LeftSwipeHelper :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false
    }

    private fun Activity.hideKeyboard(v: View) {
        val inputMethodManager = getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
    }

}