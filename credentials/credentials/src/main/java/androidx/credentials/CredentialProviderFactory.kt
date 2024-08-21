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
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/** Factory that returns the credential provider to be used by Credential Manager. */
internal class CredentialProviderFactory(val context: Context) {

    @set:VisibleForTesting
    @get:VisibleForTesting
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    var testMode = false

    @set:VisibleForTesting
    @get:VisibleForTesting
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    var testPostUProvider: CredentialProvider? = null

    @set:VisibleForTesting
    @get:VisibleForTesting
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    var testPreUProvider: CredentialProvider? = null

    companion object {
        private const val TAG = "CredProviderFactory"
        private const val MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL = Build.VERSION_CODES.TIRAMISU

        /**
         * The metadata key to be used when specifying the provider class name in the android
         * manifest file.
         */
        private const val CREDENTIAL_PROVIDER_KEY = "androidx.credentials.CREDENTIAL_PROVIDER_KEY"
    }

    /**
     * Returns the best available provider. The best available provider is determined by the
     * provided [request]. If the provided request is for the use-case of [RestoreCredential], then
     * the pre-U provider is used. If not, then the provider is determined by the API level.
     *
     * @param request is a credential request of either [CreateRestoreCredentialRequest],
     *   [ClearCredentialRequestTypes.ClearRestoreCredential], or [GetCredentialRequest] that can
     *   determine [CredentialProvider] type.
     * @return the best available provider, or null if no provider is available.
     */
    fun getBestAvailableProvider(
        request: Any,
        shouldFallbackToPreU: Boolean = true
    ): CredentialProvider? {
        if (
            request is CreateRestoreCredentialRequest ||
                request == ClearCredentialRequestTypes.CLEAR_RESTORE_CREDENTIAL
        ) {
            return tryCreatePreUOemProvider()
        } else if (request is GetCredentialRequest) {
            for (option in request.credentialOptions) {
                if (option is GetRestoreCredentialOption || option is GetDigitalCredentialOption) {
                    if (request.credentialOptions.any { it !is GetDigitalCredentialOption }) {
                        throw IllegalArgumentException(
                            "`GetDigitalCredentialOption` cannot be" +
                                " combined with other option types in a single request"
                        )
                    }
                    return tryCreatePreUOemProvider()
                }
            }
        }
        return getBestAvailableProvider(shouldFallbackToPreU)
    }

    /**
     * Returns the best available provider. Pre-U, the provider is determined by the provider
     * library that the developer includes in the app. Developer must not add more than one provider
     * library. Post-U, providers will be registered with the framework, and enabled by the user.
     */
    fun getBestAvailableProvider(shouldFallbackToPreU: Boolean = true): CredentialProvider? {
        if (Build.VERSION.SDK_INT >= 34) { // Android U
            val postUProvider = tryCreatePostUProvider()
            if (postUProvider == null && shouldFallbackToPreU) {
                return tryCreatePreUOemProvider()
            }
            return postUProvider
        } else if (Build.VERSION.SDK_INT <= MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL) {
            return tryCreatePreUOemProvider()
        } else {
            return null
        }
    }

    private fun tryCreatePreUOemProvider(): CredentialProvider? {
        if (testMode) {
            if (testPreUProvider == null) {
                return null
            }
            val isAvailable = testPreUProvider!!.isAvailableOnDevice()
            if (isAvailable) {
                return testPreUProvider
            }
            return null
        }

        val classNames = getAllowedProvidersFromManifest(context)
        if (classNames.isEmpty()) {
            return null
        } else {
            return instantiatePreUProvider(classNames, context)
        }
    }

    @RequiresApi(34)
    private fun tryCreatePostUProvider(): CredentialProvider? {
        if (testMode) {
            if (testPostUProvider == null) {
                return null
            }
            val isAvailable = testPostUProvider!!.isAvailableOnDevice()
            if (isAvailable) {
                return testPostUProvider
            }
            return null
        }

        val provider = CredentialProviderFrameworkImpl(context)
        if (provider.isAvailableOnDevice()) {
            return provider
        }
        return null
    }

    private fun instantiatePreUProvider(
        classNames: List<String>,
        context: Context
    ): CredentialProvider? {
        var provider: CredentialProvider? = null
        for (className in classNames) {
            try {
                val klass = Class.forName(className)
                val p =
                    klass.getConstructor(Context::class.java).newInstance(context)
                        as CredentialProvider
                if (p.isAvailableOnDevice()) {
                    if (provider != null) {
                        Log.i(TAG, "Only one active OEM CredentialProvider allowed")
                        return null
                    }
                    provider = p
                }
            } catch (_: Throwable) {}
        }
        return provider
    }

    @Suppress("deprecation")
    private fun getAllowedProvidersFromManifest(context: Context): List<String> {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA or PackageManager.GET_SERVICES
            )

        val classNames = mutableListOf<String>()
        if (packageInfo.services != null) {
            for (serviceInfo in packageInfo.services!!) {
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
