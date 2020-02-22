package com.funglejunk.stockz.ui

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.funglejunk.stockz.R
import com.funglejunk.stockz.model.AddOrderDialogViewModel
import com.funglejunk.stockz.model.portfolio.AssetSummary
import com.funglejunk.stockz.model.portfolio.PortfolioSummary
import com.funglejunk.stockz.repo.db.Buys
import com.funglejunk.stockz.util.SimpleTextWatcher
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.add_order_dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

class AddOrderDialog : BottomSheetDialogFragment() {

    private val viewModel: AddOrderDialogViewModel by viewModel()

    @Parcelize
    private data class Data(
        val time: Long = -1L,
        val amount: Double = -1.0,
        val price: Double = -1.0,
        val expenses: Double = -1.0
    ) : Parcelable {
        fun isValid(): Boolean = time != -1L && amount != -1.0 && price != -1.0 && expenses != -1.0
    }

    // TODO think about simply passing just isin and portfolio id
    companion object {
        const val ARGS_PORTFOLIO_SUMMARY = "com.funglejunk.stockz.portfolio_summary"
        const val ARGS_ASSET_SUMMARY = "com.funglejunk.stockz.asset_summary"
        const val ARGS_DATA = "com.funglejunk.stockz.data_order"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.add_order_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        date_button.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().build()
            picker.addOnPositiveButtonClickListener {
                updateArgsDate(it)
            }
            fragmentManager?.let {
                picker.show(it, picker.toString())
            }
        }
        price_input_field.addTextChangedListener(SimpleTextWatcher {
            updateArgsPrice(it.toDouble())
        })
        amount_input_field.addTextChangedListener(SimpleTextWatcher {
            updateArgsAmount(it.toDouble())
        })
        expenses_input_field.addTextChangedListener(SimpleTextWatcher {
            updateArgsExpenses(it.toDouble())
        })
        add_button.setOnClickListener {
            withSafeArgs {
                val portfolio: PortfolioSummary? = it.getParcelable(ARGS_PORTFOLIO_SUMMARY)
                val asset: AssetSummary? = it.getParcelable(ARGS_ASSET_SUMMARY)
                val data: Data? = it.getParcelable(ARGS_DATA)
                when (portfolio != null && asset != null && data != null && data.isValid()) {
                    true -> {
                        viewModel.submit(
                            Buys(
                                isin = asset.isin,
                                portfolioId = 1, // TODO!
                                date = Instant.ofEpochMilli(data.time).atZone(ZoneId.systemDefault()).toLocalDate(),
                                expenses = BigDecimal(data.expenses),
                                pricePerShare = BigDecimal.valueOf(data.price),
                                shares = data.amount
                            )
                        )
                    }
                    false -> {
                        Timber.e("No proper args present")
                    }
                }
            }

            this@AddOrderDialog.dismiss()
        }
    }

    private fun updateArgsDate(time: Long) {
        withSafeArgs {
            when (val data: Data? = it.getParcelable(ARGS_DATA)) {
                null -> it.putParcelable(ARGS_DATA, Data(time = time))
                else -> it.putParcelable(ARGS_DATA, data.copy(time = time))
            }
        }
    }

    private fun updateArgsPrice(price: Double) {
        withSafeArgs {
            when (val data: Data? = it.getParcelable(ARGS_DATA)) {
                null -> it.putParcelable(ARGS_DATA, Data(price = price))
                else -> it.putParcelable(ARGS_DATA, data.copy(price = price))
            }
        }
    }

    private fun updateArgsAmount(amount: Double) {
        withSafeArgs {
            when (val data: Data? = it.getParcelable(ARGS_DATA)) {
                null -> it.putParcelable(ARGS_DATA, Data(amount = amount))
                else -> it.putParcelable(ARGS_DATA, data.copy(amount = amount))
            }
        }
    }

    private fun updateArgsExpenses(expenses: Double) {
        withSafeArgs {
            when (val data: Data? = it.getParcelable(ARGS_DATA)) {
                null -> it.putParcelable(ARGS_DATA, Data(expenses = expenses))
                else -> it.putParcelable(ARGS_DATA, data.copy(expenses = expenses))
            }
        }
    }

    private fun withSafeArgs(f: (Bundle) -> Unit): Unit = arguments?.let {
        f(it)
    } ?: {
        arguments = Bundle()
        withSafeArgs(f)
    }()

}