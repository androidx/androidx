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

package androidx.credentials.exceptions.restorecredential

import androidx.credentials.exceptions.CreateCredentialException

/**
 * During the Restore Credential creation, this is thrown when the developer requests backup to
 * cloud but the user device did not enable end-to-end-encryption or backup.
 */
class E2eeUnavailableException(errorMessage: CharSequence) :
    CreateCredentialException(TYPE_E2EE_UNAVAILABLE_EXCEPTION, errorMessage) {
    internal companion object {
        internal const val TYPE_E2EE_UNAVAILABLE_EXCEPTION =
            "androidx.credentials.TYPE_E2EE_UNAVAILABLE_EXCEPTION"
    }
}
