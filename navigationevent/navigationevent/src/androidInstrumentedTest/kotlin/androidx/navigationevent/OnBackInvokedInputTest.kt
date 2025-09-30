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

import android.window.OnBackInvokedDispatcher
import androidx.navigationevent.testing.TestNavigationEventHandler
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class OnBackInvokedDefaultInputTest {

    @Test
    fun testSimpleInvoker() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}
        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        val handler = TestNavigationEventHandler()

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(1)

        handler.remove()

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerEnableDisable() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        val handler = TestNavigationEventHandler(isBackEnabled = true, isForwardEnabled = false)

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(1)

        handler.isBackEnabled = false

        assertThat(invoker.unregisterCount).isEqualTo(1)

        handler.isBackEnabled = true

        assertThat(invoker.registerCount).isEqualTo(2)
    }

    @Test
    fun testHandlerEnabledDisabled() {
        val handler = TestNavigationEventHandler(isBackEnabled = false, isForwardEnabled = false)
        assertThat(handler.isBackEnabled).isFalse()

        handler.isBackEnabled = true
        assertThat(handler.isBackEnabled).isTrue()

        handler.isBackEnabled = false
        assertThat(handler.isBackEnabled).isFalse()
    }

    @Test
    fun testInvokerAddDisabledHandler() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val handler = TestNavigationEventHandler(isBackEnabled = false, isForwardEnabled = false)

        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(0)

        handler.isBackEnabled = true

        assertThat(invoker.registerCount).isEqualTo(1)

        handler.isBackEnabled = false

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerAddEnabledHandlerBeforeSet() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val handler = TestNavigationEventHandler(isBackEnabled = true, isForwardEnabled = false)

        dispatcher.addHandler(handler)

        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        assertThat(invoker.registerCount).isEqualTo(1)

        handler.isBackEnabled = false

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testSimpleAnimatedHandler() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        val handler = TestNavigationEventHandler()

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(1)

        invoker.dispatchOnBackStarted(TestBackEvent())
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)

        invoker.dispatchOnBackProgressed(TestBackEvent())
        assertThat(handler.onBackProgressedInvocations).isEqualTo(1)

        invoker.dispatchOnBackCancelled()
        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)

        handler.remove()

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testSimpleAnimatedHandlerRemovedCancel() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        val handler = TestNavigationEventHandler()

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(1)

        invoker.dispatchOnBackStarted(TestBackEvent())

        handler.remove()
        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testSimpleAnimatedHandlerRemovedCancelInHandleOnStarted() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        val handler = TestNavigationEventHandler(onBackStarted = { remove() })

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(1)

        invoker.dispatchOnBackStarted(TestBackEvent())

        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testSimpleAnimatedHandlerAddedContinue() {
        val invoker = TestOnBackInvokedDispatcher()
        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(input)

        val handler = TestNavigationEventHandler()

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(1)

        invoker.dispatchOnBackStarted(TestBackEvent())

        dispatcher.addHandler(TestNavigationEventHandler())

        invoker.dispatchOnBackInvoked()

        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun testDefaultPriority() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher()
        dispatcher.addInput(OnBackInvokedDefaultInput(invoker))

        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_DEFAULT)

        assertThat(invoker.registerCount).isEqualTo(1)
        assertThat(invoker.priority).isEqualTo(OnBackInvokedDispatcher.PRIORITY_DEFAULT)
    }

    @Test
    fun testOverlayPriority() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher()
        dispatcher.addInput(OnBackInvokedOverlayInput(invoker))

        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_OVERLAY)

        assertThat(invoker.registerCount).isEqualTo(1)
        assertThat(invoker.priority).isEqualTo(OnBackInvokedDispatcher.PRIORITY_OVERLAY)
    }

    @Test
    fun defaultInputCanDispatchToOverlayHandler() {
        val invoker = TestOnBackInvokedDispatcher()
        val dispatcher = NavigationEventDispatcher()
        val defaultInput = OnBackInvokedDefaultInput(invoker)
        dispatcher.addInput(defaultInput, NavigationEventDispatcher.PRIORITY_DEFAULT)

        val defaultHandler = TestNavigationEventHandler()
        dispatcher.addHandler(defaultHandler, NavigationEventDispatcher.PRIORITY_DEFAULT)

        val overlayHandler = TestNavigationEventHandler()
        dispatcher.addHandler(overlayHandler, NavigationEventDispatcher.PRIORITY_OVERLAY)

        invoker.dispatchOnBackInvoked()
        assertThat(defaultHandler.onBackCompletedInvocations).isEqualTo(0)
        assertThat(overlayHandler.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun overlayInputCanNotDispatchToDefaultHandler() {
        val invoker = TestOnBackInvokedDispatcher()
        val dispatcher = NavigationEventDispatcher()
        val overlayInput = OnBackInvokedOverlayInput(invoker)
        dispatcher.addInput(overlayInput, NavigationEventDispatcher.PRIORITY_OVERLAY)

        val defaultHandler = TestNavigationEventHandler()
        dispatcher.addHandler(defaultHandler, NavigationEventDispatcher.PRIORITY_DEFAULT)

        invoker.dispatchOnBackInvoked()
        assertThat(defaultHandler.onBackCompletedInvocations).isEqualTo(0)
    }
}
