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
package androidx.window.layout.adapter.sidecar

import android.app.Activity
import android.content.Context
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.TestConsumer
import androidx.window.WindowTestBase
import androidx.window.core.Bounds
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.HardwareFoldingFeature
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.HINGE
import androidx.window.layout.WindowLayoutInfo
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/** Tests for [SidecarWindowBackend] class. */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class SidecarWindowBackendTest : WindowTestBase() {
    private lateinit var context: Context

    @Before
    public fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SidecarWindowBackend.resetInstance()
    }

    @Test
    public fun testGetInstance() {
        val backend = SidecarWindowBackend.getInstance(context)
        assertNotNull(backend)

        // Verify that getInstance always returns the same value
        val newBackend = SidecarWindowBackend.getInstance(context)
        assertEquals(backend, newBackend)
    }

    @Test
    public fun testInitAndVerifySidecar() {
        val sidecarVersion = SidecarCompat.sidecarVersion
        assumeTrue(sidecarVersion != null)
        assertTrue(SidecarWindowBackend.isSidecarVersionSupported(sidecarVersion))
        val sidecar = SidecarWindowBackend.initAndVerifyExtension(context)
        assertNotNull(sidecar)
        assertTrue(sidecar is SidecarCompat)
        assertTrue(sidecar!!.validateExtensionInterface())
    }

    @Test
    public fun testRegisterLayoutChangeCallback() {
        activityTestRule.scenario.onActivity { activity ->
            val backend = SidecarWindowBackend.getInstance(context)
            backend.windowExtension = mock()
            // Check registering the layout change callback
            val consumer = mock<Consumer<WindowLayoutInfo>>()

            backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
            assertEquals(1, backend.windowLayoutChangeCallbacks.size.toLong())
            verify(backend.windowExtension!!).onWindowLayoutChangeListenerAdded(activity)

            // Check unregistering the layout change callback
            backend.unregisterLayoutChangeCallback(consumer)
            assertTrue(backend.windowLayoutChangeCallbacks.isEmpty())
            verify(backend.windowExtension!!).onWindowLayoutChangeListenerRemoved(eq(activity))
        }
    }

    @Test
    public fun testRegisterLayoutChangeCallback_callsExtensionOnce() {
        activityTestRule.scenario.onActivity { activity ->
            val backend = SidecarWindowBackend.getInstance(context)
            backend.windowExtension = mock()

            // Check registering the layout change callback
            val consumer = mock<Consumer<WindowLayoutInfo>>()

            backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
            backend.registerLayoutChangeCallback(activity, Runnable::run, mock())
            assertEquals(2, backend.windowLayoutChangeCallbacks.size.toLong())
            verify(backend.windowExtension!!).onWindowLayoutChangeListenerAdded(activity)

            // Check unregistering the layout change callback
            backend.unregisterLayoutChangeCallback(consumer)
            assertEquals(1, backend.windowLayoutChangeCallbacks.size.toLong())
            verify(backend.windowExtension!!, times(0))
                .onWindowLayoutChangeListenerRemoved(eq(activity))
        }
    }

    @Test
    public fun testRegisterLayoutChangeCallback_clearListeners() {
        activityTestRule.scenario.onActivity { activity ->
            val backend = SidecarWindowBackend.getInstance(context)
            backend.windowExtension = mock()

            // Check registering the layout change callback
            val firstConsumer = mock<Consumer<WindowLayoutInfo>>()
            val secondConsumer = mock<Consumer<WindowLayoutInfo>>()

            backend.registerLayoutChangeCallback(
                activity,
                { obj: Runnable -> obj.run() },
                firstConsumer
            )
            backend.registerLayoutChangeCallback(
                activity,
                { obj: Runnable -> obj.run() },
                secondConsumer
            )

            // Check unregistering the layout change callback
            backend.unregisterLayoutChangeCallback(firstConsumer)
            backend.unregisterLayoutChangeCallback(secondConsumer)
            assertTrue(backend.windowLayoutChangeCallbacks.isEmpty())
            verify(backend.windowExtension!!).onWindowLayoutChangeListenerRemoved(activity)
        }
    }

    @Test
    public fun testLayoutChangeCallback_emitNewValue() {
        activityTestRule.scenario.onActivity { activity ->
            val backend = SidecarWindowBackend.getInstance(context)
            backend.windowExtension = mock()

            // Check that callbacks from the extension are propagated correctly
            val consumer = mock<Consumer<WindowLayoutInfo>>()

            backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
            val windowLayoutInfo = newTestWindowLayoutInfo()
            val backendListener = backend.ExtensionListenerImpl()
            backendListener.onWindowLayoutChanged(activity, windowLayoutInfo)
            verify(consumer).accept(eq(windowLayoutInfo))
        }
    }

    @Test
    public fun testWindowLayoutInfo_updatesOnSubsequentRegistration() {
        val interfaceCompat = SwitchOnUnregisterExtensionInterfaceCompat()
        val backend = SidecarWindowBackend(interfaceCompat)
        val activity = mock<Activity>()
        val consumer = TestConsumer<WindowLayoutInfo>()
        val expected = mutableListOf<WindowLayoutInfo>()
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
        expected.add(interfaceCompat.currentWindowLayoutInfo())
        backend.unregisterLayoutChangeCallback(consumer)
        backend.registerLayoutChangeCallback(activity, Runnable::run, consumer)
        expected.add(interfaceCompat.currentWindowLayoutInfo())
        backend.unregisterLayoutChangeCallback(consumer)
        consumer.assertValues(expected)
    }

    @Test
    public fun testWindowLayoutInfo_secondCallbackUpdatesOnRegistration() {
        val interfaceCompat = SwitchOnUnregisterExtensionInterfaceCompat()
        val backend = SidecarWindowBackend(interfaceCompat)
        val activity = mock<Activity>()
        val firstConsumer = TestConsumer<WindowLayoutInfo>()
        val secondConsumer = TestConsumer<WindowLayoutInfo>()
        val executor = MoreExecutors.directExecutor()
        val firstExpected = mutableListOf<WindowLayoutInfo>()
        val secondExpected = mutableListOf<WindowLayoutInfo>()
        backend.registerLayoutChangeCallback(activity, executor, firstConsumer)
        firstExpected.add(interfaceCompat.currentWindowLayoutInfo())
        backend.registerLayoutChangeCallback(activity, executor, secondConsumer)
        secondExpected.add(interfaceCompat.currentWindowLayoutInfo())
        backend.unregisterLayoutChangeCallback(firstConsumer)
        backend.unregisterLayoutChangeCallback(secondConsumer)
        firstConsumer.assertValues(firstExpected)
        secondConsumer.assertValues(secondExpected)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testSupportedWindowFeatures_throws() {
        val interfaceCompat = SwitchOnUnregisterExtensionInterfaceCompat()
        val backend = SidecarWindowBackend(interfaceCompat)
        backend.supportedPostures
    }

    internal companion object {
        private fun newTestWindowLayoutInfo(): WindowLayoutInfo {
            val feature1: DisplayFeature = HardwareFoldingFeature(Bounds(0, 2, 3, 4), HINGE, FLAT)
            val feature2: DisplayFeature = HardwareFoldingFeature(Bounds(0, 1, 5, 1), HINGE, FLAT)
            val displayFeatures = listOf(feature1, feature2)
            return WindowLayoutInfo(displayFeatures)
        }
    }
}
