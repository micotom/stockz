package com.funglejunk.stockz.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

abstract class StockzFragment<T : StockzFragment.ViewState> : Fragment() {

    abstract class ViewState

    abstract val liveData: LiveData<T>

    @get:LayoutRes abstract val layoutId: Int

    private fun observeViewModel() =
        liveData.observe(viewLifecycleOwner, Observer { event ->
            matchRenderFunc(event).invoke(event)
        })

    abstract fun matchRenderFunc(event: T): (T) -> Unit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(layoutId, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeViewModel()
    }

}