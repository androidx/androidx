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
package androidx.window

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

/** Tests for [ExtensionWindowBackend] class.  */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class ExtensionWindowBackendTest : WindowTestBase() {
    private lateinit var context: Context

    @Before
    public fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ExtensionWindowBackend.resetInstance()
    }

    @Test
    public fun testGetInstance() {
        val backend = ExtensionWindowBackend.getInstance(context)
        assertNotNull(backend)

        // Verify that getInstance always returns the same value
        val newBackend = ExtensionWindowBackend.getInstance(context)
        assertEquals(backend, newBackend)
    }

    @Test
    public fun testInitAndVerifyExtension() {
        val extensionVersion = ExtensionCompat.getExtensionVersion()
        assumeTrue(extensionVersion != null)
        assertTrue(ExtensionWindowBackend.isExtensionVersionSupported(extensionVersion))
        val extension = ExtensionWindowBackend.initAndVerifyExtension(context)
        assertNotNull(extension)
        assertTrue(extension is ExtensionCompat)
        assertTrue(extension!!.validateExtensionInterface())
    }

    @Test
    public fun testInitAndVerifySidecar() {
        val sidecarVersion = SidecarCompat.getSidecarVersion()
        assumeTrue(sidecarVersion != null)
        assertTrue(ExtensionWindowBackend.isExtensionVersionSupported(sidecarVersion))
        val sidecar = ExtensionWindowBackend.initAndVerifyExtension(context)
        assertNotNull(sidecar)
        assertTrue(sidecar is SidecarCompat)
        assertTrue(sidecar!!.validateExtensionInterface())
    }

    @Test
    public fun testRegisterLayoutChangeCallback() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = Mockito.mock(
            ExtensionInterfaceCompat::class.java
        )

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = Mockito.mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = activityTestRule.launchActivity(Intent())
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size.toLong())
        Mockito.verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity)

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer)
        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty())
        Mockito.verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(
            ArgumentMatchers.eq(activity)
        )
    }

    @Test
    public fun testRegisterLayoutChangeCallback_callsExtensionOnce() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = Mockito.mock(
            ExtensionInterfaceCompat::class.java
        )

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = Mockito.mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = activityTestRule.launchActivity(Intent())
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        backend.registerLayoutChangeCallback(
            activity, { obj: Runnable -> obj.run() },
            Mockito.mock(
                WindowLayoutInfoConsumer::class.java
            )
        )
        assertEquals(2, backend.mWindowLayoutChangeCallbacks.size.toLong())
        Mockito.verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity)

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer)
        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size.toLong())
        Mockito.verify(backend.mWindowExtension, Mockito.times(0))
            .onWindowLayoutChangeListenerRemoved(ArgumentMatchers.eq(activity))
    }

    @Test
    public fun testRegisterLayoutChangeCallback_clearListeners() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = Mockito.mock(
            ExtensionInterfaceCompat::class.java
        )

        // Check registering the layout change callback
        val firstConsumer: Consumer<WindowLayoutInfo> = Mockito.mock(
            WindowLayoutInfoConsumer::class.java
        )
        val secondConsumer: Consumer<WindowLayoutInfo> = Mockito.mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = activityTestRule.launchActivity(Intent())
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
        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty())
        Mockito.verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(activity)
    }

    @Test
    public fun testLayoutChangeCallback_emitNewValue() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = Mockito.mock(
            ExtensionInterfaceCompat::class.java
        )

        // Check that callbacks from the extension are propagated correctly
        val consumer: Consumer<WindowLayoutInfo> = Mockito.mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = activityTestRule.launchActivity(Intent())
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        val windowLayoutInfo = newTestWindowLayoutInfo()
        val backendListener = backend.ExtensionListenerImpl()
        backendListener.onWindowLayoutChanged(activity, windowLayoutInfo)
        Mockito.verify(consumer).accept(ArgumentMatchers.eq(windowLayoutInfo))
    }

    @Test
    public fun testWindowLayoutInfo_updatesOnSubsequentRegistration() {
        val interfaceCompat = SwitchOnUnregisterExtensionInterfaceCompat()
        val backend = ExtensionWindowBackend(interfaceCompat)
        val activity = Mockito.mock(
            Activity::class.java
        )
        val consumer = SimpleConsumer<WindowLayoutInfo>()
        val executor = MoreExecutors.directExecutor()
        val expected = mutableListOf<WindowLayoutInfo>()
        backend.registerLayoutChangeCallback(activity, executor, consumer)
        expected.add(interfaceCompat.currentWindowLayoutInfo())
        backend.unregisterLayoutChangeCallback(consumer)
        backend.registerLayoutChangeCallback(activity, executor, consumer)
        expected.add(interfaceCompat.currentWindowLayoutInfo())
        backend.unregisterLayoutChangeCallback(consumer)
        assertEquals(expected, consumer.mValues)
    }

    private interface WindowLayoutInfoConsumer : Consumer<WindowLayoutInfo>

    private class SimpleConsumer<T> : Consumer<T> {
        val mValues = mutableListOf<T>()

        override fun accept(t: T) {
            mValues.add(t)
        }

        fun allValues(): List<T> {
            return mValues
        }

        fun lastValue(): T {
            return mValues[mValues.size - 1]
        }
    }

    internal companion object {
        private fun newTestWindowLayoutInfo(): WindowLayoutInfo {
            var builder = WindowLayoutInfo.Builder()
            val windowLayoutInfo = builder.build()
            assertTrue(windowLayoutInfo.displayFeatures.isEmpty())
            val feature1: DisplayFeature = FoldingFeature(
                Rect(0, 2, 3, 4),
                FoldingFeature.TYPE_HINGE, FoldingFeature.STATE_FLAT
            )
            val feature2: DisplayFeature = FoldingFeature(
                Rect(0, 1, 5, 1),
                FoldingFeature.TYPE_HINGE, FoldingFeature.STATE_FLAT
            )
            val displayFeatures = listOf(feature1, feature2)
            builder = WindowLayoutInfo.Builder()
            builder.setDisplayFeatures(displayFeatures)
            return builder.build()
        }
    }
}
