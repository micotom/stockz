package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.model.EtfDetailViewModel
import com.funglejunk.stockz.ui.adapter.BasicDetailInfoAdapter
import kotlinx.android.synthetic.main.main_fragment_layout.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class EtfDetailFragment : Fragment() {

    private val viewModel: EtfDetailViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment_layout, container, false)
    }

    private fun renderChartData(data: EtfDetailViewModel.ViewState.NewChartData) {
        mychart.draw(data.drawableHistoricValues)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // setHasOptionsMenu(true)

        viewModel.viewStateData.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is EtfDetailViewModel.ViewState.NewChartData -> {
                    renderChartData(event)
                }
            }
        })

        arguments?.let {
            val etf = EtfDetailFragmentArgs.fromBundle(it).etf
            Timber.d("ETF: $etf")
            viewModel.setEtfArgs(etf)
            showBasicData(etf)
        }

        /*
        viewModel.viewStateData.observe(viewLifecycleOwner, Observer { event ->
            Timber.d("view state: $event")
            when (event) {
                EtfDetailViewModel.ViewState.Loading -> {
                    error_txt.visibility = View.INVISIBLE
                    mychart.visibility = View.INVISIBLE
                    progressbar.visibility = View.VISIBLE
                }
                is EtfDetailViewModel.ViewState.NewChartData -> {
                    error_txt.visibility = View.INVISIBLE
                    progressbar.visibility = View.INVISIBLE
                    mychart.visibility = View.VISIBLE
                    renderChartData(event)
                }
                is EtfDetailViewModel.ViewState.Error -> {
                    progressbar.visibility = View.INVISIBLE
                    mychart.visibility = View.INVISIBLE
                    error_txt.visibility = View.VISIBLE
                    error_txt.text = "${event.error.message}"
                    Timber.e("${event.error}")
                }
            }
        })
         */
    }

    // TODO inflate strings from resources
    private fun showBasicData(etf: XetraEtfFlattened) {
        stock_name.text = etf.name
        left_column.layoutManager = LinearLayoutManager(context)
        right_column.layoutManager = LinearLayoutManager(context)
        val leftData = listOf(
            "Isin" to etf.isin,
            "Symbol" to etf.symbol,
            "Publisher" to etf.publisherName,
            "Benchmark" to etf.benchmarkName,
            "Listing Date" to etf.listingDate
        )
        val rightData = listOf(
            "TER" to "${etf.ter} %",
            "Profit Use" to etf.profitUse,
            "Replication" to etf.replicationMethod,
            "Fund Currency" to etf.fundCurrency,
            "Trading Currency" to etf.tradingCurrency
        )
        left_column.adapter = BasicDetailInfoAdapter(leftData)
        right_column.adapter = BasicDetailInfoAdapter(rightData)
    }

    /*
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
    }
     */

    /*
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        viewModel.infoData.observe(viewLifecycleOwner, Observer { data ->
            Timber.d("got data: $data")
            stock_name.text = data.name
            left_column.layoutManager = LinearLayoutManager(context)
            right_column.layoutManager = LinearLayoutManager(context)
            val leftData = listOf(
                "Price" to "${data.price} ${data.currency}",
                "Symbol" to data.symbol,
                "Change %" to data.change_pct,
                "Day Change" to data.day_change,
                "Close Yesterday" to data.close_yesterday,
                "52 Week High" to data.fiftyTwoWeekHigh,
                "52 Week Low" to data.fiftyTwoWeekLow
            )
            val rightData = listOf(
                "Market Cap" to data.market_cap,
                "Volume" to data.volume,
                "Shares" to data.shares,
                "Stock Exchange" to data.stock_exchange_short,
                "Last Trade Time" to data.last_trade_time
            )
            left_column.adapter = BasicDetailInfoAdapter(leftData)
            right_column.adapter = BasicDetailInfoAdapter(rightData)
        })

        viewModel.chartData.observe(viewLifecycleOwner, Observer { tickers ->
            Timber.d("init suggestions")
            val from = arrayOf("Ticker")
            val to = intArrayOf(R.id.text)
            tickerSuggestionsAdapter = SimpleCursorAdapter(
                activity, R.layout.menu_suggestion_item, null, from, to,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
            ).apply {
                setFilterQueryProvider { constraint ->
                    constraint?.let {
                        populateAdapter(it.toString(), tickers)
                    }
                }
            }
            menu.findItem(R.id.action_search).actionView.apply {
                this as SearchView
                suggestionsAdapter = tickerSuggestionsAdapter
                setIconifiedByDefault(false)
                setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                    override fun onSuggestionSelect(position: Int) = true

                    override fun onSuggestionClick(position: Int): Boolean {
                        val cursor = tickerSuggestionsAdapter.getItem(position) as Cursor
                        val text = cursor.getString(cursor.getColumnIndex("Ticker"))
                        setQuery(text, true)
                        clearFocus()
                        return true
                    }

                })
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextChange(newText: String?) = true

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        query?.let { safeQuery ->

                            Timber.d("safe query: $safeQuery")

                            val xetraIsin = viewModel.getIsinForTerm(safeQuery)

                            Observable.timer(500, TimeUnit.MILLISECONDS).subscribe {
                                viewModel.fetchHistory(null, null, xetraIsin)
                            }
                        }
                        return true
                    }

                })
            }
        })
        viewModel.fetchTickers()
    }
     */

    /*
    private fun populateAdapter(query: String, suggestions: Array<String>): Cursor {
        val matrixCursor = MatrixCursor(arrayOf(BaseColumns._ID, "Ticker"))
        val queryLc = query.toLowerCase()
        val regexp = StringBuilder()
        for (term in queryLc.split(" ")) {
            regexp.append("(?=.*").append(term).append(")")
        }
        Timber.d("regex: $regexp")
        val pattern = Pattern.compile(regexp.toString())
        suggestions.forEachIndexed { index, suggestion ->
            if (pattern.matcher(suggestion.toLowerCase()).find()) {
                Timber.d("matches: $suggestion")
                matrixCursor.addRow(arrayOf(index, suggestion))
            }
        }
        return matrixCursor
    }
     */

}