package com.tvbrowser.core.adblock

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EasyListLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    companion object {
        private val URLS = listOf(
            "https://easylist.to/easylist/easylist.txt",
            "https://easylist.to/easylist/easyprivacy.txt",
        )
    }

    suspend fun refresh(): Result<Int> = runCatching {
        val accumulated = HashSet<String>(60_000)
        for (url in URLS) {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                resp.body?.byteStream()?.use { input ->
                    BufferedReader(InputStreamReader(input)).use { br ->
                        accumulated.addAll(EasyListParser.parse(br))
                    }
                }
            }
        }
        if (accumulated.isEmpty()) error("Empty parse result")
        val dir = File(context.filesDir, "adblock").apply { mkdirs() }
        File(dir, "hosts.txt").bufferedWriter().use { w ->
            for (h in accumulated.sorted()) {
                w.write("||$h^"); w.newLine()
            }
        }
        accumulated.size
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AdblockNetworkModule {
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient()
}
