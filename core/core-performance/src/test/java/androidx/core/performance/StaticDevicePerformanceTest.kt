/*
 * Copyright 2021 The Android Open Source Project
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemProperties

/** Unit tests for [StaticDevicePerformanceSupplier]. */
@RunWith(RobolectricTestRunner::class)
class StaticDevicePerformanceTest {

    @Test
    @Config(maxSdk = R)
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk30() = runTest {
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(0)
    }

    @Test
    @Config(maxSdk = R, minSdk = R)
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk30_declared() = runTest {
        // on R devices, it doesn't matter if you set the value, ignore it
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "30")
        ShadowBuild.reset()
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(0)
    }

    // declared an undefined perf class, treat it as 0
    @Test
    @Config(minSdk = S)
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk31_declared25() = runTest {
        // TODO(b/205732671): Use ShadowBuild.setMediaPerformanceClass when available
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "25")
        ShadowBuild.reset()
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(0)
    }

    @Test
    @Config(minSdk = S)
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk31_declared30() = runTest {
        // TODO(b/205732671): Use ShadowBuild.setMediaPerformanceClass when available
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "30")
        ShadowBuild.reset()
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(30)
    }

    @Test
    @Config(minSdk = S)
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk31_declared31() = runTest {
        // TODO(b/205732671): Use ShadowBuild.setMediaPerformanceClass when available
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "31")
        ShadowBuild.reset()
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(31)
    }

    @Test
    @Config(minSdk = S)
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk31_notDeclared() = runTest {
        ShadowBuild.reset()
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(0)
    }

    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk30_inList() = runTest {
        ShadowBuild.reset()
        ShadowBuild.setBrand("robolectric-BrandX")
        ShadowBuild.setProduct("ProductX")
        ShadowBuild.setDevice("Device30")
        ShadowBuild.setVersionRelease("11")
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(30)
    }

    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getMediaPerformanceClass_sdk31_inList() = runTest {
        ShadowBuild.reset()
        ShadowBuild.setBrand("robolectric-BrandX")
        ShadowBuild.setProduct("ProductX")
        ShadowBuild.setDevice("Device31")
        ShadowBuild.setVersionRelease("12")
        val pc = StaticDevicePerformanceSupplier()
        assertThat(pc.mediaPerformanceClassFlow.toList()).containsExactly(31)
    }
}
