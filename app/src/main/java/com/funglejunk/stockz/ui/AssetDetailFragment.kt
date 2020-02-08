package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.funglejunk.stockz.R
import timber.log.Timber

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
            Timber.d(assetSummary.toString())
        }
    }

}