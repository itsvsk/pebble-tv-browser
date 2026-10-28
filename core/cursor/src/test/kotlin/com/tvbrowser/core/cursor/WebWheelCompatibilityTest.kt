package com.tvbrowser.core.cursor

import org.junit.Assert.assertEquals
import org.junit.Test

class WebWheelCompatibilityTest {
    @Test fun olderChromiumProvidersNeedReversedHorizontalTicks() {
        for (major in listOf(53, 114, 144)) {
            assertEquals(-1f, horizontalWheelSignForUserAgent("Mozilla/5.0 Chrome/$major.0.0.0 Mobile Safari/537.36"))
        }
    }

    @Test fun fixedProvidersKeepTheCurrentHorizontalDirection() {
        for (major in listOf(145, 155)) {
            assertEquals(1f, horizontalWheelSignForUserAgent("Mozilla/5.0 Chrome/$major.0.0.0 Mobile Safari/537.36"))
        }
    }

    @Test fun unidentifiedProvidersKeepTheExistingMapping() {
        assertEquals(1f, horizontalWheelSignForUserAgent("Unknown TV browser"))
    }
}
