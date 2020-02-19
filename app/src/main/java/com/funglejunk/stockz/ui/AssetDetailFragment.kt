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
import com.funglejunk.stockz.data.fboerse.FBoerseHistoryData
import com.funglejunk.stockz.model.AssetDetailViewModel
import com.funglejunk.stockz.textStringCurrency
import com.funglejunk.stockz.textStringPercent
import com.funglejunk.stockz.toLocalDate
import com.funglejunk.stockz.ui.adapter.AssetBuyAdapter
import kotlinx.android.synthetic.main.asset_detail_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate

class AssetDetailFragment : Fragment() {

    private val viewModel: AssetDetailViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.asset_detail_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.liveData.observe(viewLifecycleOwner, Observer { event ->
            renderViewState(event)
        })

        val assetSummaryArg = arguments?.let { AssetDetailFragmentArgs.fromBundle(it).assetSummary }
        assetSummaryArg?.let { assetSummary ->

            buys_list.addItemDecoration(
                MarginItemDecoration(18, assetSummary.buys.size - 1)
            )
            buys_list.adapter = AssetBuyAdapter(assetSummary.buys.toList())

            current_value_info.setHeaderAndText(
                "Value (€)", assetSummary.currentTotalValueWE.textStringCurrency()
            )

            profit_euro_value_info.setHeaderAndText(
                "Profit (€)", assetSummary.profitEuroWE.textStringCurrency()
            )

            profit_perc_value_info.setHeaderAndText(
                "Profit (%)", assetSummary.profitPercentWE.textStringPercent()
            )
            shares_info.setValue(assetSummary.shares.toString())
            value_info.setValue(assetSummary.currentTotalValueNE.textStringCurrency())
            nr_orders_info.setValue(assetSummary.nrOfOrders.toString())
            expenses_info.setValue(assetSummary.totalExpenses.textStringCurrency())

            viewModel.requestDbInfo(assetSummary.isin)

            chartFunc = chartBindFunc.invoke(
                assetSummary.buys.minBy { it.date }?.date ?: LocalDate.of(2010, 1, 1)
            )
        }
    }

    private fun renderViewState(event: AssetDetailViewModel.ViewState) {
        when (event) {
            is AssetDetailViewModel.ViewState.EtfInfoRetrieved -> {
                val etf = event.etf
                val (history, perf) = event.stockData
                asset_name.text = etf.name
                symbol_info.setValue(etf.symbol)
                ter_info.setValue("${etf.ter} %")
                bar_view_returns.draw(
                    "1 Month" to perf.months1.changeInPercent.toFloat(),
                    "3 Months" to perf.months3.changeInPercent.toFloat(),
                    "6 Months" to perf.months6.changeInPercent.toFloat(),
                    "1 Year" to perf.years1.changeInPercent.toFloat(),
                    "2 Years" to perf.years2.changeInPercent.toFloat(),
                    "3 Years" to perf.years3.changeInPercent.toFloat()
                )
                chartFunc?.invoke(history)
            }
        }
    }

    private val chartBindFunc: (LocalDate) -> (FBoerseHistoryData) -> Unit = { fromDate ->
        { history ->
            val filteredHistory = history.copy(
                content = history.content.filter {
                    it.date.toLocalDate() >= fromDate
                }
            )
            chart.draw(filteredHistory)
        }
    }

    // TODO lateinit?
    private var chartFunc: ((FBoerseHistoryData) -> Unit)? = null

    private class MarginItemDecoration(private val spaceHeight: Int, private val lastIndex: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
        ) {
            with(outRect) {
                if (parent.getChildAdapterPosition(view) != lastIndex) {
                    bottom = spaceHeight
                }
            }
        }
    }

    private fun View.setHeaderAndText(header: String, text: String) = {
        findViewById<TextView>(R.id.header_text).text = header
        findViewById<TextView>(R.id.value_text).text = text
    }()

}