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
import androidx.core.util.Consumer
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/**
 * Unit tests for [ExtensionWindowBackend] that run on the JVM.
 */
public class ExtensionWindowBackendUnitTest {
    private lateinit var context: Context
    @Before
    public fun setUp() {
        context = mock(Context::class.java)
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
    public fun testRegisterLayoutChangeCallback() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = mock(
            ExtensionInterfaceCompat::class.java
        )

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = mock(
            Activity::class.java
        )
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size.toLong())
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity)

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer)
        assertTrue(backend.mWindowLayoutChangeCallbacks.isEmpty())
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(
            ArgumentMatchers.eq(activity)
        )
    }

    @Test
    public fun testRegisterLayoutChangeCallback_noExtension() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = null

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = mock(
            Activity::class.java
        )
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        verify(consumer).accept(ArgumentMatchers.any())
    }

    @Test
    public fun testRegisterLayoutChangeCallback_synchronousExtension() {
        val expectedInfo = newTestWindowLayoutInfo()
        val extensionInterfaceCompat: ExtensionInterfaceCompat =
            SynchronousExtensionInterface(expectedInfo)
        val backend = ExtensionWindowBackend(extensionInterfaceCompat)

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = mock(
            Activity::class.java
        )
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)

        // Check unregistering the layout change callback
        verify(consumer).accept(expectedInfo)
    }

    @Test
    public fun testRegisterLayoutChangeCallback_callsExtensionOnce() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = mock(
            ExtensionInterfaceCompat::class.java
        )

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = mock(
            Activity::class.java
        )
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        backend.registerLayoutChangeCallback(
            activity, { obj: Runnable -> obj.run() },
            mock(
                WindowLayoutInfoConsumer::class.java
            )
        )
        assertEquals(2, backend.mWindowLayoutChangeCallbacks.size.toLong())
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerAdded(activity)

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer)
        assertEquals(1, backend.mWindowLayoutChangeCallbacks.size.toLong())
        verify(backend.mWindowExtension, times(0))
            .onWindowLayoutChangeListenerRemoved(ArgumentMatchers.eq(activity))
    }

    @Test
    public fun testRegisterLayoutChangeCallback_clearListeners() {
        val backend = ExtensionWindowBackend.getInstance(context)
        backend.mWindowExtension = mock(
            ExtensionInterfaceCompat::class.java
        )

        // Check registering the layout change callback
        val firstConsumer: Consumer<WindowLayoutInfo> = mock(
            WindowLayoutInfoConsumer::class.java
        )
        val secondConsumer: Consumer<WindowLayoutInfo> = mock(
            WindowLayoutInfoConsumer::class.java
        )
        val activity = mock(
            Activity::class.java
        )
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
        verify(backend.mWindowExtension).onWindowLayoutChangeListenerRemoved(activity)
    }

    private interface WindowLayoutInfoConsumer : Consumer<WindowLayoutInfo>
    private class SynchronousExtensionInterface(windowLayoutInfo: WindowLayoutInfo) :
        ExtensionInterfaceCompat {
        private var mInterface: ExtensionCallbackInterface
        private val mWindowLayoutInfo: WindowLayoutInfo
        override fun validateExtensionInterface(): Boolean {
            return true
        }

        override fun setExtensionCallback(
            extensionCallback: ExtensionCallbackInterface
        ) {
            mInterface = extensionCallback
        }

        override fun onWindowLayoutChangeListenerAdded(activity: Activity) {
            mInterface.onWindowLayoutChanged(activity, mWindowLayoutInfo)
        }

        override fun onWindowLayoutChangeListenerRemoved(activity: Activity) {}

        init {
            mInterface = ExtensionCallbackInterface { _, _ -> }
            mWindowLayoutInfo = windowLayoutInfo
        }
    }

    private companion object {
        private fun newTestWindowLayoutInfo(): WindowLayoutInfo {
            val builder = WindowLayoutInfo.Builder()
            return builder.build()
        }
    }
}
