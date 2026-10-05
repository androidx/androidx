/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.xr.runtime.openxr

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.interfaces.XrNativeInstanceProvider
import androidx.xr.runtime.interfaces.XrNativeInstanceProvider.Companion.INVALID_HANDLE

/** Implementation of native data provision for the OpenXR runtime. */
internal class OpenXrInstanceManager : XrNativeInstanceProvider {
    private val LIBRARY_NAME: String = "androidx.xr.runtime.openxr"

    override val requirements: Set<Feature> = setOf(Feature.FULLSTACK, Feature.OPEN_XR)

    internal var nativeManager: Long = INVALID_HANDLE

    override var xrInstanceProcAddr: Long = INVALID_HANDLE
        private set

    override var xrInstanceHandle: Long = INVALID_HANDLE
        private set

    override var xrSessionHandle: Long = INVALID_HANDLE
        private set

    override fun initialize(context: Context, extraExtensions: List<String>) {
        initialize(context, extraExtensions, isActivityBindingEnabled())
    }

    /**
     * Initializes the native OpenXR instance.
     *
     * @param bindActivity when true, [context] (expected to be an Activity) is handed to the native
     *   runtime as-is and bound to the OpenXR instance. This is a debug-only path for OpenXR
     *   runtimes that require an Activity, see [ACTIVITY_BINDING_PROPERTY].
     */
    @VisibleForTesting
    internal fun initialize(
        context: Context,
        extraExtensions: List<String>,
        bindActivity: Boolean,
    ) {
        if (bindActivity) {
            require(context is Activity) {
                "$ACTIVITY_BINDING_PROPERTY is set but the OpenXR instance is being created with " +
                    "${context.javaClass.name}; an Activity context is required."
            }
        }
        // OpenXR native handles live for the process lifetime. Using applicationContext ensures
        // that the OpenXR native runtime does not hold a reference to an Activity context, unless
        // the runtime requires the Activity binding.
        // TODO(b/571521407): temporary workaround, remove the Activity binding once the runtime
        // takes the Activity separately from the application context used for the instance.
        // TODO(b/537445115): record the Activity binding through the JXR event recording system.
        val nativeContext = if (bindActivity) context else context.applicationContext ?: context
        // Attempt to load the test library instead if it was added based on the Gradle AndroidTest
        // variant. Else this is a non-test environment.
        try {
            System.loadLibrary("${LIBRARY_NAME}.test")
        } catch (e: UnsatisfiedLinkError) {
            System.loadLibrary(LIBRARY_NAME)
        }
        nativeManager = nativeCreateOpenXrInstanceManager(extraExtensions.toTypedArray())
        if (bindActivity) {
            check(nativeSetActivityBindingEnabled(nativeManager, true)) {
                "The Activity binding must be requested before the OpenXR instance is created."
            }
        }

        xrInstanceHandle = nativeGetOpenXrInstanceHandle(nativeContext, nativeManager)
        xrInstanceProcAddr = nativeGetGetInstanceProcAddr(nativeManager)
        xrSessionHandle =
            try {
                nativeGetOpenXrSessionHandle(nativeContext, nativeManager)
            } catch (e: UnsatisfiedLinkError) {
                INVALID_HANDLE
            }
    }

    private external fun nativeCreateOpenXrInstanceManager(extensions: Array<String>): Long

    private external fun nativeSetActivityBindingEnabled(
        nativeManager: Long,
        enabled: Boolean,
    ): Boolean

    private external fun nativeGetOpenXrInstanceHandle(context: Context, nativeManager: Long): Long

    private external fun nativeGetGetInstanceProcAddr(nativeManager: Long): Long

    private external fun nativeGetOpenXrSessionHandle(context: Context, nativeManager: Long): Long

    internal companion object {
        /**
         * Debug system property that makes the OpenXR instance be bound to the context used to
         * create the session (expected to be an Activity) instead of the application context, e.g.
         * `adb shell setprop debug.androidx.xr.openxr.activity_binding 1`.
         *
         * Only meant for OpenXR runtimes that require an Activity: the runtime holds on to the
         * Activity for the lifetime of the OpenXR instance.
         */
        private const val ACTIVITY_BINDING_PROPERTY = "debug.androidx.xr.openxr.activity_binding"

        @Suppress("BanUncheckedReflection")
        private fun isActivityBindingEnabled(): Boolean =
            try {
                val systemPropertiesClass = Class.forName("android.os.SystemProperties")
                val getBooleanMethod =
                    systemPropertiesClass.getMethod(
                        "getBoolean",
                        String::class.java,
                        java.lang.Boolean.TYPE,
                    )
                getBooleanMethod.invoke(
                    /* obj= */ null,
                    ACTIVITY_BINDING_PROPERTY,
                    /* def= */ false,
                ) as? Boolean == true
            } catch (_: Exception) {
                false
            }
    }
}
