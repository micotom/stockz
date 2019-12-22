package com.funglejunk.stockz

import kotlinx.serialization.Serializable

@Serializable
data class EtfDbScreenerResult(
    val meta: Meta,
    val count: Count,
    val `data`: List<Data>
) {
    @Serializable
    data class Meta(
        val page: Int,
        val per_page: Int,
        val sort_direction: String,
        val sort_by: String,
        val order: Order,
        val tab: String,
        val total_pages: Int,
        val total_records: Int,
        val filter: Filter
    ) {
        @Serializable
        data class Order(
            val assets: String
        )
        @Serializable
        class Filter()
    }

    @Serializable
    data class Count(
        val issuer: Map<String, Int>,
        val theme: Map<String, Int>,
        val structure: Map<String, Int>,
        val alternative_types: Map<String, Int>,
        val bond_types: Map<String, Int>,
        val bond_duration: Map<String, Int>,
        val commodity_exposures: Map<String, Int>,
        val commodity_types: Map<String, Int>,
        val currency_types: Map<String, Int>,
        val sizes: Map<String, Int>,
        val investment_strategies: Map<String, Int>,
        val regions: Map<String, Int>,
        val sectors: Map<String, Int>,
        val investment_styles: Map<String, Int>,
        val real_estate_sectors: Map<String, Int>,
        val volatility_terms: Map<String, Int>,
        val leveraged: Map<String, Int>,
        val tax_form: Map<String, Int>,
        val inverse: Map<String, Int>,
        val active_or_passive: Map<String, Int>,
        val commission_free: Map<String, Int>,
        val smart_beta: Map<String, Int>,
        val currency_hedged: Map<String, Int>,
        val equity_regions: Map<String, Int>,
        val asset_class: Map<String, Int>,
        val dividend_frequency: Map<String, Int>
    ) {
        /*
        @Serializable
        data class Issuer(
            val ishares: Int,
            val fidelity: Int,
            val first-trust: Int,
            val invesco-powershares: Int,
            val proshares: Int,
            val direxion: Int,
            val state-street-spdr: Int,
            val us-commodity-funds: Int,
            val van-eck: Int,
            val vanguard: Int,
            val wisdomtree: Int,
            val deutsche-asset--wealth-management: Int,
            val ubs: Int,
            val global-x: Int,
            val goldman-sachs: Int,
            val indexiq: Int,
            val pimco: Int,
            val jp-morgan: Int,
            val advisorshares: Int,
            val alps: Int,
            val charles-schwab: Int,
            val credit-suisse: Int,
            val sprott-asset-management: Int,
            val teucrium: Int,
            val citigroup: Int,
            val morgan-stanley: Int,
            val quantshares: Int,
            val flexshares: Int,
            val exchange-traded-concepts: Int,
            val arrowshares: Int,
            val highland-capital: Int,
            val cambria: Int,
            val kraneshares: Int,
            val renaissance-capital: Int,
            val vident-financial: Int,
            val franklin-templeton-investments: Int,
            val horizons: Int,
            val merk-funds: Int,
            val wbi-shares: Int,
            val ark-investment-management: Int,
            val alpha-architect: Int,
            val validea-funds: Int,
            val reality-shares: Int,
            val csop-asset-management: Int,
            val innovator-management: Int,
            val us-global-investors: Int,
            val trimtabs: Int,
            val oshares-investments: Int,
            val virtus-etf-solutions: Int,
            val principal-financial-group: Int,
            val tortoise-capital: Int,
            val pacer-financial: Int,
            val john-hancock: Int,
            val iteq-etf-partners-llc: Int,
            val etf-managers-group: Int,
            val alphamark-advisors-llc: Int,
            val legg-mason-global-asset-management: Int,
            val janus-henderson-investors: Int,
            val victory-capital: Int,
            val amplify: Int,
            val aptus-capital-advisors: Int,
            val columbia-threadneedle-investments: Int,
            val barclays-capital: Int,
            val natixis-global-asset-management: Int,
            val premise: Int,
            val bmo-financial-group: Int,
            val davis-advisors: Int,
            val inspire-investing: Int,
            val hartford-funds: Int,
            val saba-capital-management: Int,
            val graniteshares: Int,
            val formulafolio-investments: Int,
            val exponential-etfs: Int,
            val clearshares: Int,
            val nuveen: Int,
            val transamerica-asset-management: Int,
            val spinnaker-trust: Int,
            val main-manangement: Int,
            val point-bridge-capital: Int,
            val nationwide: Int,
            val regents-park-funds: Int,
            val change-finance: Int,
            val active-weighting-advisors: Int,
            val sage-advisory: Int,
            val entrepreneurshares: Int,
            val eaton-vance-management: Int,
            val advisors-asset-management: Int,
            val sl-advisors: Int,
            val tierra-funds: Int,
            val swedish-export-credit-corporation: Int,
            val strategy-shares: Int,
            val american-century-investments: Int,
            val motley-fool-asset-management: Int,
            val metaurus-advisors: Int,
            val whitford-asset-management-llc: Int,
            val bp-capital-fund-advisors: Int,
            val cboe-vest-financial-llc: Int,
            val little-harbor-advisors: Int,
            val pgim-investments: Int,
            val aberdeen-standard-investments: Int,
            val salt-financial-llc: Int,
            val opus-capital-management: Int,
            val impact-shares: Int,
            val knowledge-leaders-capital: Int,
            val defiance-etfs-llc: Int,
            val perth-mint: Int,
            val redwood-investment-management: Int,
            val distillate-capital-partners-llc: Int,
            val tiger-brokers: Int,
            val equbot: Int,
            val gadsden-llc: Int,
            val cushing-asset-management-lp: Int,
            val syntax-advisors-llc: Int,
            val aware-asset-management: Int,
            val hoya-capital-real-estate-llc: Int,
            val ryzz-capital-management-ll: Int,
            val fundamental-income-strategies-llc: Int,
            val sofi-etfs: Int,
            val procuream-llc: Int,
            val timothy-partners-ltd: Int,
            val im-global-partner-us-llc: Int,
            val acquirers-funds-llc: Int,
            val quadratic-capital: Int,
            val roundhill-financial-llc: Int,
            val m-cam-international-llc: Int,
            val tuttle-tactical-management-llc: Int,
            val innovation-shares-llc: Int,
            val wahed-invest-llc: Int,
            val beyond-investing-llc: Int,
            val howard-capital-management-inc: Int,
            val liquid-strategies-llc: Int
        )

         */
    }

    @Serializable
    data class Data(
        val symbol: Symbol,
        val name: Name,
        val mobile_title: String,
        val ytd: String,
        val one_week_return: String,
        val four_week_return: String,
        val fifty_two_week: String,
        val three_ytd: String,
        val five_ytd: String,
        val realtime_performance: RealtimePerformance
    ) {
        @Serializable
        data class Symbol(
            val type: String,
            val text: String,
            val url: String
        )
        @Serializable
        data class Name(
            val type: String,
            val text: String,
            val url: String
        )
        @Serializable
        data class RealtimePerformance(
            val type: String,
            val url: String
        )
    }
}
