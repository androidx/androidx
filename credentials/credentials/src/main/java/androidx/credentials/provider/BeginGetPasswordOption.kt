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
import androidx.credentials.PasswordCredential

/**
 * A request to begin the flow of retrieving the user's saved application password from their
 * password provider.
 *
 * @hide
 */
class BeginGetPasswordOption : BeginGetCredentialOption(
    type = PasswordCredential.TYPE_PASSWORD_CREDENTIAL
) {
    /** @hide */
    @Suppress("UNUSED_PARAMETER")
    companion object {
        @JvmStatic
        internal fun createFrom(data: Bundle): BeginGetPasswordOption {
            return BeginGetPasswordOption()
        }
    }
}
