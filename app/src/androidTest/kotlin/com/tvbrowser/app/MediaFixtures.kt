package com.tvbrowser.app

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun silentAudioFixture(): String {
    // Eight seconds of 8 kHz mono PCM silence. No network or audible output required.
    val sampleBytes = 8_000 * 8 * 2
    val bytes = ByteBuffer.allocate(44 + sampleBytes).order(ByteOrder.LITTLE_ENDIAN)
    bytes.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + sampleBytes)
    bytes.put("WAVEfmt ".toByteArray(Charsets.US_ASCII)).putInt(16)
    bytes.putShort(1).putShort(1).putInt(8_000).putInt(16_000).putShort(2).putShort(16)
    bytes.put("data".toByteArray(Charsets.US_ASCII)).putInt(sampleBytes)
    val audio = Base64.encodeToString(bytes.array(), Base64.NO_WRAP)
    return """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body><p>Offline silent media fixture</p><audio id="media" loop preload="auto" src="data:audio/wav;base64,$audio"></audio></body></html>"""
}
