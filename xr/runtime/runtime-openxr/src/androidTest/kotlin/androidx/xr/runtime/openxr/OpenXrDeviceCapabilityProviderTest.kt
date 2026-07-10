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
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.interfaces.DepthEstimationMode
import androidx.xr.runtime.interfaces.DisplayBlendMode
import androidx.xr.runtime.interfaces.EyeTrackingMode
import androidx.xr.runtime.interfaces.GeospatialMode
import androidx.xr.runtime.interfaces.HandTrackingMode
import androidx.xr.runtime.interfaces.RenderingMode
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalCoroutinesApi::class)
class OpenXrDeviceCapabilityProviderTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    private lateinit var context: Context

    private lateinit var underTest: OpenXrDeviceCapabilityProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val instanceManager = OpenXrInstanceManager()
        instanceManager.initialize(context)
        underTest = OpenXrDeviceCapabilityProvider(context, instanceManager.nativeManager)
    }

    @Test
    fun getLifecycle_returnsResumedLifecycle() = runTest {
        val lifecycle = underTest.lifecycle
        advanceUntilIdle()

        assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun getPreferredDisplayBlendMode_returnBlendMode() {
        // DisplayBlendMode value comes from third_party/jetpack_xr_natives/common/openxr_stub.cc.
        assertThat(underTest.getPreferredDisplayBlendMode()).isEqualTo(DisplayBlendMode.ADDITIVE)
    }

    @Test
    fun isHandTrackingSupported_returnsTrueForAllModes() {
        assertThat(underTest.isHandTrackingModeSupported(HandTrackingMode.DISABLED)).isTrue()
        assertThat(underTest.isHandTrackingModeSupported(HandTrackingMode.BOTH)).isTrue()
    }

    @Test
    fun isEyeTrackingSupported_returnsTrueForAllModes() {
        assertThat(underTest.isEyeTrackingModeSupported(EyeTrackingMode.DISABLED)).isTrue()
        assertThat(underTest.isEyeTrackingModeSupported(EyeTrackingMode.FINE_TRACKING)).isTrue()
        assertThat(underTest.isEyeTrackingModeSupported(EyeTrackingMode.COARSE_TRACKING)).isTrue()
    }

    @Test
    fun isDepthEstimationSupported_returnsTrueForAllModesExceptSmoothAndRaw() {
        assertThat(underTest.isDepthEstimationModeSupported(DepthEstimationMode.DISABLED)).isTrue()
        assertThat(underTest.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_ONLY))
            .isTrue()
        assertThat(underTest.isDepthEstimationModeSupported(DepthEstimationMode.RAW_ONLY)).isTrue()
        // OpenXR assumed to explicitly not support smooth and raw depth simultaneously
        assertThat(underTest.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW))
            .isFalse()
    }

    @Test
    fun isGeospatialSupportedAny_returnsTrueForAllModes() {
        assertThat(underTest.isGeospatialModeSupported(GeospatialMode.DISABLED)).isTrue()
        assertThat(underTest.isGeospatialModeSupported(GeospatialMode.SPATIAL)).isTrue()
    }

    @Test
    fun isRenderingSupportedAny_onlySupportsStereo() {
        assertThat(underTest.isRenderingModeSupported(RenderingMode.MONO)).isFalse()
        assertThat(underTest.isRenderingModeSupported(RenderingMode.STEREO)).isTrue()
    }
}
