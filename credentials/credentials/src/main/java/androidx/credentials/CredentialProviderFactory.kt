/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Factory that returns the credential provider to be used by Credential Manager.
 *
 * @hide
 */
class CredentialProviderFactory {
    companion object {
        private const val TAG = "CredProviderFactory"

        /** The metadata key to be used when specifying the provider class name in the
         * android manifest file. */
        private const val CREDENTIAL_PROVIDER_KEY = "androidx.credentials.CREDENTIAL_PROVIDER_KEY"

        /**
         * Returns the best available provider.
         * Pre-U, the provider is determined by the provider library that the developer includes in
         * the app. Developer must not add more than one provider library.
         * Post-U, providers will be registered with the framework, and enabled by the user.
         */
        fun getBestAvailableProvider(context: Context): CredentialProvider? {
            return tryCreatePreUOemProvider(context)
        }

        private fun tryCreatePreUOemProvider(context: Context): CredentialProvider? {
            val classNames = getAllowedProvidersFromManifest(context)
            if (classNames.isEmpty()) {
                return null
            } else {
                return instantiatePreUProvider(classNames, context)
            }
        }

        private fun instantiatePreUProvider(classNames: List<String>, context: Context):
            CredentialProvider? {
            var provider: CredentialProvider? = null
            for (className in classNames) {
                try {
                    val klass = Class.forName(className)
                    val p = klass.getConstructor(Context::class.java).newInstance(context) as
                        CredentialProvider
                    // TODO("Retrys and look into multiple constructors")
                    if (p.isAvailableOnDevice()) {
                        if (provider != null) {
                            Log.i(TAG, "Only one active OEM CredentialProvider allowed")
                            return null
                        }
                        provider = p
                    }
                } catch (_: Throwable) {
                }
            }
            return provider
        }

        @Suppress("deprecation")
        private fun getAllowedProvidersFromManifest(context: Context): List<String> {
            val packageInfo = context.packageManager
                .getPackageInfo(
                    context.packageName, PackageManager.GET_META_DATA or
                        PackageManager.GET_SERVICES
                )

            val classNames = mutableListOf<String>()
            if (packageInfo.services != null) {
                for (serviceInfo in packageInfo.services) {
                    if (serviceInfo.metaData != null) {
                        val className = serviceInfo.metaData.getString(CREDENTIAL_PROVIDER_KEY)
                        if (className != null) {
                            classNames.add(className)
                        }
                    }
                }
            }
            return classNames.toList()
        }
    }
}
