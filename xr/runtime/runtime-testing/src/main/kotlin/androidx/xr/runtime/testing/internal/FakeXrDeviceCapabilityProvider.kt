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

package androidx.xr.runtime.testing.internal

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.xr.runtime.interfaces.DepthEstimationMode as InternalDepthEstimationMode
import androidx.xr.runtime.interfaces.DisplayBlendMode
import androidx.xr.runtime.interfaces.EyeTrackingMode as InternalEyeTrackingMode
import androidx.xr.runtime.interfaces.GeospatialMode as InternalGeospatialMode
import androidx.xr.runtime.interfaces.HandTrackingMode as InternalHandTrackingMode
import androidx.xr.runtime.interfaces.RenderingMode as InternalRenderingMode
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProvider

internal class FakeXrDeviceCapabilityProvider(override val context: Context) :
    XrDeviceCapabilityProvider {

    private val testLifecycleOwner: TestLifecycleOwner = TestLifecycleOwner()

    internal var preferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.ALPHA_BLEND

    internal var supportedHandTrackingModes: MutableSet<InternalHandTrackingMode> =
        mutableSetOf(InternalHandTrackingMode.DISABLED)

    internal var supportedEyeTrackingModes: MutableSet<InternalEyeTrackingMode> =
        mutableSetOf(InternalEyeTrackingMode.DISABLED)

    internal var supportedDepthEstimationModes: MutableSet<InternalDepthEstimationMode> =
        mutableSetOf(InternalDepthEstimationMode.DISABLED)

    internal var supportedGeospatialModes: MutableSet<InternalGeospatialMode> =
        mutableSetOf(InternalGeospatialMode.DISABLED)

    internal var supportedRenderingModes: MutableSet<InternalRenderingMode> = mutableSetOf()

    override val lifecycle: Lifecycle
        get() = testLifecycleOwner.lifecycle

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode = preferredDisplayBlendMode

    override fun isHandTrackingModeSupported(mode: InternalHandTrackingMode): Boolean {
        return supportedHandTrackingModes.contains(mode)
    }

    override fun isEyeTrackingModeSupported(mode: InternalEyeTrackingMode): Boolean {
        return supportedEyeTrackingModes.contains(mode)
    }

    override fun isGeospatialModeSupported(mode: InternalGeospatialMode): Boolean {
        return supportedGeospatialModes.contains(mode)
    }

    override fun isDepthEstimationModeSupported(mode: InternalDepthEstimationMode): Boolean {
        return supportedDepthEstimationModes.contains(mode)
    }

    override fun isRenderingModeSupported(mode: InternalRenderingMode): Boolean {
        return supportedRenderingModes.contains(mode)
    }
}
