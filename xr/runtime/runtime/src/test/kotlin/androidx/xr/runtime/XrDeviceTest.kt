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

package androidx.xr.runtime

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.testing.XrDeviceTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
@Suppress("deprecation")
class XrDeviceTest {
    @Rule @JvmField val xrDeviceTestRule = XrDeviceTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val shadowApplication = shadowOf(activity.application)

        StubPerceptionRuntime.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun lifecycle_returnsLifecycleFromSession() {
        val session = createSession()
        val xrDevice = XrDevice.getCurrentDevice(session)

        assertThat(xrDevice.getLifecycle()).isEqualTo((session.lifecycleOwner.lifecycle))
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun getCurrentDevice_returnsCachedDevice() {
        val device1 = XrDevice.getCurrentDevice(activity)
        val device2 = XrDevice.getCurrentDevice(activity)

        assertThat(device1).isSameInstanceAs(device2)
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun getCurrentDevice_returnsDifferentDeviceForDifferentContext() {
        val device1 = XrDevice.getCurrentDevice(activity)
        val activityController2 = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity2 = activityController2.get()
        val device2 = XrDevice.getCurrentDevice(activity2)

        assertThat(device1).isNotSameInstanceAs(device2)
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isHandTrackingModeSupported_returnsFalseWhenInternalModeNotSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedHandTrackingModes = emptySet()

        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.DISABLED)).isFalse()
        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.BOTH)).isFalse()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isHandTrackingModeSupported_returnsTrueWhenInternalModeSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedHandTrackingModes =
            setOf(HandTrackingMode.DISABLED, HandTrackingMode.BOTH)

        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.DISABLED)).isTrue()
        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.BOTH)).isTrue()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isEyeTrackingModeSupported_returnsFalseWhenInternalModeNotSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedEyeTrackingModes = emptySet()

        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)).isFalse()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)).isFalse()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)).isFalse()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isEyeTrackingModeSupported_returnsTrueWhenInternalModeSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedEyeTrackingModes =
            setOf(
                EyeTrackingMode.DISABLED,
                EyeTrackingMode.FINE_TRACKING,
                EyeTrackingMode.COARSE_TRACKING,
            )

        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)).isTrue()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)).isTrue()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)).isTrue()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isDepthEstimationModeSupported_returnsFalseWhenInternalModeNotSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedDepthEstimationModes = emptySet()

        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.DISABLED)).isFalse()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.RAW_ONLY)).isFalse()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_ONLY)).isFalse()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW))
            .isFalse()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isDepthEstimationModeSupported_returnsTrueWhenInternalModeSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedDepthEstimationModes =
            setOf(
                DepthEstimationMode.DISABLED,
                DepthEstimationMode.RAW_ONLY,
                DepthEstimationMode.SMOOTH_ONLY,
                DepthEstimationMode.SMOOTH_AND_RAW,
            )

        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.DISABLED)).isTrue()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.RAW_ONLY)).isTrue()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_ONLY)).isTrue()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW))
            .isTrue()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isGeospatialModeSupported_returnsFalseWhenInternalModeNotSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedGeospatialModes = emptySet()

        assertThat(device.isGeospatialModeSupported(GeospatialMode.DISABLED)).isFalse()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.VPS_AND_GPS)).isFalse()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isGeospatialModeSupported_returnsTrueWhenInternalModeSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedGeospatialModes =
            setOf(GeospatialMode.DISABLED, GeospatialMode.VPS_AND_GPS)

        assertThat(device.isGeospatialModeSupported(GeospatialMode.DISABLED)).isTrue()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.VPS_AND_GPS)).isTrue()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isRenderingModeSupported_returnsFalseWhenInternalModeNotSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedRenderingModes = emptySet()

        assertThat(device.isRenderingModeSupported(RenderingMode.MONO)).isFalse()
        assertThat(device.isRenderingModeSupported(RenderingMode.STEREO)).isFalse()
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun isRenderingModeSupported_returnsTrueWhenInternalModeSupported() {
        val device = XrDevice.getCurrentDevice(activity)
        xrDeviceTestRule.supportedRenderingModes = setOf(RenderingMode.MONO, RenderingMode.STEREO)

        assertThat(device.isRenderingModeSupported(RenderingMode.MONO)).isTrue()
        assertThat(device.isRenderingModeSupported(RenderingMode.STEREO)).isTrue()
    }

    private fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher): Session {
        val result = Session.create(activity, coroutineDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        return (result as SessionCreateSuccess).session
    }
}
