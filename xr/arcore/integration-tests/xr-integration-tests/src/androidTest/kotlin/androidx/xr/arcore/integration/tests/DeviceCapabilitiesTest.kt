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
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateTimedOut
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.XrDevice
import androidx.xr.testutils.XrDeviceConfig
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Automated integration tests for [XrDevice] hardware and capability inspection. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeviceCapabilitiesTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.HAND_TRACKING",
                "android.permission.EYE_TRACKING_COARSE",
                "android.permission.EYE_TRACKING_FINE",
                "android.permission.SCENE_UNDERSTANDING_FINE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
            )
        )

    private lateinit var context: Context
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private lateinit var session: Session
    private lateinit var xrDevice: XrDevice

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val launchedScenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario = launchedScenario
        session = createSession(launchedScenario)
        xrDevice = XrDevice.getCurrentDevice(session.context)
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }

    @Test
    @XrDeviceTest
    fun getCurrentDevice_returnsNonNullDevice() {
        val device = XrDevice.getCurrentDevice(context)

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
    fun getCurrentDevice_activeActivityContext_returnsCachedInstance() {
        val firstDevice = XrDevice.getCurrentDevice(session.context)

        val secondDevice = XrDevice.getCurrentDevice(session.context)

        assertThat(firstDevice).isSameInstanceAs(secondDevice)
    }

    @Test
    @XrDeviceTest
    fun getCurrentDevice_destroyedLifecycleContext_throwsIllegalStateException() {
        val activityContext = session.context
        checkNotNull(scenario).moveToState(Lifecycle.State.DESTROYED)

        assertFailsWith<IllegalStateException> { XrDevice.getCurrentDevice(activityContext) }
    }

    @Test
    @XrDeviceTest
    fun isProjectedServiceAvailable_onImmersiveXrDevice_returnsFalse() {
        val isAvailable = XrDevice.isProjectedServiceAvailable(context)

        assertThat(isAvailable).isFalse()
    }

    @Test
    @XrDeviceTest
    fun isHandTrackingModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isHandTrackingModeSupported(HandTrackingMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isHandTrackingModeSupported_both_matchesSessionConfigurationSupport() {
        val config = Config.Builder().setHandTracking(HandTrackingMode.BOTH).build()

        val isSupported = xrDevice.isHandTrackingModeSupported(HandTrackingMode.BOTH)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isEqualTo(isConfigurable)
    }

    @Test
    @XrDeviceTest
    fun isEyeTrackingModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isEyeTrackingModeSupported_fineTracking_matchesSessionConfigurationSupport() {
        val config = Config.Builder().setEyeTracking(EyeTrackingMode.FINE_TRACKING).build()

        val isSupported = xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isEqualTo(isConfigurable)
    }

    @Test
    @XrDeviceTest
    fun isEyeTrackingModeSupported_coarseTracking_matchesSessionConfigurationSupport() {
        val config = Config.Builder().setEyeTracking(EyeTrackingMode.COARSE_TRACKING).build()

        val isSupported = xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isEqualTo(isConfigurable)
    }

    @Test
    @XrDeviceTest
    fun isEyeTrackingModeSupported_whenFineTrackingSupported_supportsCoarseTracking() {
        assumeTrue(
            "Skipping test: Device does not support EyeTrackingMode.FINE_TRACKING",
            xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING),
        )

        val isCoarseSupported = xrDevice.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)

        assertThat(isCoarseSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_rawOnly_matchesSessionConfigurationSupport() {
        val config = Config.Builder().setDepthEstimation(DepthEstimationMode.RAW_ONLY).build()

        val isSupported = xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.RAW_ONLY)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isEqualTo(isConfigurable)
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_smoothOnly_matchesSessionConfigurationSupport() {
        val config = Config.Builder().setDepthEstimation(DepthEstimationMode.SMOOTH_ONLY).build()

        val isSupported = xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_ONLY)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isEqualTo(isConfigurable)
    }

    @Test
    @XrDeviceTest
    fun isDepthEstimationModeSupported_smoothAndRaw_returnsFalseOnImmersiveXrDevice() {
        val config = Config.Builder().setDepthEstimation(DepthEstimationMode.SMOOTH_AND_RAW).build()

        val isSupported =
            xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isFalse()
        assertThat(isConfigurable).isFalse()
    }

    @Test
    @XrDeviceTest
    fun isGeospatialModeSupported_disabled_returnsTrue() {
        val isSupported = xrDevice.isGeospatialModeSupported(GeospatialMode.DISABLED)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun isGeospatialModeSupported_spatial_matchesSessionConfigurationSupport() {
        val config = Config.Builder().setGeospatial(GeospatialMode.SPATIAL).build()

        val isSupported = xrDevice.isGeospatialModeSupported(GeospatialMode.SPATIAL)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isEqualTo(isConfigurable)
    }

    @Test
    @XrDeviceTest
    fun isGeospatialModeSupported_inertial_returnsFalseOnImmersiveXrDevice() {
        val config = Config.Builder().setGeospatial(GeospatialMode.INERTIAL).build()

        val isSupported = xrDevice.isGeospatialModeSupported(GeospatialMode.INERTIAL)
        val isConfigurable = isSessionConfigSupported(config)

        assertThat(isSupported).isFalse()
        assertThat(isConfigurable).isFalse()
    }

    @Test
    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    fun isRenderingModeSupported_mono_onPhysicalXrDevice_returnsFalse() {
        val isSupported = xrDevice.isRenderingModeSupported(RenderingMode.MONO)

        assertThat(isSupported).isFalse()
    }

    @Test
    @XrDeviceTest
    fun isRenderingModeSupported_stereo_returnsTrueAndExposesStereoViewpoints() {
        val isSupported = xrDevice.isRenderingModeSupported(RenderingMode.STEREO)
        RenderViewpoint.left(session)
        RenderViewpoint.right(session)

        assertThat(isSupported).isTrue()
    }

    @Test
    @XrDeviceTest
    fun getPreferredDisplayBlendMode_onImmersiveXrDevice_returnsActiveBlendMode() {
        val displayBlendMode = xrDevice.getPreferredDisplayBlendMode()

        assertThat(displayBlendMode)
            .isAnyOf(DisplayBlendMode.ADDITIVE, DisplayBlendMode.ALPHA_BLEND)
    }

    @Test
    @XrDeviceTest
    fun getLifecycle_afterMainIdle_returnsResumedLifecycle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val lifecycle = xrDevice.getLifecycle()

        assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    private fun isSessionConfigSupported(config: Config): Boolean =
        try {
            session.configure(config) is SessionConfigureSuccess
        } catch (_: UnsupportedOperationException) {
            false
        }

    private fun createSession(scenario: ActivityScenario<ComponentActivity>): Session {
        lateinit var activity: ComponentActivity
        scenario.onActivity { activity = it }
        val result = runBlocking(Dispatchers.Main) { Session.create(context = activity) }
        assumeTrue(
            "Skipping test: Missing or outdated APK '${(result as? SessionCreateApkRequired)?.requiredApk}'",
            result !is SessionCreateApkRequired,
        )
        assumeTrue(
            "Skipping test: Device does not support required XR features",
            result !is SessionCreateUnsupportedDevice,
        )
        check(result is SessionCreateSuccess) {
            when (result) {
                is SessionCreateUnknownError -> "SessionCreateUnknownError: ${result.errorMessage}"
                is SessionCreateTimedOut -> "SessionCreateTimedOut"
                else -> "Session.create failed with result: $result"
            }
        }
        return result.session
    }
}
