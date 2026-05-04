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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
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
@RestrictTo(Scope.LIBRARY)
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
