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
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.Service
import androidx.xr.runtime.manifest.FEATURE_XR_API_OPENXR
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import java.util.ServiceLoader

// TODO(b/440615454): Reduce visibility to internal once stub providers are added for testing.
/**
 * Loads all well-known service providers directly. Combines the results with any additional
 * providers discovered via the default service loader implementation.
 *
 * This is useful in some app configurations where the APK is too big and the default service loader
 * implementation is not able to automatically find all the available service providers.
 *
 * @param service the service to load.
 * @param providersClassNames the list of known service providers to load.
 * @return the list of loaded service providers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun <S : Any> loadProviders(service: Class<S>, providersClassNames: List<String>): List<S> {
    val providers = mutableListOf<S>()

    val filteredProviderClassNames =
        providersClassNames
            .filter { providerClassName ->
                try {
                    val providerClass = Class.forName(providerClassName)
                    require(service.isAssignableFrom(providerClass)) {
                        "Provider $providerClassName is not a derived class of $service"
                    }
                    val provider = providerClass.getDeclaredConstructor().newInstance()
                    providers.add(service.cast(provider)!!)
                    true
                } catch (e: ClassNotFoundException) {
                    false
                }
            }
            .toSet()

    val filteredServiceLoaderClasses =
        ServiceLoader.load(service).filterNotNull().filter { providerClass ->
            providerClass.javaClass.name !in filteredProviderClassNames
        }

    return providers + filteredServiceLoaderClasses
}

private const val REQUIRED_DISPLAY_CATEGORY_XR_PROJECTED = "xr_projected"
private const val PROJECTED_DEVICE_NAME = "ProjectionDevice"

private fun hasXrProjectedDisplayCategory(activityInfo: ActivityInfo): Boolean {
    // TODO b/460536048 - Remove reflection once requiredDisplayCategory is public in SDK 36
    // Use reflection to access requiredDisplayCategory to avoid compile errors
    // when using an older compileSdkVersion.
    return try {
        val field = ActivityInfo::class.java.getField("requiredDisplayCategory")
        val category = field.get(activityInfo) as? String
        category == REQUIRED_DISPLAY_CATEGORY_XR_PROJECTED
    } catch (e: Exception) {
        false
    }
}

/**
 * Returns true if the activity associated with the [context] is a projected activity.
 *
 * This is determined by checking if the activity's requiredDisplayCategory is set to "xr_projected"
 * in the AndroidManifest.xml See example at
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:xr/arcore/integration-tests/projected-testapp/src/main/AndroidManifest.xml
 */
internal fun isProjectedActivity(context: Context): Boolean {
    if (context !is Activity) {
        return false
    }
    return try {
        val packageManager = context.packageManager
        val componentName = context.componentName
        val activityInfo =
            packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasXrProjectedDisplayCategory(activityInfo)
        } else {
            false
        }
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

// TODO: b/458737779 - Implement tests when the test rule is available
/** Returns whether the provided context is the Projected device context. */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal fun isProjectedDeviceContext(context: Context): Boolean =
    getVirtualDevice(context)?.name?.startsWith(PROJECTED_DEVICE_NAME) == true

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun getVirtualDevice(context: Context) =
    context.getSystemService(VirtualDeviceManager::class.java).virtualDevices.find {
        it.deviceId == context.deviceId
    }

/**
 * Returns the first service provider from [providers] that has its requirements satisfied by the
 * [features] supported by the current device.
 */
internal fun <S : Service> selectProvider(providers: List<S>, features: Set<Feature>): S? =
    providers.firstOrNull { features.containsAll(it.requirements) }

/** Returns the set of features available for the current context associated with the device. */
internal fun getDeviceContextFeatures(context: Context): Set<Feature> {
    // Short-circuit for unit tests environments.
    if (Build.FINGERPRINT.contains("robolectric")) return emptySet()

    val features = mutableSetOf<Feature>(Feature.FULLSTACK)
    val packageManager = context.packageManager

    if (context is Activity && isProjectedActivity(context)) {
        features.add(Feature.PROJECTED)
    } else if (
        // TODO: b/458737779 - Implement tests when the test rule is available
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isProjectedDeviceContext(context)
    ) {
        features.add(Feature.PROJECTED)
    }

    // TODO(b/398957058): Remove emulator check once the emulator has the system feature.
    if (
        packageManager.hasSystemFeature(FEATURE_XR_API_OPENXR) ||
            Build.FINGERPRINT.contains("emulator")
    ) {
        features.add(Feature.OPEN_XR)
    }

    // TODO(b/398957058): Remove emulator check once the emulator has the system feature.
    if (
        packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL) ||
            Build.FINGERPRINT.contains("emulator")
    ) {
        features.add(Feature.SPATIAL)
    }

    return features
}
