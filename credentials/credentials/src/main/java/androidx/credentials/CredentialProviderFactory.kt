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
import android.os.Build
import android.util.Log

/**
 * Factory that returns the credential provider to be used by Credential Manager.
 *
 * @hide
 */
class CredentialProviderFactory {
    companion object {
        private const val TAG = "CredProviderFactory"
        private const val MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL = Build.VERSION_CODES.TIRAMISU

        /** The metadata key to be used when specifying the provider class name in the
         * android manifest file. */
        private const val PRE_U_PROVIDER_CLASS_NAME_KEY =
            "androidx.credentials.CREDENTIAL_PROVIDER_KEY"

        /** The service name to be used when declaring a metadata holder service for the purpose
         * of specifying a provider implementation class name. */
        private const val PRE_U_CREDENTIAL_PROVIDER_SERVICE_NAME =
            "CredentialProviderMetadataHolder"

        /**
         * Returns the best available provider.
         * Pre-U, the provider is determined by the provider library that the developer includes in
         * the app. Developer must not add more than one provider library.
         * Post-U, providers will be registered with the framework, and enabled by the user.
         */
        fun getBestAvailableProvider(context: Context): CredentialProvider? {
            if (Build.VERSION.SDK_INT <= MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL) {
                return tryCreatePreUOemProvider(context)
            } else {
                // TODO("Implement")
                throw UnsupportedOperationException("Post-U not supported yet")
            }
        }

        private fun tryCreatePreUOemProvider(context: Context): CredentialProvider? {
            val className: String? = getClassNameFromManifest(context)
            return if (!className.isNullOrEmpty()) {
                Log.i(TAG, "Classname retrieved from manifest: $className")
                instantiatePreUProvider(className)
            } else {
                Log.i(TAG, "No class name found in the manifest")
                null
            }
        }

        private fun instantiatePreUProvider(className: String): CredentialProvider? {
            return try {
                val klass = Class.forName(className)
                val provider: CredentialProvider =
                    klass.getConstructor().newInstance()
                        as CredentialProvider
                if (!provider.isAvailableOnDevice()) {
                    Log.i(TAG, "Credential Provider library found but " +
                        "not available on the device")
                }
                provider
            } catch (throwable: Throwable) {
                Log.i(TAG, "Unable to instantiate class using reflection: " +
                    throwable.message)
                null
            }
        }

        @Suppress("deprecation")
        private fun getClassNameFromManifest(context: Context): String? {
            Log.i(TAG, "Package name: " + context.packageName)
            val packageInfo = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_META_DATA or
                        PackageManager.GET_SERVICES)

            var providerClassName = ""
            if (packageInfo.services == null) {
                Log.i(TAG, "No services found in the manifest")
                return providerClassName
            }
            for (serviceInfo in packageInfo.services) {
                if (serviceInfo.name.contains(PRE_U_CREDENTIAL_PROVIDER_SERVICE_NAME)) {
                    Log.i(TAG, "Found service name: " + serviceInfo.name)
                    val classname =
                        serviceInfo.metaData.getString(PRE_U_PROVIDER_CLASS_NAME_KEY)
                    if (!classname.isNullOrEmpty()) {
                        if (providerClassName.isEmpty()) {
                            providerClassName = classname
                        } else {
                            Log.i(TAG, "More than one credential provider " +
                                "libraries found")
                            return null
                        }
                    }
                }
            }
            return providerClassName
        }
    }
}