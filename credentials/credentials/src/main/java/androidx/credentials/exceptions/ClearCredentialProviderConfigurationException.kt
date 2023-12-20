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

package androidx.credentials.exceptions

/**
 * During the clear credential flow, this is thrown when configurations are mismatched for the
 * provider, typically indicating the provider dependency is missing in the manifest or some
 * system service is not enabled.
 *
 * @see GetCredentialException
 */
class ClearCredentialProviderConfigurationException @JvmOverloads constructor(
    errorMessage: CharSequence? = null
) : ClearCredentialException(TYPE_CLEAR_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION, errorMessage) {
    internal companion object {
        internal const val TYPE_CLEAR_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION =
            "androidx.credentials.TYPE_CLEAR_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION"
    }
}
