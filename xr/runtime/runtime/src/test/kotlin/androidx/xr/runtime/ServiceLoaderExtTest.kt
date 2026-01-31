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

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.AnotherFakeStateExtender
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeStateExtender
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import androidx.xr.runtime.internal.Service
import androidx.xr.runtime.manifest.FEATURE_XR_API_OPENXR
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(AndroidJUnit4::class)
class ServiceLoaderExtTest {

    @Test
    // TODO(b/440615454) - Move this test to arcore-testing.
    fun loadProviders_loadsProviders() {
        assertThat(
                loadProviders(
                        PerceptionRuntimeFactory::class.java,
                        listOf(FakePerceptionRuntimeFactory::class.java.name),
                    )
                    .single()
            )
            .isInstanceOf(FakePerceptionRuntimeFactory::class.java)
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

        assertThat(stateExtenders.size).isEqualTo(2)

        // TODO(b/436933956) - temp. dependency on arcore package is pulling in
        // PerceptionStateExtender
        assertThat(stateExtenders.any { it is FakeStateExtender || it is AnotherFakeStateExtender })
            .isTrue()
    }

    @Test
    fun getDeviceContextFeatures_onRobolectric_returnsEmptySet() {
        assertThat(getDeviceContextFeatures(ApplicationProvider.getApplicationContext())).isEmpty()
    }

    @Test
    fun getDeviceContextFeatures_notOnRobolectric_addsFullStack() {
        ShadowBuild.setFingerprint("a_real_device")

        assertThat(getDeviceContextFeatures(ApplicationProvider.getApplicationContext()))
            .containsExactly(Feature.FULLSTACK)
    }

    @Test
    fun getDeviceContextFeatures_onOpenXrDevice_addsOpenXr() {
        ShadowBuild.setFingerprint("a_real_device")
        val context: Context = ApplicationProvider.getApplicationContext()
        shadowOf(context.packageManager)
            .setSystemFeature(FEATURE_XR_API_OPENXR, /* supported= */ true)

        assertThat(getDeviceContextFeatures(context)).contains(Feature.OPEN_XR)
    }

    @Test
    fun getDeviceContextFeatures_onSpatialDevice_addsSpatial() {
        ShadowBuild.setFingerprint("a_real_device")
        val context: Context = ApplicationProvider.getApplicationContext()
        shadowOf(context.packageManager)
            .setSystemFeature(FEATURE_XR_API_SPATIAL, /* supported= */ true)

        assertThat(getDeviceContextFeatures(context)).contains(Feature.SPATIAL)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun getDeviceContextFeatures_onProjectedActivity_addsProjected() {
        ShadowBuild.setFingerprint("a_real_device")
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val activityInfo = ActivityInfo()
        activityInfo.packageName = activity.packageName
        activityInfo.name = activity.componentName.className
        val field = ActivityInfo::class.java.getField("requiredDisplayCategory")
        field.set(activityInfo, "xr_projected")
        val packageInfo = PackageInfo()
        packageInfo.packageName = activity.packageName
        packageInfo.activities = arrayOf(activityInfo)

        shadowOf(activity.packageManager).installPackage(packageInfo)

        assertThat(getDeviceContextFeatures(activity)).contains(Feature.PROJECTED)
    }

    @Test
    fun getDeviceContextFeatures_onNonProjectedActivity_doesNotAddProjected() {
        ShadowBuild.setFingerprint("a_real_device")

        assertThat(getDeviceContextFeatures(ApplicationProvider.getApplicationContext()))
            .doesNotContain(Feature.PROJECTED)
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
