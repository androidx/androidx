/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnBackPressedDispatcherTest {

    private lateinit var dispatcher: OnBackPressedDispatcher

    @Before
    fun setup() {
        dispatcher = OnBackPressedDispatcher()
    }

    @UiThreadTest
    @Test
    fun testRegisterCallback() {
        var count = 0
        val callback = dispatcher.addCallback {
            count++
        }
        assertWithMessage("Callback should be enabled by default")
            .that(callback.isEnabled)
            .isTrue()
        assertWithMessage("Dispatcher should have an enabled callback")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()

        dispatcher.onBackPressed()

        assertWithMessage("Count should be incremented after onBackPressed")
            .that(count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testRegisterDisabledCallback() {
        var count = 0
        val callback = dispatcher.addCallback(enabled = false) {
            count++
        }
        assertWithMessage("Callback should be disabled by default")
            .that(callback.isEnabled)
            .isFalse()
        assertWithMessage("Dispatcher should not have an enabled callback")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()

        callback.isEnabled = true

        assertWithMessage("Dispatcher should have an enabled callback after setting isEnabled")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallback() {
        val lifecycleOwner = object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this)

            override fun getLifecycle() = lifecycleRegistry
        }
        var count = 0
        dispatcher.addCallback(lifecycleOwner) {
            count++
        }
        assertWithMessage("Handler should return false if the Lifecycle isn't started")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(count)
            .isEqualTo(0)

        // Now start the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        dispatcher.onBackPressed()
        assertWithMessage("Once the callbacks is started, the count should increment")
            .that(count)
            .isEqualTo(1)

        // Now stop the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertWithMessage("Handler should return false if the Lifecycle isn't started")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testIsEnabledWithinCallback() {
        var count = 0
        val callback = dispatcher.addCallback {
            count++
            isEnabled = false
        }
        assertWithMessage("Callback should be enabled by default")
            .that(callback.isEnabled)
            .isTrue()
        assertWithMessage("Dispatcher should have an enabled callback")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()

        dispatcher.onBackPressed()

        assertWithMessage("Count should be incremented after onBackPressed")
            .that(count)
            .isEqualTo(1)
        assertWithMessage("Callback should be disabled after onBackPressed()")
            .that(callback.isEnabled)
            .isFalse()
        assertWithMessage("Dispatcher should have no enabled callbacks")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testRemoveWithinCallback() {
        var count = 0
        dispatcher.addCallback {
            count++
            remove()
        }

        dispatcher.onBackPressed()

        assertWithMessage("Count should be incremented after onBackPressed")
            .that(count)
            .isEqualTo(1)
        assertWithMessage("Dispatcher should have no enabled callbacks after remove")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }
}
