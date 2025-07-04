/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.transfer

import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * The state of the primary provider's credentials
 *
 * @property totalNumCredentials the total number of credentials the provider holds
 * @property numPasswords the number of passwords the provider holds
 * @property numPasskeys the number of passkeys the provider holds
 * @property totalSizeBytes the total size of credentials the provider will export
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CredentialTransferCapabilities(
    public val totalNumCredentials: Int,
    public val numPasswords: Int,
    public val numPasskeys: Int,
    public val totalSizeBytes: Long,
) {
    public companion object {
        private const val TOTAL_NUM_CREDENTIALS_KEY = "TOTAL_NUM_CREDENTIALS"
        private const val NUM_PASSWORDS_KEY = "NUM_PASSWORDS"
        private const val NUM_PASSKEYS_KEY = "NUM_PASSKEYS"
        private const val TOTAL_SIZE_BYTES_KEY = "TOTAL_SIZE_BYTES"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun asBundle(capabilities: CredentialTransferCapabilities): Bundle =
            Bundle().apply {
                putInt(TOTAL_NUM_CREDENTIALS_KEY, capabilities.totalNumCredentials)
                putInt(NUM_PASSWORDS_KEY, capabilities.numPasswords)
                putInt(NUM_PASSKEYS_KEY, capabilities.numPasskeys)
                putLong(TOTAL_SIZE_BYTES_KEY, capabilities.totalSizeBytes)
            }
    }
}
