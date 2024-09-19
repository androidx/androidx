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

import androidx.annotation.RestrictTo

/**
 * During the [RegistryManager.registerCredentials] transaction, this is thrown when configurations
 * are mismatched for the RegistryManager service, typically indicating the service provider
 * dependency is missing in the manifest or some system service is not enabled.
 *
 * Please check your dependencies and make sure to include the intended service provider dependency.
 * For example, for devices with Play Services, the dependency
 * `androidx.credentials:credentials-provider-registry-play-services` should be added.
 *
 * @param errorMessage the error message
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RegisterCredentialsConfigurationException(errorMessage: CharSequence? = null) :
    RegisterCredentialsException(
        type = TYPE_REGISTER_CREDENTIALS_CONFIGURATION_EXCEPTION,
        errorMessage = errorMessage
    ) {
    private companion object {
        const val TYPE_REGISTER_CREDENTIALS_CONFIGURATION_EXCEPTION =
            "androidx.credentials.provider.registry.TYPE_REGISTER_CREDENTIALS_CONFIGURATION_EXCEPTION"
    }
}
