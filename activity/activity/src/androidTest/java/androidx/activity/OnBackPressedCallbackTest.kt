/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnBackPressedCallbackTest {

    @Test
    fun testCannotAddCallbackToMultipleDispatchers() {
        val callback =
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {}
            }

        val dispatcher1 = OnBackPressedDispatcher()
        dispatcher1.addCallback(callback)

        val dispatcher2 = OnBackPressedDispatcher()
        assertThrows(IllegalStateException::class.java) { dispatcher2.addCallback(callback) }
    }

    @Test
    fun testCannotAddCallbackMultipleTimesToSameDispatcher() {
        val callback =
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {}
            }

        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(callback)

        assertThrows(IllegalStateException::class.java) { dispatcher.addCallback(callback) }
    }

    @Test
    fun testDispatcherGotNotifiedForEnabledChanges() {
        val callback =
            object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackPressed() {}
            }

        var hasEnabledCallbacksResult = false
        var notificationCount = 0

        val dispatcher =
            OnBackPressedDispatcher(
                fallbackOnBackPressed = null,
                onHasEnabledCallbacksChanged = { hasEnabledCallbacks ->
                    hasEnabledCallbacksResult = hasEnabledCallbacks
                    notificationCount++
                },
            )

        // Adding the callback triggers the initial state emission.
        dispatcher.addCallback(callback)
        assertThat(notificationCount).isEqualTo(1)
        assertThat(hasEnabledCallbacksResult).isFalse()

        // Enabling the callback should notify the dispatcher.
        callback.isEnabled = true
        assertThat(notificationCount).isEqualTo(2)
        assertThat(hasEnabledCallbacksResult).isTrue()

        // Disabling it should notify the dispatcher again.
        callback.isEnabled = false
        assertThat(notificationCount).isEqualTo(3)
        assertThat(hasEnabledCallbacksResult).isFalse()
    }
}
