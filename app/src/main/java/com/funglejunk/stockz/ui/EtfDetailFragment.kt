package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.data.fboerse.FBoersePerfData
import com.funglejunk.stockz.model.EtfDetailViewModel
import com.funglejunk.stockz.round
import com.funglejunk.stockz.ui.adapter.BasicDetailInfoAdapter
import com.funglejunk.stockz.util.TimeSpanFilter
import com.funglejunk.stockz.withSafeContext
import kotlinx.android.synthetic.main.etf_detail_fragment.*
import kotlinx.android.synthetic.main.etf_detail_fragment.atr_checkbox
import kotlinx.android.synthetic.main.etf_detail_fragment.bollinger_checkbox
import kotlinx.android.synthetic.main.etf_detail_fragment.sma_checkbox
import kotlinx.android.synthetic.main.etf_detail_fragment.spinner
import kotlinx.android.synthetic.main.portfolio_fragment2.*
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.viewStateData.observe(viewLifecycleOwner, Observer { event ->
            renderNewViewState(event)
        })

        arguments?.let {
            val etf = EtfDetailFragmentArgs.fromBundle(it).etf
            viewModel.setEtfArgs(etf)
            showBasicData(etf)
            fav_button.setOnClickListener {
                viewModel.addToFavourites(etf)
            }
            add_to_portfolio_button.setOnClickListener {
                findNavController().navigate(EtfDetailFragmentDirections.detailToPortfolioAction())
            }
        }

        fun drawTimeSpan(timeSpanFilter: TimeSpanFilter) = viewModel.getHistory(timeSpanFilter)?.let {
            mychart.draw(it)
        }

        val chartTimes = listOf("MAX", "1 YEAR", "3 MONTHS", "MONTH", "WEEK")

        bollinger_checkbox.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> mychart.showBollinger()
                false -> mychart.hideBollinger()
            }
        }

        sma_checkbox.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> mychart.showSma()
                false -> mychart.hideSma()
            }
        }

        atr_checkbox.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> mychart.showAtr()
                false -> mychart.hideAtr()
            }
        }

        spinner.setItems(chartTimes)
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
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

    private fun renderNewViewState(event: EtfDetailViewModel.ViewState) {
        Timber.d("New view state: ${event::class.java.simpleName}")
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
                renderChartData(event.historyData)
                showExtendedData(event.etf, event.performanceData)
            }
            is EtfDetailViewModel.ViewState.Error -> {
                progressbar.visibility = View.INVISIBLE
                mychart.visibility = View.INVISIBLE
                error_txt.visibility = View.VISIBLE
                error_txt.text = "Something went wrong :(\n${event.error.message}" // TODO externalize to string resources
            }
            is EtfDetailViewModel.ViewState.NewEtfFavouriteState -> {
                fav_button.visibility = when (event.isFavourite) {
                    true -> View.GONE
                    false -> View.VISIBLE
                }
            }
        }
    }

    private fun renderChartData(data: FBoerseHistoryData) {
        mychart.draw(data)
    }

    private fun showBasicData(etf: Etf) {
        stock_name.text = etf.name
        publisher_name.text = etf.publisherName
        isin.text = etf.isin
    }

    // TODO inflate strings from resources
    private fun showExtendedData(etf: Etf, data: FBoersePerfData) {
        val leftData = listOf(
            "Symbol" to etf.symbol,
            "Benchmark" to etf.benchmarkName,
            "Replication" to etf.replicationMethod,
            "Listing Date" to etf.listingDate,
            "1 Month" to "${data.months1.changeInPercent.round()}%",
            "3 Months" to "${data.months3.changeInPercent.round()}%",
            "6 Months" to "${data.months6.changeInPercent.round()}%"
        )
        val rightData = listOf(
            "TER" to "${etf.ter} %",
            "Profit Use" to etf.profitUse,
            "Fund Currency" to etf.fundCurrency,
            "Trading Currency" to etf.tradingCurrency,
            "1 Year" to "${data.years1.changeInPercent.round()}%",
            "2 Years" to "${data.years2.changeInPercent.round()}%",
            "3 Years" to "${data.years3.changeInPercent.round()}%"
        )

        left_column.adapter = BasicDetailInfoAdapter(leftData.toMutableList())
        right_column.adapter = BasicDetailInfoAdapter(rightData.toMutableList())
    }

    private fun Spinner.setItems(items: List<String>) {
        withSafeContext { context ->
            adapter = ArrayAdapter<String>(context, R.layout.small_dropdown_item, items.toTypedArray())
            setSelection(0)
        }
    }

}
