package com.funglejunk.stockz.ui

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.withSafeContext
import kotlinx.android.synthetic.main.filter_dialog.*
import kotlin.math.round

typealias QueryDataListener = (UiEtfQuery) -> Unit

class FilterDialog : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(listener: QueryDataListener): FilterDialog {
            return FilterDialog().apply {
                queryDataListener = listener
            }
        }
    }

    private lateinit var queryDataListener: QueryDataListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.filter_dialog, container, false)

    // TODO use string placeholder for slider text
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        slider_text.text = "${ter_slider.value.round()}%"
        ter_slider.setOnChangeListener { _, value ->
            slider_text.text = "${value.round()}%"
        }
        withSafeContext {
            profit_use_dropdown.setAdapter(
                ArrayAdapter<String>(
                    it, R.layout.dropdown_item, arrayOf("Distributing", "Accumulating")
                )
            )
        }
        submit_button.setOnClickListener {
            queryDataListener.invoke(
                UiEtfQuery(
                    name = name_input_field.text?.toString() ?: UiEtfQuery.NAME_EMPTY,
                    ter = ter_slider.value.round(),
                    profitUse = profit_use_dropdown.text.toString()
                )
            )
            dismiss()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO apply existing query
    }

    private fun Float.round() = round(this * 100) / 100

}