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
 * During the get credential flow, this is returned when no viable credential is available for the
 * the user. This can be caused by various scenarios such as that the user doesn't have any
 * credential or the user doesn't grant consent to using any available credential. Upon this
 * exception, your app should navigate to use the regular app sign-up or sign-in screen.
 * When that succeeds, you should invoke [androidx.credentials.CredentialManager.createCredential]
 * (kotlin) or [androidx.credentials.CredentialManager.createCredentialAsync] (java) to store the
 * login info, so that your user can sign in more easily through Credential Manager the next time.
 */
class NoCredentialException @JvmOverloads constructor(
    errorMessage: CharSequence? = null
) : GetCredentialException(TYPE_FRAMEWORK_TYPE_NO_CREDENTIAL, errorMessage) {

    internal companion object {
        /** Maintain a copy of the framework type so that we aren't restricted by the API level. */
        internal const val TYPE_FRAMEWORK_TYPE_NO_CREDENTIAL =
            "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL"
    }
}
