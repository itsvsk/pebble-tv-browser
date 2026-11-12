package com.tvbrowser.core.adblock

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal class FilterHttpException(val statusCode: Int) : IOException("Filter HTTP failure")

@Singleton
class EasyListLoader @Inject constructor(@ApplicationContext context: Context) {
    private val client by lazy { OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS).followSslRedirects(false).build() }
    private val repository = FilterRepository(FilterSnapshotStore(File(context.filesDir, "adblock/sources-v1")), ::fetch)
    val status = repository.status
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init { scope.launch { repository.initialize() } }

    suspend fun refresh(): Result<Int> = repository.refresh()
    fun shouldBlock(url: String, pageUrl: String): Boolean = repository.shouldBlock(url, pageUrl)

    private suspend fun fetch(source: FilterSource): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(source.url).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.use {
                        if (!it.isSuccessful) throw FilterHttpException(it.code)
                        val body = it.body ?: throw IOException("Missing filter content")
                        if (body.contentLength() > MAX_FILTER_BYTES) throw IOException("Filter list too large")
                        body.byteStream().use(::readBoundedFilterText)
                    }
                    if (continuation.isActive) continuation.resume(text)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        })
    }

}

internal fun readBoundedFilterText(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (output.size() + count > MAX_FILTER_BYTES) throw IOException("Filter list too large")
        output.write(buffer, 0, count)
    }
    return output.toString("UTF-8")
}
