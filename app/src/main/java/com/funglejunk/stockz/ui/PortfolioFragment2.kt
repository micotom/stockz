package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.PortfolioViewModel2
import com.funglejunk.stockz.ui.adapter.PortfolioEntry2Adapter
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

                assets_list.addItemDecoration(
                    PortfolioEntry2Adapter.MarginItemDecoration(
                        12,
                        etfList.size - 1
                    )
                )

                portfolio_name.text = "FOO PORTFOLIO"
                total_value_ne_text.text = summary.currentValueEuroNE.toString()
                total_value_we_text.text = summary.currentValueEuroWE.toString()
                total_profit_ne_text.text = summary.profitEuroNE.toString()
                total_profit_we_text.text = summary.profitEuroWE.toString()
                total_profit_ne_perc_text.text = summary.profitPercentNE.toString()
                total_profit_we_perc_text.text = summary.profitPercentWE.toString()

                assets_list.adapter = PortfolioEntry2Adapter(viewState.portfolioSummary)
            }
        }
    }

}