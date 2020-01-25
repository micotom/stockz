package com.funglejunk.stockz.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.PortfolioViewModel2
import com.funglejunk.stockz.textStringCurrency
import com.funglejunk.stockz.textStringPercent
import com.funglejunk.stockz.ui.adapter.PortfolioEntryShortAdapter
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

        viewModel.addFooData()

        viewModel.liveData.observe(viewLifecycleOwner, Observer { state ->
            handleNewViewState(state)
        })
    }

    private fun handleNewViewState(viewState: PortfolioViewModel2.ViewState) {
        when (viewState) {
            is PortfolioViewModel2.ViewState.NewPortfolioData -> {
                val (summary, etfList) = viewState.portfolioSummary

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
                assets_list.adapter = PortfolioEntryShortAdapter(viewState.portfolioSummary)

                viewModel.loadChart(summary)
            }
            is PortfolioViewModel2.ViewState.NewChartData -> {
                chart.draw(viewState.history)
            }
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