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

package androidx.navigationevent

import android.os.Build
import android.window.BackEvent.EDGE_LEFT
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class NavigationInputHandlerTest {

    @Test
    fun testSimpleInvoker() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        val callback =
            object : NavigationEventCallback(true) {
                override fun onEventCompleted() {}
            }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        callback.remove()

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerEnableDisable() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        val callback =
            object : NavigationEventCallback(true) {
                override fun onEventCompleted() {}
            }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)

        callback.isEnabled = true

        assertThat(registerCount).isEqualTo(2)
    }

    @Test
    fun testCallbackEnabledDisabled() {
        val callback =
            object : NavigationEventCallback(false) {
                override fun onEventCompleted() {
                    TODO("Not yet implemented")
                }
            }

        callback.isEnabled = true
        callback.isEnabled = false
    }

    @Test
    fun testInvokerAddDisabledCallback() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val callback =
            object : NavigationEventCallback(false) {
                override fun onEventCompleted() {}
            }

        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(0)

        callback.isEnabled = true

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerAddEnabledCallbackBeforeSet() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val callback =
            object : NavigationEventCallback(true) {
                override fun onEventCompleted() {}
            }

        dispatcher.addCallback(callback)

        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallback() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        var startedCount = 0
        var progressedCount = 0
        var cancelledCount = 0
        val callback =
            object : NavigationEventCallback(true) {
                override fun onEventStarted(event: NavigationEvent) {
                    startedCount++
                }

                override fun onEventProgressed(event: NavigationEvent) {
                    progressedCount++
                }

                override fun onEventCompleted() {}

                override fun onEventCancelled() {
                    cancelledCount++
                }
            }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        dispatcher.dispatchOnStarted(NavigationEvent(0.1F, 0.1F, 0.1F, EDGE_LEFT))
        assertThat(startedCount).isEqualTo(1)

        dispatcher.dispatchOnProgressed(NavigationEvent(0.1F, 0.1F, 0.1F, EDGE_LEFT))
        assertThat(progressedCount).isEqualTo(1)

        dispatcher.dispatchOnCancelled()
        assertThat(cancelledCount).isEqualTo(1)

        callback.remove()

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallbackRemovedCancel() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        var cancelledCount = 0
        val callback =
            object : NavigationEventCallback(true) {
                override fun onEventStarted(event: NavigationEvent) {}

                override fun onEventProgressed(event: NavigationEvent) {}

                override fun onEventCompleted() {}

                override fun onEventCancelled() {
                    cancelledCount++
                }
            }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        dispatcher.dispatchOnStarted(NavigationEvent(0.1F, 0.1F, 0.1F, EDGE_LEFT))

        callback.remove()
        assertThat(cancelledCount).isEqualTo(1)

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallbackRemovedCancelInHandleOnStarted() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        var cancelledCount = 0
        val callback =
            object : NavigationEventCallback(true) {
                override fun onEventStarted(event: NavigationEvent) {
                    this.remove()
                }

                override fun onEventProgressed(event: NavigationEvent) {}

                override fun onEventCompleted() {}

                override fun onEventCancelled() {
                    cancelledCount++
                }
            }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        dispatcher.dispatchOnStarted(NavigationEvent(0.1F, 0.1F, 0.1F, EDGE_LEFT))

        assertThat(cancelledCount).isEqualTo(1)

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallbackAddedContinue() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        var completedCount = 0
        val callback =
            object : NavigationEventCallback(true) {
                override fun onEventStarted(event: NavigationEvent) {}

                override fun onEventProgressed(event: NavigationEvent) {}

                override fun onEventCompleted() {
                    completedCount++
                }

                override fun onEventCancelled() {}
            }

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        dispatcher.dispatchOnStarted(NavigationEvent(0.1F, 0.1F, 0.1F, EDGE_LEFT))

        dispatcher.addCallback(
            object : NavigationEventCallback(true) {
                override fun onEventCompleted() {}
            }
        )

        dispatcher.dispatchOnCompleted()

        assertThat(completedCount).isEqualTo(1)
    }

    @Test
    fun testDoubleStartCallbackCausesCancel() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = NavigationInputHandler(dispatcher)

        inputHandler.setOnBackInvokedDispatcher(invoker)

        var cancelledCount = 0
        val callback1 =
            object : NavigationEventCallback(true) {
                override fun onEventStarted(event: NavigationEvent) {}

                override fun onEventProgressed(event: NavigationEvent) {}

                override fun onEventCompleted() {}

                override fun onEventCancelled() {
                    cancelledCount++
                }
            }

        dispatcher.addCallback(callback1)

        assertThat(registerCount).isEqualTo(1)

        dispatcher.dispatchOnStarted(NavigationEvent(0.1F, 0.1F, 0.1F, EDGE_LEFT))

        var startedCount2 = 0

        val callback2 =
            object : NavigationEventCallback(true) {
                override fun onEventStarted(event: NavigationEvent) {
                    startedCount2++
                }
            }

        dispatcher.addCallback(callback2)

        dispatcher.dispatchOnStarted(NavigationEvent(0.1F, 0.1F, 0.1F, EDGE_LEFT))

        assertThat(registerCount).isEqualTo(1)

        assertThat(cancelledCount).isEqualTo(1)

        assertThat(startedCount2).isEqualTo(1)
    }
}
