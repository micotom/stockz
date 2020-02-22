package com.funglejunk.stockz.ui.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MarginItemDecoration(private val spaceHeight: Int, private val lastIndex: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) = with(outRect) {
        if (parent.getChildAdapterPosition(view) != lastIndex) {
            bottom = spaceHeight
        }
    }

}