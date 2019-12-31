package com.funglejunk.stockz.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.Etf

class FavouritesAdapter(
    private val data: MutableList<Etf>,
    private val onClickListener: (Etf) -> Unit
) : RecyclerView.Adapter<FavouritesAdapter.FavouriteHolder>() {

    class FavouriteHolder(view: View) : RecyclerView.ViewHolder(view) {
        val etfName: TextView = view.findViewById(R.id.etf_name)
        val isin: TextView = view.findViewById(R.id.info_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.favourites_item, parent, false) as ViewGroup
        return FavouriteHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: FavouriteHolder, position: Int) {
        val etfItem = data[position]
        holder.apply {
            etfName.text = etfItem.name
            isin.text = etfItem.isin
            itemView.setOnClickListener {
                onClickListener(etfItem)
            }
        }
    }

    fun getItemAt(index: Int): Etf = data[index]

    fun removeItem(index: Int) {
        data.removeAt(index)
        notifyItemRemoved(index)
    }

}