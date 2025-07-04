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
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnBackPressedCallbackTest {

    @Test
    fun remove_fromMultipleDispatchers_doesNotThrowConcurrentModificationException() {
        val callback =
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    error("not implemented")
                }
            }

        repeat(times = 5) {
            // Adds the same callback to multiple dispatchers. This setup verifies that
            // `OnBackPressedCallback.remove()` handles these concurrent modifications to its
            // internal state without throwing a `ConcurrentModificationException`.
            val dispatcher = OnBackPressedDispatcher()
            dispatcher.addCallback(callback)
        }

        callback.remove()
    }

    @Test
    fun remove_fromMultipleCloseables_doesNotThrowConcurrentModificationException() {
        val callback =
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    error("not implemented")
                }
            }

        repeat(times = 5) {
            // Creates multiple closeables that will attempt to remove themselves from the callback
            // when closed. This setup verifies that `OnBackPressedCallback.remove()` handles these
            // concurrent modifications to its internal state without throwing a
            // `ConcurrentModificationException`.
            val closeable =
                object : AutoCloseable {
                    override fun close() {
                        callback.removeCloseable(closeable = this)
                    }
                }
            callback.addCloseable(closeable)
        }

        callback.remove()
    }

    @Test
    fun all_dispatchers_got_notified_for_enabled_changes() {
        val callback =
            object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackPressed() {
                    error("not implemented")
                }
            }
        var changedCount = 0

        repeat(times = 5) {
            val dispatcher =
                OnBackPressedDispatcher(
                    fallbackOnBackPressed = null,
                    onHasEnabledCallbacksChanged = { changedCount++ },
                )
            dispatcher.addCallback(callback)
        }

        callback.isEnabled = true

        assertThat(changedCount).isEqualTo(5)
    }
}
