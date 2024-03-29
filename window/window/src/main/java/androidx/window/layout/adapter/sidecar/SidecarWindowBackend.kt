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
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.UiContext
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer
import androidx.window.core.Version
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.adapter.WindowBackend
import androidx.window.layout.adapter.sidecar.ExtensionInterfaceCompat.ExtensionCallbackInterface
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Default implementation of [WindowBackend] that uses a combination of platform APIs and
 * device-dependent OEM extensions.
 */
internal class SidecarWindowBackend @VisibleForTesting constructor(
    @field:VisibleForTesting @field:GuardedBy(
        "globalLock"
    ) var windowExtension: ExtensionInterfaceCompat?
) : WindowBackend {

    /**
     * List of all registered callbacks for window layout info. Not protected by [globalLock] to
     * allow iterating and callback execution without holding the global lock.
     */
    @VisibleForTesting
    val windowLayoutChangeCallbacks: CopyOnWriteArrayList<WindowLayoutChangeCallbackWrapper>

    init {
        windowLayoutChangeCallbacks = CopyOnWriteArrayList<WindowLayoutChangeCallbackWrapper>()
        windowExtension?.setExtensionCallback(ExtensionListenerImpl())
    }

    override fun registerLayoutChangeCallback(
        @UiContext context: Context,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {
        val activity = context as? Activity
        activity?.let {
            globalLock.withLock {
                val windowExtension = windowExtension
                if (windowExtension == null) {
                    if (DEBUG) {
                        Log.v(TAG, "Extension not loaded, skipping callback registration.")
                    }
                    callback.accept(WindowLayoutInfo(emptyList()))
                    return
                }

                // Check if the activity was already registered, in case we need to report tracking
                // of a new activity to the extension.
                val isActivityRegistered = isActivityRegistered(activity)
                val callbackWrapper =
                    WindowLayoutChangeCallbackWrapper(activity, executor, callback)
                windowLayoutChangeCallbacks.add(callbackWrapper)
                if (!isActivityRegistered) {
                    windowExtension.onWindowLayoutChangeListenerAdded(activity)
                } else {
                    // Latest info for the previously registered callback for activity
                    // and send it to the new activity
                    val lastInfo = windowLayoutChangeCallbacks.firstOrNull {
                        activity == it.activity
                    }?.lastInfo
                    if (lastInfo != null) {
                        callbackWrapper.accept(lastInfo)
                    }
                }
            }
        } ?: run {
            callback.accept(WindowLayoutInfo(emptyList()))
        }
    }

    private fun isActivityRegistered(activity: Activity): Boolean {
        return windowLayoutChangeCallbacks.any { callbackWrapper ->
            callbackWrapper.activity == activity
        }
    }

    override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
        synchronized(globalLock) {
            if (windowExtension == null) {
                if (DEBUG) {
                    Log.v(TAG, "Extension not loaded, skipping callback un-registration.")
                }
                return
            }

            // The same callback may be registered for multiple different window tokens, and
            // vice-versa. First collect all items to be removed.
            val itemsToRemove: MutableList<WindowLayoutChangeCallbackWrapper> = ArrayList()
            for (callbackWrapper in windowLayoutChangeCallbacks) {
                val registeredCallback = callbackWrapper.callback
                if (registeredCallback === callback) {
                    itemsToRemove.add(callbackWrapper)
                }
            }
            // Remove the items from the list and notify extension if needed.
            windowLayoutChangeCallbacks.removeAll(itemsToRemove)
            for (callbackWrapper in itemsToRemove) {
                callbackRemovedForActivity(callbackWrapper.activity)
            }
        }
    }

    /**
     * Checks if there are no more registered callbacks left for the activity and inform
     * extension if needed.
     */
    @GuardedBy("globalLock")
    private fun callbackRemovedForActivity(activity: Activity) {
        val hasRegisteredCallback = windowLayoutChangeCallbacks.any { wrapper ->
            wrapper.activity == activity
        }
        if (hasRegisteredCallback) {
            return
        }
        // No registered callbacks left for the activity - report to extension.
        windowExtension?.onWindowLayoutChangeListenerRemoved(activity)
    }

    @VisibleForTesting
    internal inner class ExtensionListenerImpl : ExtensionCallbackInterface {
        override fun onWindowLayoutChanged(
            activity: Activity,
            newLayout: WindowLayoutInfo
        ) {
            for (callbackWrapper in windowLayoutChangeCallbacks) {
                if (callbackWrapper.activity != activity) {
                    continue
                }
                callbackWrapper.accept(newLayout)
            }
        }
    }

    /**
     * Wrapper around [Consumer<WindowLayoutInfo>] that also includes the [Executor]
     * on which the callback should run and the [Activity].
     */
    internal class WindowLayoutChangeCallbackWrapper(
        val activity: Activity,
        private val executor: Executor,
        val callback: Consumer<WindowLayoutInfo>
    ) {
        var lastInfo: WindowLayoutInfo? = null
        fun accept(newLayoutInfo: WindowLayoutInfo) {
            lastInfo = newLayoutInfo
            executor.execute { callback.accept(newLayoutInfo) }
        }
    }

    companion object {
        const val DEBUG = false

        @Volatile
        private var globalInstance: SidecarWindowBackend? = null
        private val globalLock = ReentrantLock()
        private const val TAG = "WindowServer"

        /**
         * Gets the shared instance of the class.
         */
        fun getInstance(context: Context): SidecarWindowBackend {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        val windowExtension = initAndVerifyExtension(context)
                        globalInstance = SidecarWindowBackend(windowExtension)
                    }
                }
            }
            return globalInstance!!
        }

        /**
         * Loads an instance of [androidx.window.sidecar.SidecarInterface] implemented by OEM
         * if available on this device. This also verifies if the loaded implementation conforms
         * to the declared API version.
         */
        fun initAndVerifyExtension(context: Context): ExtensionInterfaceCompat? {
            var impl: ExtensionInterfaceCompat? = null
            try {
                if (isSidecarVersionSupported(SidecarCompat.sidecarVersion)) {
                    impl = SidecarCompat(context)
                    if (!impl.validateExtensionInterface()) {
                        if (DEBUG) {
                            Log.d(TAG, "Loaded Sidecar doesn't match the interface version")
                        }
                        impl = null
                    }
                }
            } catch (t: Throwable) {
                if (DEBUG) {
                    Log.d(TAG, "Failed to load sidecar: $t")
                }
                impl = null
            }
            if (impl == null) {
                if (DEBUG) {
                    Log.d(TAG, "No supported extension or sidecar found")
                }
            }
            return impl
        }

        /**
         * Checks if the Sidecar version provided on this device is supported by the current
         * version of the library.
         * @param sidecarVersion the [Version] of Sidecar that is provided on the device.
         */
        @VisibleForTesting
        fun isSidecarVersionSupported(sidecarVersion: Version?): Boolean {
            if (sidecarVersion == null) {
                return false
            }
            return sidecarVersion >= Version.VERSION_0_1
        }

        /**
         * Test-only affordance to forget the existing instance.
         */
        @VisibleForTesting
        fun resetInstance() {
            globalInstance = null
        }
    }
}
