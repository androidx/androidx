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

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.xr.runtime.interfaces.DepthEstimationMode
import androidx.xr.runtime.interfaces.DisplayBlendMode
import androidx.xr.runtime.interfaces.EyeTrackingMode
import androidx.xr.runtime.interfaces.GeospatialMode
import androidx.xr.runtime.interfaces.HandTrackingMode
import androidx.xr.runtime.interfaces.RenderingMode
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class OpenXrDeviceCapabilityProvider(override val context: Context) :
    XrDeviceCapabilityProvider {

    // See b/496257589: Use a stub class to avoid dependency on lifecycle-process.
    override val lifecycle: Lifecycle = StubProcessLifecycleOwner.lifecycle

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        return nativeGetPreferredBlendMode(OpenXrInstanceManager.nativePointer)
            ?: throw IllegalStateException("Failed to get preferred blend mode.")
    }

    override fun isHandTrackingModeSupported(mode: HandTrackingMode): Boolean {
        return if (mode == HandTrackingMode.DISABLED) {
            true
        } else {
            nativeIsHandTrackingSupported(OpenXrInstanceManager.nativePointer)
        }
    }

    override fun isEyeTrackingModeSupported(mode: EyeTrackingMode): Boolean {
        return if (mode == EyeTrackingMode.DISABLED) {
            true
        } else {
            nativeIsEyeTrackingSupported(OpenXrInstanceManager.nativePointer)
        }
    }

    override fun isGeospatialModeSupported(mode: GeospatialMode): Boolean {
        return if (mode == GeospatialMode.DISABLED) {
            true
        } else {
            nativeIsGeospatialSupported(OpenXrInstanceManager.nativePointer)
        }
    }

    override fun isDepthEstimationModeSupported(mode: DepthEstimationMode): Boolean {
        return if (mode == DepthEstimationMode.DISABLED) {
            true
        } else {
            nativeIsDepthTrackingSupported(OpenXrInstanceManager.nativePointer)
        }
    }

    override fun isRenderingModeSupported(mode: RenderingMode): Boolean {
        return nativeIsRenderingModeSupported(OpenXrInstanceManager.nativePointer, mode.value)
    }

    private external fun nativeGetPreferredBlendMode(nativePointer: Long): DisplayBlendMode?

    private external fun nativeIsHandTrackingSupported(nativePointer: Long): Boolean

    private external fun nativeIsEyeTrackingSupported(nativePointer: Long): Boolean

    private external fun nativeIsGeospatialSupported(nativePointer: Long): Boolean

    private external fun nativeIsDepthTrackingSupported(nativePointer: Long): Boolean

    private external fun nativeIsRenderingModeSupported(nativePointer: Long, mode: Int): Boolean
}

/**
 * A singleton that provides a lifecycle for the application process.
 *
 * Note: The lifecycle provided by this object does not observe the application going into the
 * background and will remain in [Lifecycle.State.RESUMED] after the process has been initialized,
 * until it is destroyed. This is different from
 * [ProcessLifecycleOwner](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner)
 * which will dispatch [Lifecycle.Event.ON_STOP] if the application is backgrounded.
 */
private object StubProcessLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    init {
        CoroutineScope(Dispatchers.Main).launch { registry.currentState = Lifecycle.State.RESUMED }
    }

    override val lifecycle: Lifecycle = registry
}
