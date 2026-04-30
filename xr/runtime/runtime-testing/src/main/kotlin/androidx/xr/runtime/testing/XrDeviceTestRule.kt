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

package androidx.xr.runtime.testing

import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.SpatialApiVersion
import androidx.xr.runtime.SpatialApiVersionHelper
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.testing.internal.FakeSpatialApiVersionProvider
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProvider
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProviderFactory
import androidx.xr.runtime.toInternalDepthEstimationMode
import androidx.xr.runtime.toInternalEyeTrackingMode
import androidx.xr.runtime.toInternalGeospatialMode
import androidx.xr.runtime.toInternalHandTrackingMode
import androidx.xr.runtime.toInternalRenderingMode
import org.junit.rules.ExternalResource

/** JUnit Rule containing properties that affect the results of [XrDevice] capability APIs. */
public class XrDeviceTestRule : ExternalResource() {
    internal var capabilityProvider: FakeXrDeviceCapabilityProvider? = null
    internal var spatialApiVersionProvider: FakeSpatialApiVersionProvider? = null

    /**
     * The result of [XrDevice.getPreferredDisplayBlendMode].
     *
     * Tests can set this property to control the value returned by
     * [XrDevice.getPreferredDisplayBlendMode] during the test execution.
     */
    public var preferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.ALPHA_BLEND
        set(value) {
            field = value
            capabilityProvider?.preferredDisplayBlendMode = value.toInternal()
        }

    /**
     * The set of [HandTrackingMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isHandTrackingModeSupported] during the test execution.
     */
    public var supportedHandTrackingModes: Set<HandTrackingMode> = setOf(HandTrackingMode.DISABLED)
        set(value) {
            field = value
            capabilityProvider?.let { capabilityProvider ->
                capabilityProvider.supportedHandTrackingModes.clear()
                value.forEach { mode ->
                    capabilityProvider.supportedHandTrackingModes.add(
                        mode.toInternalHandTrackingMode()
                    )
                }
            }
        }

    /**
     * The set of [EyeTrackingMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isEyeTrackingModeSupported] during the test execution.
     */
    public var supportedEyeTrackingModes: Set<EyeTrackingMode> = setOf(EyeTrackingMode.DISABLED)
        set(value) {
            field = value
            capabilityProvider?.let { capabilityProvider ->
                capabilityProvider.supportedEyeTrackingModes.clear()
                value.forEach { mode ->
                    capabilityProvider.supportedEyeTrackingModes.add(
                        mode.toInternalEyeTrackingMode()
                    )
                }
            }
        }

    /**
     * The set of [DepthEstimationMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isDepthEstimationModeSupported] during the test execution.
     */
    public var supportedDepthEstimationModes: Set<DepthEstimationMode> =
        setOf(DepthEstimationMode.DISABLED)
        set(value) {
            field = value
            capabilityProvider?.let { capabilityProvider ->
                capabilityProvider.supportedDepthEstimationModes.clear()
                value.forEach { mode ->
                    capabilityProvider.supportedDepthEstimationModes.add(
                        mode.toInternalDepthEstimationMode()
                    )
                }
            }
        }

    /**
     * The set of [GeospatialMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isGeospatialModeSupported] during the test execution.
     */
    public var supportedGeospatialModes: Set<GeospatialMode> = setOf(GeospatialMode.DISABLED)
        set(value) {
            field = value
            capabilityProvider?.let { capabilityProvider ->
                capabilityProvider.supportedGeospatialModes.clear()
                value.forEach { mode ->
                    capabilityProvider.supportedGeospatialModes.add(mode.toInternalGeospatialMode())
                }
            }
        }

    /**
     * The set of [RenderingMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isRenderingModeSupported] during the test execution.
     */
    public var supportedRenderingModes: Set<RenderingMode> = setOf()
        set(value) {
            field = value
            capabilityProvider?.let { capabilityProvider ->
                capabilityProvider.supportedRenderingModes.clear()
                value.forEach { mode ->
                    capabilityProvider.supportedRenderingModes.add(mode.toInternalRenderingMode())
                }
            }
        }

    /**
     * The value to be returned by [SpatialApiVersionHelper.spatialApiVersion].
     *
     * Tests can set this property to control the value returned by
     * [SpatialApiVersionHelper.spatialApiVersion] during the test execution. By default the value
     * is set to the latest stable API level.
     */
    @SpatialApiVersion
    public var spatialApiVersion: Int = SpatialApiVersions.UNKNOWN
        set(@SpatialApiVersion value) {
            spatialApiVersionProvider?.spatialApiVersion = value
            field = value
        }

    internal fun DisplayBlendMode.toInternal() =
        when (this) {
            DisplayBlendMode.ALPHA_BLEND ->
                androidx.xr.runtime.interfaces.DisplayBlendMode.ALPHA_BLEND
            DisplayBlendMode.ADDITIVE -> androidx.xr.runtime.interfaces.DisplayBlendMode.ADDITIVE
            else -> androidx.xr.runtime.interfaces.DisplayBlendMode.NO_DISPLAY
        }

    init {
        // Force SpatialApiVersionHelper to load FakeSpatialApiVersionProvider
        SpatialApiVersionHelper.spatialApiVersion
    }

    override fun before() {
        FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule = this

        FakeSpatialApiVersionProvider.xrDeviceTestRule = this
        FakeSpatialApiVersionProvider.instance?.registerProvider()
        spatialApiVersion =
            spatialApiVersionProvider?.spatialApiVersion ?: SpatialApiVersions.UNKNOWN
    }

    override fun after() {
        FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule = null
        FakeSpatialApiVersionProvider.xrDeviceTestRule = null
    }
}
