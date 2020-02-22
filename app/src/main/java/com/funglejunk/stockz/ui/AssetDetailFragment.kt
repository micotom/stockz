package com.funglejunk.stockz.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.AssetSummary
import com.funglejunk.stockz.data.PortfolioSummary
import com.funglejunk.stockz.data.RepoHistoryData
import com.funglejunk.stockz.model.AssetDetailViewModel
import com.funglejunk.stockz.textStringCurrency
import com.funglejunk.stockz.textStringPercent
import com.funglejunk.stockz.toLocalDate
import com.funglejunk.stockz.ui.adapter.AssetBuyAdapter
import kotlinx.android.synthetic.main.asset_detail_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate

class AssetDetailFragment : StockzFragment<AssetDetailViewModel.ViewState>() {

    private val viewModel: AssetDetailViewModel by viewModel()

    private lateinit var chartFunc: ((RepoHistoryData) -> Unit)

    private lateinit var drawBasicInfoFunc: () -> Unit

    override val liveData: LiveData<AssetDetailViewModel.ViewState> by lazy {
        viewModel.liveData
    }

    override val layoutId: Int = R.layout.asset_detail_fragment

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        withSafePortfolioAndAssetArgs { portfolioSummary, assetSummary ->
            add_order_button.setOnClickListener {
                openAddOrderDialog(assetSummary, portfolioSummary)
            }
            drawBasicInfoFunc = drawBasicInfoBindFunc(assetSummary)
            chartFunc = chartBindFunc.invoke(
                assetSummary.buys.minBy { it.date }?.date ?: LocalDate.of(2010, 1, 1)
            )
            viewModel.requestDbInfo(assetSummary.isin)
        }
    }

    override fun matchRenderFunc(event: AssetDetailViewModel.ViewState): (AssetDetailViewModel.ViewState) -> Unit =
        when (event) {
            is AssetDetailViewModel.ViewState.EtfInfoRetrieved -> drawInfo
            AssetDetailViewModel.ViewState.Loading -> showProgressBar
        }

    private val chartBindFunc: (LocalDate) -> (RepoHistoryData) -> Unit = { fromDate ->
        { history ->
            val filteredHistory = history.copy(
                content = history.content.filter {
                    it.date.toLocalDate() >= fromDate
                }
            )
            chart.draw(filteredHistory)
        }
    }

    private val drawInfo: (AssetDetailViewModel.ViewState) -> Unit =
        { event: AssetDetailViewModel.ViewState ->
            event as AssetDetailViewModel.ViewState.EtfInfoRetrieved
            val (history, perf) = event.stockData
            showContent()
            drawBasicInfoFunc()
            chartFunc.invoke(history)
            event.etf.also { etf ->
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
            }
        }

    private val showProgressBar: (AssetDetailViewModel.ViewState) -> Unit = { _ ->
        root_layout.children.forEach {
            if (it.id != R.id.progress_bar) {
                it.visibility = View.INVISIBLE
            } else {
                it.visibility = View.VISIBLE
            }
        }
    }

    private val showContent: () -> Unit = {
        root_layout.children.forEach {
            if (it.id == R.id.progress_bar) {
                it.visibility = View.INVISIBLE
            } else {
                it.visibility = View.VISIBLE
            }
        }
    }

    private fun openAddOrderDialog(
        assetSummary: AssetSummary,
        portfolioSummary: PortfolioSummary
    ) {
        activity?.let { safeActivity ->
            AddOrderDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(AddOrderDialog.ARGS_ASSET_SUMMARY, assetSummary)
                    putParcelable(AddOrderDialog.ARGS_PORTFOLIO_SUMMARY, portfolioSummary)
                }
            }.show(
                safeActivity.supportFragmentManager,
                AddOrderDialog::class.java.name
            )
        }
    }

    private val drawBasicInfoBindFunc: (AssetSummary) -> () -> Unit = { assetSummary ->
        {
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

            chartFunc = chartBindFunc.invoke(
                assetSummary.buys.minBy { it.date }?.date ?: LocalDate.of(2010, 1, 1)
            )
        }
    }

    private fun withSafePortfolioAndAssetArgs(f: (PortfolioSummary, AssetSummary) -> Unit): Unit? =
        arguments?.let {
            val portfolio = AssetDetailFragmentArgs.fromBundle(it).portfolioSummary
            val asset = AssetDetailFragmentArgs.fromBundle(it).assetSummary
            f.invoke(portfolio, asset)
        }

    // TODO introduce custom view
    private fun View.setHeaderAndText(header: String, text: String) = {
        findViewById<TextView>(R.id.header_text).text = header
        findViewById<TextView>(R.id.value_text).text = text
    }()

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

}