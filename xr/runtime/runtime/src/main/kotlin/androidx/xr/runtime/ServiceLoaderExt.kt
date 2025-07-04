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
import android.os.Build
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.Service
import androidx.xr.runtime.manifest.FEATURE_XR_API_OPENXR
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import java.util.ServiceLoader

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
internal fun <S : Any> loadProviders(
    service: Class<S>,
    providersClassNames: List<String>,
): List<S> {
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

/**
 * Returns the first service provider from [providers] that has its requirements satisfied by the
 * [features] supported by the current device.
 */
internal fun <S : Service> selectProvider(providers: List<S>, features: Set<Feature>): S? =
    providers.firstOrNull { features.containsAll(it.requirements) }

/** Returns the features that this device supports. */
internal fun getDeviceFeatures(context: Context): Set<Feature> {
    // Short-circuit for unit tests environments.
    if (Build.FINGERPRINT.contains("robolectric")) return emptySet()

    val features = mutableSetOf<Feature>(Feature.FULLSTACK)
    val packageManager = context.packageManager

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
