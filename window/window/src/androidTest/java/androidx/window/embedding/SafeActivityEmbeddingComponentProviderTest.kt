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

package androidx.window.embedding

import android.util.Log
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensions
import androidx.window.extensions.WindowExtensions.VENDOR_API_LEVEL_1
import androidx.window.extensions.WindowExtensionsProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An integration test to verify that if [WindowExtensionsProvider] is present then
 * [SafeActivityEmbeddingComponentProvider.activityEmbeddingComponent] will return a value.
 * This can fail if the implementation of window:extensions:extensions
 * does not have the expected API.
 */
class SafeActivityEmbeddingComponentProviderTest {

    // TODO(b/267708462) : add a more reliable test
    /**
     * Test that if [WindowExtensionsProvider] is available then
     * use [SafeActivityEmbeddingComponentProvider.activityEmbeddingComponent] to validate.
     * If [WindowExtensions.getActivityEmbeddingComponent] matches contract,
     * return a non-null value.
     * If it doesn't match, it will return a null.
     */
    @Test
    fun activityEmbeddingComponentIsAvailable_ifProviderIsAvailable() {
        val loader = SafeActivityEmbeddingComponentProviderTest::class.java.classLoader!!
        val consumerAdapter = ConsumerAdapter(loader)
        val windowExtensions: WindowExtensions = try {
            WindowExtensionsProvider.getWindowExtensions()
        } catch (e: UnsupportedOperationException) {
            Log.d(TAG, "Device doesn't have WindowExtensions available")
            return
        }
        val safeProvider = SafeActivityEmbeddingComponentProvider(
            loader,
            consumerAdapter,
            windowExtensions
        )
        val safeComponent = safeProvider.activityEmbeddingComponent
        try {
            val actualComponent = windowExtensions.activityEmbeddingComponent
            if (actualComponent == null) {
                assertNull(safeComponent)
            } else {
                // TODO(b/267573854) : verify upon each api level
                // TODO(b/267708462) : more reliable test for testing actual method matching
                assertNotNull(safeComponent)
                assertTrue(safeProvider.isActivityEmbeddingComponentAccessible())
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

    companion object {
        private const val TAG = "SafeActivityEmbeddingComponentProviderTest"
    }
}