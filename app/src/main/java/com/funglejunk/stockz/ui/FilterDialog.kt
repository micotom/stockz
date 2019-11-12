package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.Observer
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.UiEtfQuery
import com.funglejunk.stockz.model.FilterDialogViewModel
import com.funglejunk.stockz.withSafeContext
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.round
import kotlinx.android.synthetic.main.filter_dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel

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

    private val viewModel: FilterDialogViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.filter_dialog, container, false)

    // TODO use string placeholder for slider text
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slider_text.text = "${ter_slider.value.round()}%"
        ter_slider.setOnChangeListener { _, value ->
            slider_text.text = "${value.round()}%"
        }
        withSafeContext { context ->
            profit_use_dropdown.setAdapter(
                ArrayAdapter<String>(
                    context, R.layout.dropdown_item, arrayOf("Distributing", "Accumulating")
                )
            )
            replication_dropdown.setAdapter(
                ArrayAdapter<String>(
                    context,
                    R.layout.dropdown_item,
                    arrayOf("Full Replication", "Optimised", "Swap-based")
                )
            )
            viewModel.benchmarkNamesLiveData.observe(viewLifecycleOwner, Observer {
                benchmark_dropdown.setAdapter(
                    ArrayAdapter<String>(
                        context, R.layout.dropdown_item, it.toTypedArray())
                )
            })
            viewModel.publisherNamesLiveData.observe(viewLifecycleOwner, Observer {
                publisher_dropdown.setAdapter(
                    ArrayAdapter<String>(
                        context, R.layout.dropdown_item, it.toTypedArray())
                )
            })
        }
        submit_button.setOnClickListener {
            queryDataListener.invoke(
                UiEtfQuery(
                    name = name_input_field.textOrIfEmpty { UiEtfQuery.NAME_EMPTY },
                    ter = ter_slider.valueOrDefault { UiEtfQuery.TER_MAX },
                    profitUse = profit_use_dropdown.textOrIfEmpty { UiEtfQuery.PROFIT_USE_EMPTY },
                    replicationMethod = replication_dropdown.textOrIfEmpty {
                        UiEtfQuery.REPLICATION_METHOD_EMPTY
                    },
                    publisher = publisher_dropdown.textOrIfEmpty { UiEtfQuery.PUBLISHER_EMPTY },
                    benchmark = benchmark_dropdown.textOrIfEmpty { UiEtfQuery.BENCHMARK_EMPTY }
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

    private fun TextInputEditText.textOrIfEmpty(f: () -> String) =
        when (text?.toString().isNullOrEmpty()) {
            true -> f()
            else -> text.toString()
        }

    private fun AutoCompleteTextView.textOrIfEmpty(f: () -> String) =
        when (text.toString().isEmpty()) {
            true -> f()
            else -> text.toString()
        }

    private fun Slider.valueOrDefault(f: () -> Float) = when (isEnabled) {
        true -> value.round()
        false -> f()
    }
}
