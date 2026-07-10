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

import androidx.activity.ComponentActivity
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.testing.internal.FakeSpatialApiVersionProvider
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProviderFactory
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class XrDeviceTestRuleTest {

    @Rule @JvmField val underTest: XrDeviceTestRule = XrDeviceTestRule()

    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
    }

    @Test
    fun before_registersSelfWithProviders() {
        assertThat(FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule).isEqualTo(underTest)
        assertThat(FakeSpatialApiVersionProvider.xrDeviceTestRule).isEqualTo(underTest)
    }

    @Test
    fun preferredDisplayBlendMode_returnedByDevice() {
        val device = XrDevice.getCurrentDevice(activity)
        underTest.preferredDisplayBlendMode = DisplayBlendMode.NO_DISPLAY

        assertThat(device.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.NO_DISPLAY)

        underTest.preferredDisplayBlendMode = DisplayBlendMode.ALPHA_BLEND

        assertThat(device.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.ALPHA_BLEND)
    }

    @Test
    fun supportedHandTrackingModes_controlledByTestRule() {
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.DISABLED)).isTrue()
        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.BOTH)).isFalse()

        underTest.supportedHandTrackingModes =
            setOf(HandTrackingMode.DISABLED, HandTrackingMode.BOTH)

        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.DISABLED)).isTrue()
        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.BOTH)).isTrue()
    }

    @Test
    fun supportedEyeTrackingModes_controlledByTestRule() {
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)).isTrue()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)).isFalse()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)).isFalse()

        underTest.supportedEyeTrackingModes =
            setOf(
                EyeTrackingMode.DISABLED,
                EyeTrackingMode.FINE_TRACKING,
                EyeTrackingMode.COARSE_TRACKING,
            )

        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)).isTrue()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)).isTrue()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)).isTrue()
    }

    @Test
    fun supportedDepthEstimationModes_controlledByTestRule() {
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.DISABLED)).isTrue()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.RAW_ONLY)).isFalse()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_ONLY)).isFalse()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW))
            .isFalse()

        underTest.supportedDepthEstimationModes =
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

    @Test
    fun supportedGeospatialModes_controlledByTestRule() {
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isGeospatialModeSupported(GeospatialMode.DISABLED)).isTrue()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.SPATIAL)).isFalse()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.INERTIAL)).isFalse()

        underTest.supportedGeospatialModes =
            setOf(GeospatialMode.DISABLED, GeospatialMode.SPATIAL, GeospatialMode.INERTIAL)

        assertThat(device.isGeospatialModeSupported(GeospatialMode.DISABLED)).isTrue()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.SPATIAL)).isTrue()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.INERTIAL)).isTrue()
    }

    @Test
    fun supportedRenderingModes_controlledByTestRule() {
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isRenderingModeSupported(RenderingMode.STEREO)).isFalse()
        assertThat(device.isRenderingModeSupported(RenderingMode.MONO)).isFalse()

        underTest.supportedRenderingModes = setOf(RenderingMode.STEREO, RenderingMode.MONO)

        assertThat(device.isRenderingModeSupported(RenderingMode.STEREO)).isTrue()
        assertThat(device.isRenderingModeSupported(RenderingMode.MONO)).isTrue()
    }

    @Test
    fun isProjectedServiceAvailable_enabledByDefault() {
        assertThat(XrDevice.isProjectedServiceAvailable(activity)).isTrue()
    }

    @Test
    fun isProjectedServiceAvailable_controlsReturnValue() {
        underTest.isProjectedServiceAvailable = false

        assertThat(XrDevice.isProjectedServiceAvailable(activity)).isFalse()

        underTest.isProjectedServiceAvailable = true

        assertThat(XrDevice.isProjectedServiceAvailable(activity)).isTrue()
    }

    @Test
    fun lifecycleState_stateInitializedByDefault() {
        val device = XrDevice.getCurrentDevice(activity)
        assertThat(device.getLifecycle().currentState).isEqualTo(Lifecycle.State.INITIALIZED)
    }

    @Test
    fun lifecycleState_controlsReturnValue() {
        val testLifecycleState = Lifecycle.State.STARTED
        val device = XrDevice.getCurrentDevice(activity)

        underTest.lifecycleState = testLifecycleState

        assertThat(device.getLifecycle().currentState).isEqualTo(testLifecycleState)
    }

    @Test
    fun preferredDisplayBlendMode_updatedWhenXrDeviceInitializedAfterTestRule() {
        check(underTest.preferredDisplayBlendMode == DisplayBlendMode.ALPHA_BLEND)

        underTest.preferredDisplayBlendMode = DisplayBlendMode.NO_DISPLAY
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.NO_DISPLAY)
    }

    @Test
    fun supportedHandTrackingModes_updatedWhenXrDeviceInitializedAfterTestRule() {
        check(underTest.supportedHandTrackingModes == setOf(HandTrackingMode.DISABLED))

        underTest.supportedHandTrackingModes =
            setOf(HandTrackingMode.DISABLED, HandTrackingMode.BOTH)
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.DISABLED)).isTrue()
        assertThat(device.isHandTrackingModeSupported(HandTrackingMode.BOTH)).isTrue()
    }

    @Test
    fun supportedEyeTrackingModes_updatedWhenXrDeviceInitializedAfterTestRule() {
        check(underTest.supportedEyeTrackingModes == setOf(EyeTrackingMode.DISABLED))

        underTest.supportedEyeTrackingModes =
            setOf(
                EyeTrackingMode.DISABLED,
                EyeTrackingMode.FINE_TRACKING,
                EyeTrackingMode.COARSE_TRACKING,
            )
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)).isTrue()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)).isTrue()
        assertThat(device.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)).isTrue()
    }

    @Test
    fun supportedDepthEstimationModes_updatedWhenXrDeviceInitializedAfterTestRule() {
        check(underTest.supportedDepthEstimationModes == setOf(DepthEstimationMode.DISABLED))

        underTest.supportedDepthEstimationModes =
            setOf(
                DepthEstimationMode.DISABLED,
                DepthEstimationMode.RAW_ONLY,
                DepthEstimationMode.SMOOTH_ONLY,
                DepthEstimationMode.SMOOTH_AND_RAW,
            )
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.DISABLED)).isTrue()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.RAW_ONLY)).isTrue()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_ONLY)).isTrue()
        assertThat(device.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW))
            .isTrue()
    }

    @Test
    fun supportedGeospatialModes_updatedWhenXrDeviceInitializedAfterTestRule() {
        check(underTest.supportedGeospatialModes == setOf(GeospatialMode.DISABLED))

        underTest.supportedGeospatialModes =
            setOf(GeospatialMode.DISABLED, GeospatialMode.SPATIAL, GeospatialMode.INERTIAL)
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isGeospatialModeSupported(GeospatialMode.DISABLED)).isTrue()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.SPATIAL)).isTrue()
        assertThat(device.isGeospatialModeSupported(GeospatialMode.INERTIAL)).isTrue()
    }

    @Test
    fun supportedRenderingModes_updatedWhenXrDeviceInitializedAfterTestRule() {
        check(underTest.supportedRenderingModes == emptySet<RenderingMode>())

        underTest.supportedRenderingModes = setOf(RenderingMode.STEREO, RenderingMode.MONO)
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.isRenderingModeSupported(RenderingMode.STEREO)).isTrue()
        assertThat(device.isRenderingModeSupported(RenderingMode.MONO)).isTrue()
    }

    @Test
    fun lifecycleState_updatedWhenXrDeviceInitializedAfterTestRule() {
        val testLifecycleState = Lifecycle.State.STARTED
        check(underTest.lifecycleState == Lifecycle.State.INITIALIZED)

        underTest.lifecycleState = testLifecycleState
        val device = XrDevice.getCurrentDevice(activity)

        assertThat(device.getLifecycle().currentState).isEqualTo(testLifecycleState)
    }
}
