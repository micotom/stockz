package com.funglejunk.stockz.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.funglejunk.stockz.R


class LabelledBoxLayout(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        //LayoutInflater.from(context).inflate(R.layout.labelled_alg_box, this, true)
        inflate(context, R.layout.labelled_alg_box, this)

        /*
val imageView: ImageView = findViewById(R.id.image)
val textView: TextView = findViewById(R.id.caption)

val attributes = context.obtainStyledAttributes(attrs, R.styleable.BenefitView)
imageView.setImageDrawable(attributes.getDrawable(R.styleable.BenefitView_image))
textView.text = attributes.getString(R.styleable.BenefitView_text)
attributes.recycle()
*/
    }

    /*
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            getChildAt(i).layout(left, top, right, bottom)
        }
    }

     */



}