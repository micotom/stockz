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
import kotlinx.android.synthetic.main.filter_dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel
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

    private val viewModel: FilterDialogViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.filter_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO use string placeholder for slider text
        slider_text.text = "${ter_slider.value.round()}%"
        ter_slider.setOnChangeListener { _, value ->
            slider_text.text = "${value.round()}%"
        }
        profit_use_dropdown.setItems(listOf("Distributing", "Accumulating"))
        replication_dropdown.setItems(listOf("Full Replication", "Optimised", "Swap-based"))

        viewModel.benchmarkNamesLiveData.observe(viewLifecycleOwner, Observer {
            benchmark_dropdown.setItems(it)
        })

        viewModel.publisherNamesLiveData.observe(viewLifecycleOwner, Observer {
            publisher_dropdown.setItems(it)
        })

        viewModel.replicationLiveData.observe(viewLifecycleOwner, Observer {
            replication_dropdown.setItems(it)
        })

        viewModel.profitUseLiveData.observe(viewLifecycleOwner, Observer {
            profit_use_dropdown.setItems(it)
        })

        benchmark_dropdown.setOnClickListener {
            viewModel.onQueryParamsUpdate(buildCurrentQuery())
        }

        publisher_dropdown.setOnClickListener {
            viewModel.onQueryParamsUpdate(buildCurrentQuery())
        }

        replication_dropdown.setOnClickListener {
            viewModel.onQueryParamsUpdate(buildCurrentQuery())
        }

        profit_use_dropdown.setOnClickListener {
            viewModel.onQueryParamsUpdate(buildCurrentQuery())
        }

        submit_button.setOnClickListener {
            queryDataListener.invoke(buildCurrentQuery())
            dismiss()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO apply existing query
    }

    private fun buildCurrentQuery() = UiEtfQuery(
        name = name_input_field.textOrIfEmpty { UiEtfQuery.NAME_EMPTY },
        ter = ter_slider.valueOrDefault { UiEtfQuery.TER_MAX },
        profitUse = profit_use_dropdown.textOrIfEmpty { UiEtfQuery.PROFIT_USE_EMPTY },
        replicationMethod = replication_dropdown.textOrIfEmpty {
            UiEtfQuery.REPLICATION_METHOD_EMPTY
        },
        publisher = publisher_dropdown.textOrIfEmpty { UiEtfQuery.PUBLISHER_EMPTY },
        benchmark = benchmark_dropdown.textOrIfEmpty { UiEtfQuery.BENCHMARK_EMPTY }
    )

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

    private fun AutoCompleteTextView.setItems(items: List<String>) {
        withSafeContext { context ->
            setAdapter(
                ArrayAdapter<String>(context, R.layout.dropdown_item, items.toTypedArray())
            )
        }
    }

}
