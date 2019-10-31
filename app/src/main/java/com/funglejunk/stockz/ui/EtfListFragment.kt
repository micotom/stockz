package com.funglejunk.stockz.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funglejunk.stockz.R
import com.funglejunk.stockz.data.XetraEtfFlattened
import com.funglejunk.stockz.model.EtfListViewModel
import kotlinx.android.synthetic.main.etf_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class EtfListFragment : Fragment() {

    private val viewModel: EtfListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.etf_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recycler_view.layoutManager = LinearLayoutManager(context)

        val itemClickListener: (XetraEtfFlattened) -> Unit = { etf ->
            findNavController().navigate(EtfListFragmentDirections.listToDetailAction(etf))
        }

        viewModel.etfData.observe(viewLifecycleOwner, Observer {
            recycler_view.adapter = ListAdapter(it, itemClickListener)
        })
    }

}

private class ListAdapter(
    private val data: List<XetraEtfFlattened>,
    private val onClickListener: (XetraEtfFlattened) -> Unit
) :
    RecyclerView.Adapter<EtfViewHolder>() {

    companion object {
        const val ANIM_DURATION = 1000L
    }

    private var onAttach = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EtfViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.etf_view, parent, false) as ViewGroup
        return EtfViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: EtfViewHolder, position: Int) {
        val item = data[position]
        holder.apply {
            nameText.text = item.name
            publisherText.text = item.publisherName
            isinText.text = item.isin
            symbolText.text = item.symbol
            indexText.text = item.benchmarkName
            terText.text = item.ter.toString()
            profitUseText.text = item.profitUse
            replicationText.text = item.replicationMethod
            listingText.text = item.listingDate

            itemView.setOnClickListener {
                onClickListener(item)
            }
        }
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
        itemView.findViewById<View>(R.id.left_column).translationX = -500.0f
        itemView.findViewById<View>(R.id.right_column).alpha = 0f
        val animatorSet = AnimatorSet()
        val leftAnimator = ObjectAnimator.ofFloat(
            itemView.findViewById(R.id.left_column),
            "translationX",
            -500f,
            0f
        )
        val rightAnimator =
            ObjectAnimator.ofFloat(itemView.findViewById(R.id.right_column), "alpha", 1f).also {
                it.duration = 1000
            }
        animatorSet.also {
            it.duration = ANIM_DURATION
            it.startDelay = when (isNotFirstItem) {
                true -> ANIM_DURATION
                false -> iTemp * ANIM_DURATION
            }
            it.playTogether(leftAnimator, rightAnimator)
        }
        leftAnimator.start()
        rightAnimator.start()
    }

}

private class EtfViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    init {
        leftColumn().apply {
            findVg(R.id.name).setHeadline("Name")
            findVg(R.id.publisher).setHeadline("Publisher")
            findVg(R.id.isin).setHeadline("Isin")
            findVg(R.id.symbol).setHeadline("Symbol")
            findVg(R.id.index).setHeadline("Index")
        }

        rightColumn().apply {
            findVg(R.id.ter).setHeadline("TER")
            findVg(R.id.profitUse).setHeadline("Profit Use")
            findVg(R.id.replication).setHeadline("Replication")
            findVg(R.id.listing_date).setHeadline("Listing Date")
        }
    }

    val nameText: TextView = leftColumn().findVg(R.id.name).findTv(R.id.info_text)
    val publisherText: TextView = leftColumn().findVg(R.id.publisher).findTv(R.id.info_text)
    val isinText: TextView = leftColumn().findVg(R.id.isin).findTv(R.id.info_text)
    val symbolText: TextView = leftColumn().findVg(R.id.symbol).findTv(R.id.info_text)
    val indexText: TextView = leftColumn().findVg(R.id.index).findTv(R.id.info_text)

    val terText: TextView = rightColumn().findVg(R.id.ter).findTv(R.id.info_text)
    val profitUseText: TextView = rightColumn().findVg(R.id.profitUse).findTv(R.id.info_text)
    val replicationText: TextView = rightColumn().findVg(R.id.replication).findTv(R.id.info_text)
    val listingText: TextView = rightColumn().findVg(R.id.listing_date).findTv(R.id.info_text)

    private fun leftColumn() = view.findViewById<ViewGroup>(R.id.left_column)
    private fun rightColumn() = view.findViewById<ViewGroup>(R.id.right_column)

    private fun ViewGroup.setHeadline(headline: String) {
        findTv(R.id.headline).text = headline
    }

    private fun ViewGroup.findVg(id: Int) = findViewById<ViewGroup>(id)

    private fun ViewGroup.findTv(id: Int) = findViewById<TextView>(id)
}