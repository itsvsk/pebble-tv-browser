package com.tvbrowser.core.adblock

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdBlockInterceptor @Inject constructor(private val loader: EasyListLoader) {
    /** Kept for explicitly constructed WebViews in instrumentation fixtures. */
    constructor(context: Context) : this(EasyListLoader(context))
    fun shouldBlock(url: String, pageUrl: String): Boolean = loader.shouldBlock(url, pageUrl)
}
