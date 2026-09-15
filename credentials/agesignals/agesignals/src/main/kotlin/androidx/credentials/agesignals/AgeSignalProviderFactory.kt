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

package androidx.credentials.agesignals

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/** Factory that discovers and instantiates the active [AgeSignalProvider] from the manifest. */
internal class AgeSignalProviderFactory(context: Context) {

    private val context: Context = context.applicationContext ?: context

    @set:VisibleForTesting
    @get:VisibleForTesting
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    var testMode: Boolean = false

    @set:VisibleForTesting
    @get:VisibleForTesting
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    var testProvider: AgeSignalProvider? = null

    companion object {
        private const val TAG = "AgeSignalProviderFactory"

        /**
         * The metadata key to be used when specifying the provider class name in the android
         * manifest file.
         */
        private const val AGE_SIGNAL_PROVIDER_KEY =
            "androidx.credentials.agesignals.AGE_SIGNAL_PROVIDER_KEY"
    }

    /**
     * Discovers and returns the best available provider configured in the manifest.
     *
     * @return an active, available [AgeSignalProvider], or `null` if none are found or if multiple
     *   competing active providers are declared.
     */
    fun getBestAvailableProvider(): AgeSignalProvider? {
        if (testMode) {
            val provider = testProvider ?: return null
            return if (provider.isAvailable()) provider else null
        }

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
            try {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA or PackageManager.GET_SERVICES,
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to get package info for ${context.packageName}", t)
                return emptyList()
            }

        val classNames = mutableListOf<String>()
        val services = packageInfo.services
        if (services != null) {
            for (serviceInfo in services) {
                val metaData = serviceInfo.metaData
                if (metaData != null) {
                    val className = metaData.getString(AGE_SIGNAL_PROVIDER_KEY)
                    if (!className.isNullOrEmpty()) {
                        classNames.add(className)
                    }
                }
            }
        }
        return classNames.distinct()
    }

    @VisibleForTesting
    internal fun instantiateProvider(classNames: List<String>): AgeSignalProvider? {
        var provider: AgeSignalProvider? = null
        for (className in classNames) {
            try {
                val klass = Class.forName(className)
                val candidate =
                    klass.getConstructor(Context::class.java).newInstance(context)
                        as AgeSignalProvider
                if (candidate.isAvailable()) {
                    if (provider != null) {
                        Log.w(TAG, "Multiple active AgeSignalProviders found. Only one is allowed.")
                        return null
                    }
                    provider = candidate
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to instantiate AgeSignalProvider: $className", t)
            }
        }
        return provider
    }
}
