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
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.adapter.sidecar.ExtensionInterfaceCompat.ExtensionCallbackInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/** Unit tests for [SidecarWindowBackend] that run on the JVM. */
public class SidecarWindowBackendUnitTest {
    private lateinit var context: Context

    @Before
    public fun setUp() {
        context = mock()
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
    public fun testRegisterLayoutChangeCallback() {
        val backend = SidecarWindowBackend.getInstance(context)
        backend.windowExtension = mock()

        // Check registering the layout change callback
        val consumer = mock<Consumer<WindowLayoutInfo>>()
        val activity = mock<Activity>()
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        assertEquals(1, backend.windowLayoutChangeCallbacks.size.toLong())
        verify(backend.windowExtension!!).onWindowLayoutChangeListenerAdded(activity)

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer)
        assertTrue(backend.windowLayoutChangeCallbacks.isEmpty())
        verify(backend.windowExtension!!).onWindowLayoutChangeListenerRemoved(eq(activity))
    }

    @Test
    public fun testRegisterLayoutChangeCallback_withContext() {
        val backend = SidecarWindowBackend.getInstance(context)
        backend.windowExtension = mock()

        // Check registering the layout change callback
        val consumer = mock<Consumer<WindowLayoutInfo>>()
        val context = mock<Context>()
        backend.registerLayoutChangeCallback(context, { obj: Runnable -> obj.run() }, consumer)
        verify(consumer).accept(any())
    }

    @Test
    public fun testRegisterLayoutChangeCallback_noExtension() {
        val backend = SidecarWindowBackend.getInstance(context)
        backend.windowExtension = null

        // Check registering the layout change callback
        val consumer = mock<Consumer<WindowLayoutInfo>>()
        val activity = mock<Activity>()
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        verify(consumer).accept(any())
    }

    @Test
    public fun testRegisterLayoutChangeCallback_synchronousExtension() {
        val expectedInfo = newTestWindowLayoutInfo()
        val extensionInterfaceCompat: ExtensionInterfaceCompat =
            SynchronousExtensionInterface(expectedInfo)
        val backend = SidecarWindowBackend(extensionInterfaceCompat)

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = mock()
        val activity = mock<Activity>()
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)

        // Check unregistering the layout change callback
        verify(consumer).accept(expectedInfo)
    }

    @Test
    public fun testRegisterLayoutChangeCallback_callsExtensionOnce() {
        val backend = SidecarWindowBackend.getInstance(context)
        backend.windowExtension = mock()

        // Check registering the layout change callback
        val consumer: Consumer<WindowLayoutInfo> = mock()
        val activity = mock<Activity>()
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, consumer)
        backend.registerLayoutChangeCallback(activity, { obj: Runnable -> obj.run() }, mock())
        assertEquals(2, backend.windowLayoutChangeCallbacks.size.toLong())
        verify(backend.windowExtension!!).onWindowLayoutChangeListenerAdded(activity)

        // Check unregistering the layout change callback
        backend.unregisterLayoutChangeCallback(consumer)
        assertEquals(1, backend.windowLayoutChangeCallbacks.size.toLong())
        verify(backend.windowExtension!!, times(0))
            .onWindowLayoutChangeListenerRemoved(eq(activity))
    }

    @Test
    public fun testRegisterLayoutChangeCallback_clearListeners() {
        val backend = SidecarWindowBackend.getInstance(context)
        backend.windowExtension = mock()

        // Check registering the layout change callback
        val firstConsumer = mock<Consumer<WindowLayoutInfo>>()
        val secondConsumer = mock<Consumer<WindowLayoutInfo>>()
        val activity = mock<Activity>()
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

    private class SynchronousExtensionInterface(windowLayoutInfo: WindowLayoutInfo) :
        ExtensionInterfaceCompat {
        private var mInterface: ExtensionCallbackInterface
        private val mWindowLayoutInfo: WindowLayoutInfo

        override fun validateExtensionInterface(): Boolean {
            return true
        }

        override fun setExtensionCallback(extensionCallback: ExtensionCallbackInterface) {
            mInterface = extensionCallback
        }

        override fun onWindowLayoutChangeListenerAdded(activity: Activity) {
            mInterface.onWindowLayoutChanged(activity, mWindowLayoutInfo)
        }

        override fun onWindowLayoutChangeListenerRemoved(activity: Activity) {}

        init {
            mInterface =
                object : ExtensionCallbackInterface {
                    override fun onWindowLayoutChanged(
                        activity: Activity,
                        newLayout: WindowLayoutInfo
                    ) {}
                }
            mWindowLayoutInfo = windowLayoutInfo
        }
    }

    private companion object {
        private fun newTestWindowLayoutInfo(): WindowLayoutInfo {
            return WindowLayoutInfo(emptyList())
        }
    }
}
