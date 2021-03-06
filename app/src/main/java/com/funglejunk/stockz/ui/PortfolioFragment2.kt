package com.funglejunk.stockz.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.PortfolioViewModel2
import com.funglejunk.stockz.textStringCurrency
import com.funglejunk.stockz.textStringPercent
import com.funglejunk.stockz.ui.adapter.PortfolioEntryShortAdapter
import com.funglejunk.stockz.util.TimeSpanFilter
import com.funglejunk.stockz.withSafeContext
import kotlinx.android.synthetic.main.labelled_alg_box.*
import kotlinx.android.synthetic.main.portfolio_fragment2.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class PortfolioFragment2 : Fragment() {

    private val viewModel: PortfolioViewModel2 by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.portfolio_fragment2, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.liveData.observe(viewLifecycleOwner, Observer { state ->
            handleNewViewState(state)
        })

        if (savedInstanceState == null) {
            viewModel.addFooData()
        }

        fun drawTimeSpan(timeSpanFilter: TimeSpanFilter) = viewModel.getHistory(timeSpanFilter)?.let {
            chart.draw(it)
        }

        val chartTimes = listOf("MAX", "1 YEAR", "3 MONTHS", "MONTH", "WEEK")

        bollinger_checkbox.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> chart.showBollinger()
                false -> chart.hideBollinger()
            }
        }

        sma_checkbox.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> chart.showSma()
                false -> chart.hideSma()
            }
        }

        atr_checkbox.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> chart.showAtr()
                false -> chart.hideAtr()
            }
        }

        spinner.setItems(chartTimes)
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> drawTimeSpan(TimeSpanFilter.Max)
                    1 -> drawTimeSpan(TimeSpanFilter.Year)
                    2 -> drawTimeSpan(TimeSpanFilter.Months3)
                    3 -> drawTimeSpan(TimeSpanFilter.Month)
                    4 -> drawTimeSpan(TimeSpanFilter.Week)
                }
            }

        }
    }

    private fun handleNewViewState(viewState: PortfolioViewModel2.ViewState) {
        when (viewState) {
            PortfolioViewModel2.ViewState.Loading -> {
                // chart_time_dropdown.isEnabled = false
            }
            is PortfolioViewModel2.ViewState.NewPortfolioData -> {
                // chart_time_dropdown.isEnabled = true

                val (summary, etfList, history) = viewState.summary

                portfolio_name.text = "Foo Portfolio"

                with(current_value_info) {
                    findViewById<TextView>(R.id.header_text).text = "Value (€)"
                    findViewById<TextView>(R.id.value_text).text =
                        summary.currentValueEuroWE.textStringCurrency()
                }

                with(profit_euro_value_info) {
                    findViewById<TextView>(R.id.header_text).text = "Profit (€)"
                    findViewById<TextView>(R.id.value_text).text =
                        summary.profitEuroWE.textStringCurrency()
                }

                with(profit_perc_value_info) {
                    findViewById<TextView>(R.id.header_text).text = "Profit (%)"
                    findViewById<TextView>(R.id.value_text).text =
                        summary.profitPercentWE.textStringPercent()
                }

                assets_list.addItemDecoration(
                    MarginItemDecoration(
                        18,
                        etfList.size - 1
                    )
                )
                assets_list.adapter = PortfolioEntryShortAdapter(summary, etfList)

                chart.draw(history)

                assets_header.text = "Assets"

                asset_allocation_view.applyData(summary)
            }
        }
    }

    private fun AutoCompleteTextView.setItems(items: List<String>) {
        withSafeContext { context ->
            setAdapter(
                ArrayAdapter<String>(context, R.layout.small_dropdown_item, items.toTypedArray())
            )
        }
    }

    private fun Spinner.setItems(items: List<String>) {
        withSafeContext { context ->
            adapter = ArrayAdapter<String>(context, R.layout.small_dropdown_item, items.toTypedArray())
            setSelection(0)
        }
    }

    private class MarginItemDecoration(private val spaceHeight: Int, private val lastIndex: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            with(outRect) {
                if (parent.getChildAdapterPosition(view) != lastIndex) {
                    bottom = spaceHeight
                }
            }
        }
    }

}