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
class OnBackInvokedInputTest {

    @Test
    fun testSimpleInvoker() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}
        val input = OnBackInvokedInput(invoker)
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

        val input = OnBackInvokedInput(invoker)
        dispatcher.addInput(input)

        val handler = TestNavigationEventHandler()

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(1)

        handler.isBackEnabled = false

        assertThat(invoker.unregisterCount).isEqualTo(1)

        handler.isBackEnabled = true

        assertThat(invoker.registerCount).isEqualTo(2)
    }

    @Test
    fun testhandlerEnabledDisabled() {
        val handler = TestNavigationEventHandler(isBackEnabled = false)
        assertThat(handler.isBackEnabled).isFalse()

        handler.isBackEnabled = true
        assertThat(handler.isBackEnabled).isTrue()

        handler.isBackEnabled = false
        assertThat(handler.isBackEnabled).isFalse()
    }

    @Test
    fun testInvokerAddDisabledhandler() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val handler = TestNavigationEventHandler(isBackEnabled = false)

        val input = OnBackInvokedInput(invoker)
        dispatcher.addInput(input)

        dispatcher.addHandler(handler)

        assertThat(invoker.registerCount).isEqualTo(0)

        handler.isBackEnabled = true

        assertThat(invoker.registerCount).isEqualTo(1)

        handler.isBackEnabled = false

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerAddEnabledhandlerBeforeSet() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val handler = TestNavigationEventHandler()

        dispatcher.addHandler(handler)

        val input = OnBackInvokedInput(invoker)
        dispatcher.addInput(input)

        assertThat(invoker.registerCount).isEqualTo(1)

        handler.isBackEnabled = false

        assertThat(invoker.unregisterCount).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testSimpleAnimatedhandler() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedInput(invoker)
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
    fun testSimpleAnimatedhandlerRemovedCancel() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedInput(invoker)
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
    fun testSimpleAnimatedhandlerRemovedCancelInHandleOnStarted() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedInput(invoker)
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
    fun testSimpleAnimatedhandlerAddedContinue() {
        val invoker = TestOnBackInvokedDispatcher()
        val dispatcher = NavigationEventDispatcher {}

        val input = OnBackInvokedInput(invoker)
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
    @SdkSuppress(minSdkVersion = 34)
    fun testDoubleStarthandlerCausesCancel() {
        val invoker = TestOnBackInvokedDispatcher()

        val dispatcher = NavigationEventDispatcher {}
        val input = OnBackInvokedInput(invoker)
        dispatcher.addInput(input)

        val handler1 = TestNavigationEventHandler()

        dispatcher.addHandler(handler1)

        assertThat(invoker.registerCount).isEqualTo(1)

        invoker.dispatchOnBackStarted(TestBackEvent())

        val handler2 = TestNavigationEventHandler()

        dispatcher.addHandler(handler2)

        invoker.dispatchOnBackStarted(TestBackEvent())

        assertThat(invoker.registerCount).isEqualTo(1)

        assertThat(handler1.onBackCancelledInvocations).isEqualTo(1)

        assertThat(handler2.onBackStartedInvocations).isEqualTo(1)
    }
}
