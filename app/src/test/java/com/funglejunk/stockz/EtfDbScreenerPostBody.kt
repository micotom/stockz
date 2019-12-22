package com.funglejunk.stockz

import kotlinx.serialization.Serializable

@Serializable
data class EtfDbScreenerPostBody(
    val page: Int,
    val only: List<String>,
    val tab: String,
    val per_page: Int
)
