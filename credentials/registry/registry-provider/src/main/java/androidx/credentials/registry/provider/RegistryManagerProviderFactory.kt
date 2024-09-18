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

package androidx.credentials.registry.provider

import android.content.Context
import android.content.pm.PackageManager

/** Factory that returns the RegistryManager provider to be used. */
internal class RegistryManagerProviderFactory(private val context: Context) {
    companion object {
        /**
         * The metadata key to be used when specifying the provider class name in the android
         * manifest file.
         */
        private const val REGISTRY_MANAGER_PROVIDER_KEY =
            "androidx.credentials.registry.provider.REGISTRY_MANAGER_PROVIDER_KEY"
    }

    fun getBestAvailableProvider(): RegistryManagerProvider? {
        val classNames = getAllowedProvidersFromManifest()
        return if (classNames.isEmpty()) {
            null
        } else {
            instantiateProvider(classNames)
        }
    }

    @Suppress("deprecation")
    private fun getAllowedProvidersFromManifest(): List<String> {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA or PackageManager.GET_SERVICES
            )

        val classNames = mutableListOf<String>()
        if (packageInfo.services != null) {
            for (serviceInfo in packageInfo.services!!) {
                if (serviceInfo.metaData != null) {
                    val className = serviceInfo.metaData.getString(REGISTRY_MANAGER_PROVIDER_KEY)
                    if (className != null) {
                        classNames.add(className)
                    }
                }
            }
        }
        return classNames.toList()
    }

    private fun instantiateProvider(classNames: List<String>): RegistryManagerProvider? {
        var provider: RegistryManagerProvider? = null
        for (className in classNames) {
            try {
                val klass = Class.forName(className)
                val p =
                    klass.getConstructor(Context::class.java).newInstance(context)
                        as RegistryManagerProvider
                if (p.isAvailable()) {
                    if (provider != null) { // Only one active OEM CredentialProvider allowed
                        return null
                    }
                    provider = p
                }
            } catch (_: Throwable) {}
        }
        return provider
    }
}
