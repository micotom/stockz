package com.funglejunk.stockz.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.ui.adapter.AssetBuyAdapter
import kotlinx.android.synthetic.main.asset_detail_fragment.*

class AssetDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.asset_detail_fragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val assetSummaryArg = arguments?.let { AssetDetailFragmentArgs.fromBundle(it).assetSummary }
        assetSummaryArg?.let { assetSummary ->

            buys_list.addItemDecoration(
                MarginItemDecoration(18, assetSummary.buys.size - 1)
            )
            buys_list.adapter = AssetBuyAdapter(assetSummary.buys.toList())

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