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
import com.funglejunk.stockz.model.AssetDetailViewModel
import com.funglejunk.stockz.textStringCurrency
import com.funglejunk.stockz.textStringPercent
import com.funglejunk.stockz.ui.adapter.AssetBuyAdapter
import kotlinx.android.synthetic.main.asset_detail_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

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

            with(current_value_info) {
                findViewById<TextView>(R.id.header_text).text = "Value (€)"
                findViewById<TextView>(R.id.value_text).text =
                    assetSummary.currentTotalValueWE.textStringCurrency()
            }

            with(profit_euro_value_info) {
                findViewById<TextView>(R.id.header_text).text = "Profit (€)"
                findViewById<TextView>(R.id.value_text).text =
                    assetSummary.profitEuroWE.textStringCurrency()
            }

            with(profit_perc_value_info) {
                findViewById<TextView>(R.id.header_text).text = "Profit (%)"
                findViewById<TextView>(R.id.value_text).text =
                    assetSummary.profitPercentWE.textStringPercent()
            }

            bar_view.draw(
                "Buy Price" to assetSummary.totalBuyPriceWE.toFloat(),
                "Value WE" to assetSummary.currentTotalValueWE.toFloat(),
                "Value NE" to assetSummary.currentTotalValueNE.toFloat()
            )

            viewModel.requestDbInfo(assetSummary.isin)

        }
    }

    private fun renderViewState(event: AssetDetailViewModel.ViewState) {
        when (event) {
            is AssetDetailViewModel.ViewState.EtfInfoRetrieved -> {
                asset_name.text = event.etf.name
            }
        }
    }

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