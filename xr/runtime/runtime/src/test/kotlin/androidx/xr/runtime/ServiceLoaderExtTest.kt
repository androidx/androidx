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
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.interfaces.Service
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import androidx.xr.runtime.manifest.FEATURE_XR_API_OPENXR
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(AndroidJUnit4::class)
class ServiceLoaderExtTest {

    @Test
    fun loadProviders_loadsProviders() {
        assertThat(
                loadProviders(
                        PerceptionRuntimeFactory::class.java,
                        listOf(StubPerceptionRuntimeFactory::class.java.name),
                    )
                    .single()
            )
            .isInstanceOf(StubPerceptionRuntimeFactory::class.java)
        assertThat(
                loadProviders(StateExtender::class.java, listOf(StubStateExtender::class.java.name))
                    .iterator()
                    .next()
            )
            .isInstanceOf(StubStateExtender::class.java)
    }

    @Test
    fun loadProviders_combinesFastAndLoaderProviders() {
        val stateExtenders =
            loadProviders(StateExtender::class.java, listOf(StubStateExtender::class.java.name))

        assertThat(stateExtenders.size).isEqualTo(1)
        assertThat(stateExtenders.any { it is StubStateExtender }).isTrue()
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
    fun getDeviceContextFeatures_withForceOpenXrPropOne_addsOpenXr() {
        ShadowBuild.setFingerprint("a_real_device")
        ShadowSystemProperties.override(FORCE_OPENXR_PROPERTY, "1")

        assertThat(getDeviceContextFeatures(ApplicationProvider.getApplicationContext()))
            .contains(Feature.OPEN_XR)
    }

    @Test
    fun getDeviceContextFeatures_withForceOpenXrPropTrue_addsOpenXr() {
        ShadowBuild.setFingerprint("a_real_device")
        ShadowSystemProperties.override(FORCE_OPENXR_PROPERTY, "true")

        assertThat(getDeviceContextFeatures(ApplicationProvider.getApplicationContext()))
            .contains(Feature.OPEN_XR)
    }

    @Test
    fun getDeviceContextFeatures_withForceOpenXrPropDisabled_doesNotAddOpenXr() {
        ShadowBuild.setFingerprint("a_real_device")
        ShadowSystemProperties.override(FORCE_OPENXR_PROPERTY, "0")

        assertThat(getDeviceContextFeatures(ApplicationProvider.getApplicationContext()))
            .doesNotContain(Feature.OPEN_XR)
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
        field.set(activityInfo, REQUIRED_DISPLAY_CATEGORY_XR_PROJECTED)
        val packageInfo = PackageInfo()
        packageInfo.packageName = activity.packageName
        packageInfo.activities = arrayOf(activityInfo)

        shadowOf(activity.packageManager).installPackage(packageInfo)

        assertThat(getDeviceContextFeatures(activity)).contains(Feature.PROJECTED)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun getDeviceContextFeatures_onProjectedActivityLegacy_addsProjected() {
        ShadowBuild.setFingerprint("a_real_device")
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val activityInfo = ActivityInfo()
        activityInfo.packageName = activity.packageName
        activityInfo.name = activity.componentName.className
        val field = ActivityInfo::class.java.getField("requiredDisplayCategory")
        field.set(activityInfo, REQUIRED_DISPLAY_CATEGORY_XR_PROJECTED_LEGACY)
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

    @Test
    @Config(sdk = [Build.VERSION_CODES.S]) // API 31 (Android S)
    // RequiredDisplayCategory was introudced in API 34, any API below this would work
    fun isProjectedActivity_withMissingApi_doesNotCrashAndReturnsFalse() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val activityInfo = ActivityInfo()
        activityInfo.packageName = activity.packageName
        activityInfo.name = activity.componentName.className
        val packageInfo = PackageInfo()
        packageInfo.packageName = activity.packageName
        packageInfo.activities = arrayOf(activityInfo)

        shadowOf(activity.packageManager).installPackage(packageInfo)
        val result = isProjectedActivity(activity)

        assertThat(result).isFalse()
    }
}
