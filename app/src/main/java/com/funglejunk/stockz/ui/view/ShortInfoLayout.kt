package com.funglejunk.stockz.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.funglejunk.stockz.R


class ShortInfoLayout : LinearLayout {

    constructor(context: Context?) : super(context) {
        inflateLayout(context).let { (headerView, infoView) ->
            header = headerView
            info = infoView
        }
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        inflateLayout(context).let { (headerView, infoView) ->
            header = headerView
            info = infoView
        }
        applyAttrs(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        inflateLayout(context).let { (headerView, infoView) ->
            header = headerView
            info = infoView
        }
        applyAttrs(context, attrs)
    }

    @Suppress("unused")
    constructor(
        context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        inflateLayout(context).let { (headerView, infoView) ->
            header = headerView
            info = infoView
        }
        applyAttrs(context, attrs)
    }

    val header: TextView
    val info: TextView

    init {
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
            orientation = VERTICAL
        }
    }

    private fun inflateLayout(context: Context?): Pair<TextView, TextView> =
        LayoutInflater.from(context).inflate(R.layout.short_info, this, true).let {
            it.findViewById<TextView>(R.id.header_text) to it.findViewById(R.id.value_text)
        }

    private fun applyAttrs(context: Context?, attrs: AttributeSet?) =
        context?.theme?.obtainStyledAttributes(attrs, R.styleable.ShortInfoLayout, 0, 0)?.let {
            it.getString(R.styleable.ShortInfoLayout_header)?.let { headerStr ->
                header.text = headerStr
            }
            it.getString(R.styleable.ShortInfoLayout_value)?.let { infoStr ->
                info.text = infoStr
            }
            it.recycle()
        }

    fun setValue(value: String?) = value.let { info.text = it }

}