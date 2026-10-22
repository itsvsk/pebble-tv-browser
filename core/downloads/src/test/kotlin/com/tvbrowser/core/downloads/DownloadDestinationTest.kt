package com.tvbrowser.core.downloads

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.io.File

class DownloadDestinationTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun unsafeNameUsesOnlyFinalComponent() {
        assertEquals("report.pdf", safeDownloadName("../private/report.pdf"))
        assertEquals("download", safeDownloadName(".."))
        assertEquals("download", safeDownloadName("  "))
        assertEquals("report.pdf", safeDownloadName("C:\\tmp\\report.pdf"))
    }

    @Test
    fun existingDownloadGetsDistinctFilename() {
        val directory = temporaryFolder.root
        File(directory, "report.pdf").writeText("existing")

        assertEquals("report (1).pdf", uniqueDownloadFile(directory, "report.pdf").name)
        assertEquals("existing", File(directory, "report.pdf").readText())
    }

    @Test
    fun pendingDownloadPathIsNotReusedBeforeFileExists() {
        val directory = temporaryFolder.root
        val occupied = setOf(File(directory, "report.pdf").absolutePath)

        assertEquals("report (1).pdf", uniqueDownloadFile(directory, "report.pdf", occupied).name)
    }
}
