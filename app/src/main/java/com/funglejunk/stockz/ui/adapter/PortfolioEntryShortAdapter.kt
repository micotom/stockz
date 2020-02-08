package com.funglejunk.stockz.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.Etf
import com.funglejunk.stockz.model.portfolio.AssetSummary
import com.funglejunk.stockz.model.portfolio.PortfolioSummary
import com.funglejunk.stockz.textStringCurrency
import com.funglejunk.stockz.textStringPercent

class PortfolioEntryShortAdapter(
    private val summary: PortfolioSummary,
    val etfs: List<Etf>,
    val clickListener: (AssetSummary) -> Unit
) :
    RecyclerView.Adapter<PortfolioEntryShortAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val etfName: TextView = view.findViewById(R.id.etf_name_short_info)
        val isin: TextView = view.findViewById(R.id.isin_short_info)
        val currentValueLayout: ViewGroup = view.findViewById(R.id.current_value_short_info)
        val profitEuroLayout: ViewGroup = view.findViewById(R.id.profit_euro_value_short_info)
        val profitPercLayout: ViewGroup = view.findViewById(R.id.profit_perc_value_short_info)

        fun ViewGroup.setText(descriptor: String, info: String) {
            findViewById<TextView>(R.id.header_text).text = descriptor
            findViewById<TextView>(R.id.value_text).text = info
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        with(LayoutInflater.from(parent.context)) {
            Holder(inflate(R.layout.portfolio_info_short, parent, false) as ViewGroup)
        }

    override fun getItemCount(): Int = etfs.size

    override fun onBindViewHolder(holder: Holder, position: Int) = etfs[position].let { etf ->
        summary.assets.find { it.isin == etf.isin }?.let { asset ->
            holder.apply {
                etfName.text = etf.name
                isin.text = etf.isin
                currentValueLayout.setText("Value", asset.currentTotalValueNE.textStringCurrency())
                profitEuroLayout.setText("Profit (€)", asset.profitEuroWE.textStringCurrency())
                profitPercLayout.setText("Profit (%)", asset.profitPercentWE.textStringPercent())
            }
            holder.itemView.setOnClickListener {
                clickListener(asset)
            }
        }
        Unit
    }

}