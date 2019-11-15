package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.model.EtfDetailViewModel
import com.funglejunk.stockz.ui.adapter.BasicDetailInfoAdapter
import kotlinx.android.synthetic.main.etf_detail_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class EtfDetailFragment : Fragment() {

    private val viewModel: EtfDetailViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.etf_detail_fragment, container, false)
    }

    private fun renderChartData(data: EtfDetailViewModel.ViewState.NewChartData) {
        mychart.draw(data.drawableHistoricValues)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.viewStateData.observe(viewLifecycleOwner, Observer { event ->
            renderNewViewState(event)
        })

        arguments?.let {
            val etf = EtfDetailFragmentArgs.fromBundle(it).etf
            Timber.d("ETF: $etf")
            viewModel.setEtfArgs(etf)
            showBasicData(etf)
        }
    }

    private fun renderNewViewState(event: EtfDetailViewModel.ViewState?) {
        when (event) {
            EtfDetailViewModel.ViewState.Loading -> {
                error_txt.visibility = View.INVISIBLE
                mychart.visibility = View.INVISIBLE
                progressbar.visibility = View.VISIBLE
            }
            is EtfDetailViewModel.ViewState.NewChartData -> {
                error_txt.visibility = View.INVISIBLE
                progressbar.visibility = View.INVISIBLE
                mychart.visibility = View.VISIBLE
                renderChartData(event)
            }
            is EtfDetailViewModel.ViewState.Error -> {
                progressbar.visibility = View.INVISIBLE
                mychart.visibility = View.INVISIBLE
                error_txt.visibility = View.VISIBLE
                error_txt.text = "${event.error.message}"
                Timber.e("${event.error}")
            }
        }
    }

    // TODO inflate strings from resources
    private fun showBasicData(etf: Etf) {
        stock_name.text = etf.name
        left_column.layoutManager = LinearLayoutManager(context)
        right_column.layoutManager = LinearLayoutManager(context)
        val leftData = listOf(
            "Isin" to etf.isin,
            "Symbol" to etf.symbol,
            "Publisher" to etf.publisherName,
            "Benchmark" to etf.benchmarkName,
            "Listing Date" to etf.listingDate
        )
        val rightData = listOf(
            "TER" to "${etf.ter} %",
            "Profit Use" to etf.profitUse,
            "Replication" to etf.replicationMethod,
            "Fund Currency" to etf.fundCurrency,
            "Trading Currency" to etf.tradingCurrency
        )
        left_column.adapter = BasicDetailInfoAdapter(leftData)
        right_column.adapter = BasicDetailInfoAdapter(rightData)
    }
}
