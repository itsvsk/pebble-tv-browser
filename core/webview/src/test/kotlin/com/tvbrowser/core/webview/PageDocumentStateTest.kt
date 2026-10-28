package com.tvbrowser.core.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageDocumentStateTest {
    private val old = "https://old.test/page"
    private val next = "https://next.test/page"

    @Test fun pendingFailureBelongsToStartedUrlWhileNativeStillShowsPreviousPage() {
        val state = PageDocumentState()
        state.started(next, old)
        assertTrue(state.isCurrent(next, old))
        assertFalse(state.isCurrent(old, old))
    }

    @Test fun advancingNativeUrlRejectsBothEarlierStartedAndCommittedDocuments() {
        val state = PageDocumentState()
        state.started(next, old)
        val newer = "https://newer.test/page"
        assertTrue(state.isCurrent(newer, newer))
        assertFalse(state.isCurrent(next, newer))
        assertFalse(state.isCurrent(old, newer))
    }

    @Test fun redirectAndLateFinishCannotRestoreTheEarlierPendingTarget() {
        val state = PageDocumentState()
        state.started(next, old)
        val redirect = "https://redirect.test/page"
        state.started(redirect, old)
        state.finished(next)
        assertTrue(state.isCurrent(redirect, old))
        assertFalse(state.isCurrent(next, old))
    }

    @Test fun completedDataDocumentUsesItsNativeIdentity() {
        val state = PageDocumentState()
        state.started("about:blank", null)
        state.finished("about:blank")
        val native = "data:text/html;charset=utf-8;base64,"
        assertTrue(state.isCurrent(native, native))
        assertFalse(state.isCurrent("about:blank", native))
    }

    @Test fun schemeAndFragmentNormalizationAppliesToBothSnapshotAndRequest() {
        val state = PageDocumentState()
        state.started("HTTPS://next.test/page#one", "HTTPS://old.test/page#one")
        assertTrue(state.isCurrent("https://next.test/page#two", "https://old.test/page#two"))
    }
}
