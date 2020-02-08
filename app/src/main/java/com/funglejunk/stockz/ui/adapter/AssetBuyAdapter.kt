package com.funglejunk.stockz.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.portfolio.AssetSummary
import com.funglejunk.stockz.textStringCurrency
import com.funglejunk.stockz.toYearMonthDayString

class AssetBuyAdapter(val items: List<AssetSummary.AssetBuy>) : RecyclerView.Adapter<AssetBuyAdapter.Holder>() {

    class Holder(view: View): RecyclerView.ViewHolder(view) {
        val dateView: TextView = view.findViewById(R.id.date)
        val sharesView: TextView = view.findViewById(R.id.shares)
        val priceView: TextView = view.findViewById(R.id.price)
        val expensesView: TextView = view.findViewById(R.id.expenses)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        with(LayoutInflater.from(parent.context)) {
            Holder(
                inflate(
                    R.layout.buy_entry,
                    parent,
                    false
                ) as ViewGroup
            )
        }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = items[position].let { buy ->
        holder.apply {
            holder.dateView.text = buy.date.toYearMonthDayString()
            holder.sharesView.text = buy.shares.toString()
            holder.priceView.text = buy.pricePerShare.textStringCurrency()
            holder.expensesView.text = buy.expenses.textStringCurrency()
        }
        Unit
    }

}