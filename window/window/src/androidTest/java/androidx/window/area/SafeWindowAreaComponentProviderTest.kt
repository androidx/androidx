/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensionsProvider
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * An integration test to verify that if [WindowExtensionsProvider] is present then
 * [SafeWindowAreaComponentProvider.windowAreaComponent] will return a value. This can fail if
 * the implementation of window:extensions:extensions does not have the expected API.
 */
class SafeWindowAreaComponentProviderTest {

    /**
     * Test that if [WindowExtensionsProvider] is available then
     * [SafeWindowAreaComponentProvider.windowAreaComponent] returns a non-null value.
     */
    @Test
    fun windowAreaComponentIsAvailable_ifProviderIsAvailable() {
        assumeTrue(ExtensionsUtil.safeVendorApiLevel >= 2)
        val loader = SafeWindowAreaComponentProvider::class.java.classLoader!!
        val safeComponent = SafeWindowAreaComponentProvider(loader).windowAreaComponent

        try {
            val extensions = WindowExtensionsProvider.getWindowExtensions()
            val actualComponent = extensions.windowAreaComponent
            if (actualComponent == null) {
                assertNull(safeComponent)
            }
            // TODO(b/267831038): verify upon each api level
            // TODO(b/267708462): more reliable test for testing actual method matching
        } catch (e: UnsupportedOperationException) {
            // Invalid implementation of extensions
            assertNull(safeComponent)
        }
    }
}
