package com.tvbrowser.core.adblock

enum class FilterSource(val title: String, val cacheName: String, val url: String) {
    Ads("EasyList", "easylist.snapshot", "https://easylist.to/easylist/easylist.txt"),
    Trackers("EasyPrivacy", "easyprivacy.snapshot", "https://easylist.to/easylist/easyprivacy.txt"),
}

data class FilterListStatus(
    val source: FilterSource,
    val ruleCount: Int = 0,
    val unsupportedRules: Int = 0,
    val updatedAtMillis: Long? = null,
    val bundled: Boolean = true,
    val error: String? = null,
)

data class FilterStatus(
    val ready: Boolean = false,
    val refreshing: Boolean = false,
    val lists: List<FilterListStatus> = emptyList(),
)

/** Small hand-maintained startup fallback, not a full copy of either subscription. */
internal object BundledFilters {
    val lists: Map<FilterSource, String> = mapOf(
        FilterSource.Ads to """
            ||doubleclick.net^
            ||googlesyndication.com^
            ||googleadservices.com^
            ||googletagservices.com^
            ||adservice.google.com^
            ||adnxs.com^
            ||criteo.com^
            ||criteo.net^
            ||taboola.com^
            ||outbrain.com^
            ||moatads.com^
            ||rubiconproject.com^
            ||pubmatic.com^
            ||openx.net^
            ||adcolony.com^
            ||applovin.com^
            ||mopub.com^
            ||zedo.com^
            ||casalemedia.com^
            ||smartadserver.com^
            ||liadm.com^
            ||advertising.com^
            ||yieldlab.net^
            ||turn.com^
            ||bidswitch.net^
            ||adsafeprotected.com^
            ||serving-sys.com^
            ||doubleverify.com^
        """.trimIndent(),
        FilterSource.Trackers to """
            ||google-analytics.com^
            ||googletagmanager.com^
            ||scorecardresearch.com^
            ||quantserve.com^
            ||chartbeat.com^
            ||hotjar.com^
            ||segment.io^
            ||mixpanel.com^
            ||amplitude.com^
            ||branch.io^
        """.trimIndent(),
    )
}
