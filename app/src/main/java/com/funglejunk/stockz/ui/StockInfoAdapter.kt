package com.funglejunk.stockz.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import android.animation.ObjectAnimator
import android.animation.AnimatorSet


class StockInfoAdapter(private val data: List<Pair<String, String>>) : RecyclerView.Adapter<StockInfoAdapter.Holder>() {

    companion object {
        const val ANIM_DURATION = 500L
    }

    private var onAttach = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.info_single_layout, parent, false) as ViewGroup
        return Holder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = data[position]
        holder.headline.text = item.first
        holder.infoText.text = item.second
        setAnimation(holder.itemView, position)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                onAttach = false
                super.onScrollStateChanged(recyclerView, newState)
            }
        })

        super.onAttachedToRecyclerView(recyclerView)
    }

    private fun setAnimation(itemView: View, itemPosition: Int) {
        var iTemp = itemPosition
        if (!onAttach) {
            iTemp = -1
        }
        val isNotFirstItem = iTemp == -1
        iTemp++
        itemView.alpha = 0f
        val animatorSet = AnimatorSet()
        ObjectAnimator.ofFloat(itemView, "alpha", 0f, 0.5f, 1.0f).also {
            it.startDelay = if (isNotFirstItem) ANIM_DURATION / 2 else iTemp * ANIM_DURATION / 3
            it.duration = 500
            animatorSet.play(it)
            it.start()
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val headline: TextView = view.findViewById(R.id.headline)
        val infoText: TextView = view.findViewById(R.id.info_text)
    }

}