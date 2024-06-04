/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.window.area

import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNAVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.area.adapter.WindowAreaAdapter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.WindowAreaComponent.STATUS_ACTIVE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_AVAILABLE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_UNAVAILABLE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_UNSUPPORTED
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for [WindowAreaAdapter]. */
@ExperimentalWindowApi
class WindowAreaAdapterTest {

    @Test
    fun testWindowAreaAdapterUnsupported_vendorApiLevel3() {
        assertEquals(
            WindowAreaAdapter.translate(
                STATUS_UNSUPPORTED,
                sessionActive = false,
                vendorApiLevel = 3
            ),
            WINDOW_AREA_STATUS_UNSUPPORTED
        )
    }

    @Test
    fun testWindowAreaAdapterAvailable_vendorApiLevel3() {
        assertEquals(
            WindowAreaAdapter.translate(
                STATUS_AVAILABLE,
                sessionActive = false,
                vendorApiLevel = 3
            ),
            WINDOW_AREA_STATUS_AVAILABLE
        )
    }

    @Test
    fun testWindowAreaAdapterUnavailable_vendorApiLevel3() {
        assertEquals(
            WindowAreaAdapter.translate(
                STATUS_UNAVAILABLE,
                sessionActive = false,
                vendorApiLevel = 3
            ),
            WINDOW_AREA_STATUS_UNAVAILABLE
        )

        // Verify that if there is a session Active, we return ACTIVE
        assertEquals(
            WindowAreaAdapter.translate(
                STATUS_UNAVAILABLE,
                sessionActive = true,
                vendorApiLevel = 3
            ),
            WINDOW_AREA_STATUS_ACTIVE
        )
    }

    @Test
    fun testWindowAreaAdapterActive_vendorApiLevel3() {
        assertEquals(
            WindowAreaAdapter.translate(STATUS_ACTIVE, sessionActive = false, vendorApiLevel = 3),
            WINDOW_AREA_STATUS_ACTIVE
        )
    }

    @Test
    fun testWindowAreaAdapterUnsupported_vendorApiLevel4() {
        assertEquals(
            WindowAreaAdapter.translate(
                STATUS_UNSUPPORTED,
                sessionActive = false,
                vendorApiLevel = 4
            ),
            WINDOW_AREA_STATUS_UNSUPPORTED
        )
    }

    @Test
    fun testWindowAreaAdapterAvailable_vendorApiLevel4() {
        assertEquals(
            WindowAreaAdapter.translate(
                STATUS_AVAILABLE,
                sessionActive = false,
                vendorApiLevel = 4
            ),
            WINDOW_AREA_STATUS_AVAILABLE
        )
    }

    @Test
    fun testWindowAreaAdapterUnavailable_vendorApiLevel4() {
        assertEquals(
            WindowAreaAdapter.translate(
                STATUS_UNAVAILABLE,
                sessionActive = false,
                vendorApiLevel = 4
            ),
            WINDOW_AREA_STATUS_UNAVAILABLE
        )
    }

    @Test
    fun testWindowAreaAdapterActive_vendorApiLevel4() {
        assertEquals(
            WindowAreaAdapter.translate(STATUS_ACTIVE, sessionActive = false, vendorApiLevel = 4),
            WINDOW_AREA_STATUS_ACTIVE
        )
    }
}
