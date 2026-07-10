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

package androidx.xr.projected

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.xr.runtime.interfaces.DepthEstimationMode
import androidx.xr.runtime.interfaces.DisplayBlendMode
import androidx.xr.runtime.interfaces.EyeTrackingMode
import androidx.xr.runtime.interfaces.GeospatialMode
import androidx.xr.runtime.interfaces.HandTrackingMode
import androidx.xr.runtime.interfaces.RenderingMode
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProvider
import kotlin.coroutines.CoroutineContext

internal class ProjectedDeviceCapabilityProvider(
    override val context: Context,
    private val coroutineContext: CoroutineContext,
) : XrDeviceCapabilityProvider {
    override val lifecycle: Lifecycle
        get() = ProjectedDeviceLifecycle(provider = this, context, coroutineContext)

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        // TODO(b/461561664): Implement this function dynamically.
        return DisplayBlendMode.ADDITIVE
    }

    override fun isHandTrackingModeSupported(mode: HandTrackingMode): Boolean {
        return mode == HandTrackingMode.DISABLED
    }

    override fun isEyeTrackingModeSupported(mode: EyeTrackingMode): Boolean {
        return mode == EyeTrackingMode.DISABLED
    }

    override fun isGeospatialModeSupported(mode: GeospatialMode): Boolean {
        return true
    }

    override fun isDepthEstimationModeSupported(mode: DepthEstimationMode): Boolean {
        return mode == DepthEstimationMode.DISABLED
    }

    override fun isRenderingModeSupported(mode: RenderingMode): Boolean {
        // TODO(b/500757202): Determine rendering support dynamically based on device capability.
        return false
    }
}
