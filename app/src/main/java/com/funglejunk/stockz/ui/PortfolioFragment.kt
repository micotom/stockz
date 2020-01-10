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
                bundle.clear()
            } ?: {
                hideEntryAddViews()
            }()
        } ?: {
            hideEntryAddViews()
        }()

        viewModel.init()

        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            handleNewViewState(state)
        })

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
                progressbar.visibility = View.INVISIBLE
                portfolio_list.adapter = PortfolioEntryAdapter(viewState.entries.toMutableList())
                val performance = viewState.performance
                total_value_text.text = "TOTAL VALUE: ${performance.totalValue}"
                total_perf_text.text = "TOTAL PERF: ${performance.totalPerformance}%"
            }
            is PortfolioViewModel.ViewState.NewAddButtonEnabledState ->
                save_to_portfolio_button.isEnabled = viewState.enabled
            is PortfolioViewModel.ViewState.PortfolioEntrySaved -> {
                if (portfolio_list.adapter != null) { // we did not come from orientation change
                    (portfolio_list.adapter as PortfolioEntryAdapter).addNewEntry(viewState.entry)
                    (portfolio_list.adapter)?.notifyDataSetChanged()
                    progressbar.visibility = View.INVISIBLE
                    hideEntryAddViews()
                }
            }
        }
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

    private fun Activity.hideKeyboard(v: View) {
        val inputMethodManager = getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
    }

}