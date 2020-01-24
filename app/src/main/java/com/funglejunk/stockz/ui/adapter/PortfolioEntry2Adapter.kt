package com.funglejunk.stockz.ui.adapter

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.PortfolioSummaryViewModel
import com.funglejunk.stockz.model.portfolio.PortfolioSummary
import java.math.BigDecimal
import java.text.NumberFormat

class PortfolioEntry2Adapter(
    private val summary: PortfolioSummaryViewModel
) :
    RecyclerView.Adapter<PortfolioEntry2Adapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val etfName: TextView = view.findViewById(R.id.etf_name)
        val isinLayout: ViewGroup = view.findViewById(R.id.isin_layout)
        val currentPriceLayout: ViewGroup = view.findViewById(R.id.current_price_layout)
        val nrOfSharesLayout: ViewGroup = view.findViewById(R.id.nr_of_shares_layout)
        val totalValueNeLayout: ViewGroup = view.findViewById(R.id.total_value_ne_layout)
        val totalValueWeLayout: ViewGroup = view.findViewById(R.id.total_value_we_layout)
        val profitNeLayout: ViewGroup = view.findViewById(R.id.profit_ne_layout)
        val profitWeLayout: ViewGroup = view.findViewById(R.id.profit_we_layout)
        val totalExpensesLayout: ViewGroup = view.findViewById(R.id.total_expenses_layout)
        val targetAllocLayout: ViewGroup = view.findViewById(R.id.target_allocation_layout)
        val allocationNeLayout: ViewGroup = view.findViewById(R.id.ne_allocation_layout)
        val allocationWeLayout: ViewGroup = view.findViewById(R.id.we_allocation_layout)
        val nrOfOrdersLayout: ViewGroup = view.findViewById(R.id.nr_orders_layout)

        fun ViewGroup.setText(descriptor: String, info: String) {
            findViewById<TextView>(R.id.descriptor).text = descriptor
            findViewById<TextView>(R.id.text).text = info
        }
    }

    private val data = summary.first.assets.toList()

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        with(LayoutInflater.from(parent.context)) {
            Holder(inflate(R.layout.portfolio_entry2, parent, false) as ViewGroup)
        }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val asset = data[position]
        holder.apply {
            etfName.text = summary.second.find {
                it.isin == asset.isin
            }!!.name
            isinLayout.setText(
                "ISIN",
                asset.isin
            )
            currentPriceLayout.setText(
                "CURRENT PRICE",
                asset.currentSharePrice.textStringCurrency()
            )
            nrOfSharesLayout.setText(
                "NUMBER OF SHARES",
                asset.shares.toString()
            )
            totalValueNeLayout.setText(
                "CURRENT TOTAL VALUE (NE)",
                asset.currentTotalValueNE.textStringCurrency()
            )
            totalValueWeLayout.setText(
                "CURRENT TOTAL VALUE (WE)",
                asset.currentTotalValueWE.textStringCurrency()
            )
            profitNeLayout.setText(
                "PROFIT (NE)",
                "${asset.profitEuroNE.textStringCurrency()} (${asset.profitPercentNE.textStringPercent()})"
            )
            profitWeLayout.setText(
                "PROFIT (WE)",
                "${asset.profitEuroWE.textStringCurrency()} (${asset.profitPercentWE.textStringPercent()})"
            )
            totalExpensesLayout.setText(
                "TOTAL EXPENSES",
                asset.totalExpenses.textStringCurrency()
            )
            targetAllocLayout.setText(
                "TARGET ALLOCATION",
                asset.targetAllocationPercent.textStringPercent()
            )
            summary.first.allocationInfo[asset]?.let { info ->
                allocationNeLayout.setText(
                    "ACTUAL ALLOCATION (NE)",
                    (info.differencePercentNE + asset.targetAllocationPercent).textStringPercent()
                )
                allocationWeLayout.setText(
                    "ACTUAL ALLOCATION (WE)",
                    (info.differencePercentWE + asset.targetAllocationPercent).textStringPercent()
                )
            }
            nrOfOrdersLayout.setText(
                "NR OF ORDERS",
                asset.nrOfOrders.toString()
            )
        }
    }

    class MarginItemDecoration(private val spaceHeight: Int, private val lastIndex: Int) :
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

    private companion object {
        val percentFormat = NumberFormat.getPercentInstance().apply {
            minimumFractionDigits = 2
        }
        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            minimumFractionDigits = 3
        }
    }

    private fun Double.textStringPercent() = percentFormat.format(this / 100.0)
    private fun BigDecimal.textStringPercent() =
        percentFormat.format(this / BigDecimal.valueOf(100.0))

    private fun BigDecimal.textStringCurrency() = currencyFormat.format(this)

}