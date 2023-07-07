/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.internal

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.R

@RequiresApi(23)
internal class FrameworkImplHelper {
    companion object {
        /**
         * Take the create request's `credentialData` and add SDK specific values to it.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        @JvmStatic
        @RequiresApi(23)
        fun getFinalCreateCredentialData(
            request: CreateCredentialRequest,
            context: Context,
        ): Bundle {
            val createCredentialData = request.credentialData
            val displayInfoBundle = request.displayInfo.toBundle()
            displayInfoBundle.putParcelable(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON,
                Icon.createWithResource(
                    context,
                    when (request) {
                        is CreatePasswordRequest -> R.drawable.ic_password
                        is CreatePublicKeyCredentialRequest -> R.drawable.ic_passkey
                        else -> R.drawable.ic_other_sign_in
                    }
                )
            )
            createCredentialData.putBundle(
                CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO,
                displayInfoBundle
            )
            return createCredentialData
        }
    }
}
