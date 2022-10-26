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

package androidx.credentials.playservices.controllers.BeginSignIn

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

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import com.google.android.gms.auth.api.identity.BeginSignInRequest

/**
 * A utility class to handle logic for the begin sign in controller.
 *
 * @hide
 */
class BeginSignInControllerUtility {

    companion object {
        internal fun constructBeginSignInRequest(request: GetCredentialRequest):
            BeginSignInRequest {
            val requestBuilder = BeginSignInRequest.Builder()
            for (option in request.getCredentialOptions) {
                if (option is GetPasswordOption) {
                    requestBuilder.setPasswordRequestOptions(
                        BeginSignInRequest.PasswordRequestOptions.Builder()
                            .setSupported(true)
                            .build()
                    )
                }
                // TODO("Add GoogleIDToken version and passkey version")
            }
            return requestBuilder
                .setAutoSelectEnabled(request.isAutoSelectAllowed)
                .build()
        }
    }
}