/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.JxrPlatformAdapterFactory
import androidx.xr.runtime.internal.RuntimeFactory
import androidx.xr.runtime.internal.Service
import androidx.xr.runtime.manifest.FEATURE_XR_API_OPENXR
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import androidx.xr.runtime.testing.AnotherFakeStateExtender
import androidx.xr.runtime.testing.FakeJxrPlatformAdapterFactory
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.runtime.testing.FakeStateExtender
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowBuild

@RunWith(AndroidJUnit4::class)
class ServiceLoaderExtTest {

    @Test
    fun loadProviders_loadsProviders() {
        assertThat(
                loadProviders(
                        RuntimeFactory::class.java,
                        listOf(FakeRuntimeFactory::class.java.name),
                    )
                    .single()
            )
            .isInstanceOf(FakeRuntimeFactory::class.java)
        assertThat(
                loadProviders(
                        JxrPlatformAdapterFactory::class.java,
                        listOf(FakeJxrPlatformAdapterFactory::class.java.name),
                    )
                    .single()
            )
            .isInstanceOf(FakeJxrPlatformAdapterFactory::class.java)
        assertThat(
                loadProviders(StateExtender::class.java, listOf(FakeStateExtender::class.java.name))
                    .iterator()
                    .next()
            )
            .isInstanceOf(FakeStateExtender::class.java)
    }

    @Test
    fun loadProviders_combinesFastAndLoaderProviders() {
        val stateExtenders =
            loadProviders(StateExtender::class.java, listOf(FakeStateExtender::class.java.name))

        assertThat(stateExtenders.size).isEqualTo(1)
        for (stateExtender in stateExtenders) {
            assert(stateExtender is FakeStateExtender || stateExtender is AnotherFakeStateExtender)
        }
    }

    @Test
    fun getDeviceFeatures_onRobolectric_returnsEmptySet() {
        assertThat(getDeviceFeatures(ApplicationProvider.getApplicationContext())).isEmpty()
    }

    @Test
    fun getDeviceFeatures_notOnRobolectric_addsFullStack() {
        ShadowBuild.setFingerprint("a_real_device")

        assertThat(getDeviceFeatures(ApplicationProvider.getApplicationContext()))
            .containsExactly(Feature.FULLSTACK)
    }

    @Test
    fun getDeviceFeatures_onOpenXrDevice_addsOpenXr() {
        ShadowBuild.setFingerprint("a_real_device")
        val context: Context = ApplicationProvider.getApplicationContext()
        shadowOf(context.packageManager)
            .setSystemFeature(FEATURE_XR_API_OPENXR, /* supported= */ true)

        assertThat(getDeviceFeatures(context)).contains(Feature.OPEN_XR)
    }

    @Test
    fun getDeviceFeatures_onSpatialDevice_addsSpatial() {
        ShadowBuild.setFingerprint("a_real_device")
        val context: Context = ApplicationProvider.getApplicationContext()
        shadowOf(context.packageManager)
            .setSystemFeature(FEATURE_XR_API_SPATIAL, /* supported= */ true)

        assertThat(getDeviceFeatures(context)).contains(Feature.SPATIAL)
    }

    @Test
    fun selectProvider_selectsSupportedProvider() {
        val supportedProvider =
            object : Service {
                override val requirements: Set<Feature> = setOf(Feature.FULLSTACK)
            }
        val unsupportedProvider =
            object : Service {
                override val requirements: Set<Feature> = setOf(Feature.FULLSTACK, Feature.OPEN_XR)
            }

        assertThat(
                selectProvider(
                    listOf(unsupportedProvider, supportedProvider),
                    setOf(Feature.FULLSTACK),
                )
            )
            .isEqualTo(supportedProvider)
    }

    @Test
    fun selectProvider_noSupportedProvider_returnsNull() {
        val unsupportedProvider =
            object : Service {
                override val requirements: Set<Feature> = setOf(Feature.FULLSTACK, Feature.OPEN_XR)
            }

        assertThat(selectProvider(listOf(unsupportedProvider), emptySet())).isNull()
    }
}
