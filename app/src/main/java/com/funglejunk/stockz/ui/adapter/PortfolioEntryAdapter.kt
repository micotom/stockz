package com.funglejunk.stockz.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import arrow.core.toOption
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.PortfolioViewModel

class PortfolioEntryAdapter(private val data: MutableList<PortfolioViewModel.PortfolioViewEntry>) :
    RecyclerView.Adapter<PortfolioEntryAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val etfName: TextView = view.findViewById(R.id.etf_name)
        val currentValue: TextView = view.findViewById(R.id.current_value)
        val amount: TextView = view.findViewById(R.id.amount)
        val buyPrice: TextView = view.findViewById(R.id.buy_price)
        val performance: TextView = view.findViewById(R.id.performance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.portfolio_entry, parent, false) as ViewGroup
        return Holder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val portfolioEntry = data[position]
        holder.apply {
            etfName.text = portfolioEntry.etfName
            currentValue.text = "${portfolioEntry.currentValue} €"
            amount.text = portfolioEntry.amount.toString()
            buyPrice.text = "${portfolioEntry.buyPrice} €"
            performance.text = "${portfolioEntry.performance}%"
        }
    }

    fun addNewEntry(entry: PortfolioViewModel.PortfolioViewEntry) {
        val existingEntry = data.find {
            it.isin == entry.isin
        }.toOption()
        existingEntry.fold(
            {
                data.add(entry)
            },
            {
                data.remove(it)
                data.add(entry)
            }
        )
    }


}