/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.layout

import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensions.VENDOR_API_LEVEL_1
import androidx.window.extensions.WindowExtensionsProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An integration test to verify that if [WindowExtensionsProvider] is present then
 * [SafeWindowLayoutComponentProvider.windowLayoutComponent] will return a value. This can fail if
 * the implementation of window:extensions:extensions does not have the expected API.
 */
class SafeWindowLayoutComponentProviderTest {

    /**
     * Test that if [WindowExtensionsProvider] is available then
     * [SafeWindowLayoutComponentProvider.windowLayoutComponent] returns a non-null value.
     */
    @Test
    fun windowLayoutComponentIsAvailable_ifProviderIsAvailable() {
        val loader = SafeWindowLayoutComponentProviderTest::class.java.classLoader!!
        val consumerAdapter = ConsumerAdapter(loader)
        val safeProvider = SafeWindowLayoutComponentProvider(loader, consumerAdapter)
        val safeComponent = safeProvider.windowLayoutComponent

        try {
            val extensions = WindowExtensionsProvider.getWindowExtensions()
            val actualComponent = extensions.windowLayoutComponent
            if (actualComponent == null) {
                assertNull(safeComponent)
            } else {
                // TODO(b/267831038): verify upon each api level
                // TODO(b/267708462): more reliable test for testing actual method matching
                assertNotNull(safeComponent)
                assertTrue(safeProvider.isWindowLayoutComponentAccessible())
                when (ExtensionsUtil.safeVendorApiLevel) {
                    VENDOR_API_LEVEL_1 -> assertTrue(safeProvider.hasValidVendorApiLevel1())
                    else -> assertTrue(safeProvider.hasValidVendorApiLevel2())
                }
            }
        } catch (e: UnsupportedOperationException) {
            // Invalid implementation of extensions
            assertNull(safeComponent)
        }
    }
}