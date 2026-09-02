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

package androidx.xr.arcore.integration.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.XrDevice
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Automated integration tests for [XrDevice] hardware and capability inspection. */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class DeviceCapabilitiesTest {

    private lateinit var context: Context
    private lateinit var xrDevice: XrDevice

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        xrDevice = XrDevice.getCurrentDevice(context)
    }

    @Test
    @XrDeviceTest
    fun getCurrentDevice_returnsNonNullDevice() {
        val appContext = context

        val device = XrDevice.getCurrentDevice(appContext)

        assertThat(device).isNotNull()
    }

    @Test
    @XrDeviceTest
    fun getCurrentDevice_sameContext_returnsCachedInstance() {
        val firstDevice = XrDevice.getCurrentDevice(context)

        val secondDevice = XrDevice.getCurrentDevice(context)

        assertThat(firstDevice).isSameInstanceAs(secondDevice)
    }

    @Test
    @XrDeviceTest
    fun isHandTrackingModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isHandTrackingModeSupported(HandTrackingMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isHandTrackingModeSupported_both_returnsBoolean() {
        val isSupported = xrDevice.isHandTrackingModeSupported(HandTrackingMode.BOTH)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isEyeTrackingModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isEyeTrackingModeSupported_fineTracking_returnsBoolean() {
        val isSupported = xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isEyeTrackingModeSupported_coarseTracking_returnsBoolean() {
        val isSupported = xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_rawOnly_returnsBoolean() {
        val isSupported = xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.RAW_ONLY)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_smoothOnly_returnsBoolean() {
        val isSupported = xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_ONLY)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_smoothAndRaw_returnsBoolean() {
        val isSupported =
            xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isGeospatialModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isGeospatialModeSupported(GeospatialMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isGeospatialModeSupported_spatial_returnsBoolean() {
        val isSupported = xrDevice.isGeospatialModeSupported(GeospatialMode.SPATIAL)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isRenderingModeSupported_mono_returnsBoolean() {
        val isSupported = xrDevice.isRenderingModeSupported(RenderingMode.MONO)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun isRenderingModeSupported_stereo_returnsBoolean() {
        val isSupported = xrDevice.isRenderingModeSupported(RenderingMode.STEREO)

        assertThat(isSupported).isAnyOf(true, false)
    }

    @Test
    @XrDeviceTest
    fun getPreferredDisplayBlendMode_returnsValidBlendMode() {
        val displayBlendMode = xrDevice.getPreferredDisplayBlendMode()

        assertThat(displayBlendMode)
            .isAnyOf(
                DisplayBlendMode.NO_DISPLAY,
                DisplayBlendMode.ADDITIVE,
                DisplayBlendMode.ALPHA_BLEND,
            )
    }

    @Test
    @XrDeviceTest
    fun getLifecycle_returnsNonNullLifecycle() {
        val lifecycle = xrDevice.getLifecycle()

        assertThat(lifecycle).isNotNull()
    }
}
