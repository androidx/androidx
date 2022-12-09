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

package androidx.credentials.provider

import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base request class for registering a credential.
 *
 * Providers will receive a subtype of this request with the call.
 *
 * @hide
 */
@RequiresApi(34)
open class BeginCreateCredentialProviderRequest internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val type: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val applicationInfo: ApplicationInfo
    ) {
    companion object {
        internal fun createFrom(
            type: String,
            data: Bundle,
            // TODO("Change to framework ApplicationInfo")
            callingPackage: String
        ): BeginCreateCredentialProviderRequest {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        BeginCreatePasswordCredentialRequest.createFrom(
                            data,
                            ApplicationInfo(callingPackage, ArrayList())
                        )
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        BeginCreatePublicKeyCredentialRequest.createFrom(
                            data,
                            ApplicationInfo(callingPackage, ArrayList())
                        )
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // TODO: Change to custom class when ready
                BeginCreateCredentialProviderRequest(
                    type,
                    ApplicationInfo(callingPackage, ArrayList())
                )
            }
        }
    }
}