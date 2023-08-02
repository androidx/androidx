/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.performance

import android.os.Build.VERSION_CODES.R
import android.os.Build.VERSION_CODES.S
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemProperties

/** Unit tests for [DefaultDevicePerformance]. */
@RunWith(RobolectricTestRunner::class)
class DefaultDevicePerformanceTest {

    @Test
    @Config(maxSdk = R, minSdk = R)
    fun mediaPerformanceClass_SdkR_DeclaredMpc() = runTest {
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "30")
        ShadowBuild.reset()
        val mpc = DefaultDevicePerformance().mediaPerformanceClass
        assertThat(mpc).isEqualTo(0)
    }

    @Test
    @Config(minSdk = S)
    fun mediaPerformanceClass_SdkS_DeclaredMpc() = runTest {
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "30")
        ShadowBuild.reset()
        val mpc = DefaultDevicePerformance().mediaPerformanceClass
        assertThat(mpc).isEqualTo(30)
    }

    @Test
    @Config(minSdk = S)
    fun mediaPerformanceClass_SdkS_BuildFingerprintMatch() = runTest {
        ShadowBuild.reset()
        ShadowBuild.setBrand("robolectric-BrandX")
        ShadowBuild.setProduct("ProductX")
        ShadowBuild.setDevice("Device31")
        ShadowBuild.setVersionRelease("12")
        val mpc = DefaultDevicePerformance().mediaPerformanceClass
        assertThat(mpc).isEqualTo(31)
    }

    @Test
    @Config(minSdk = S)
    fun mediaPerformanceClass_SdkS_DeclaredMpc_BuildFingerprintMatch() = runTest {
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "30")
        ShadowBuild.reset()
        ShadowBuild.setBrand("robolectric-BrandX")
        ShadowBuild.setProduct("ProductX")
        ShadowBuild.setDevice("Device31")
        ShadowBuild.setVersionRelease("12")
        val mpc = DefaultDevicePerformance().mediaPerformanceClass
        assertThat(mpc).isEqualTo(30)
    }
}
