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

/** Success response of exporting the credentials to the provider */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ExportCredentialsResponse(
    public val numSuccess: Int,
    public val numFailure: Int,
    public val numIgnored: Int,
) {
    public companion object {
        // values derived from identity credential sdk
        private const val NUM_SUCCESS_KEY = "NUM_SUCCESS"
        private const val NUM_FAILURE_KEY = "NUM_FAILURE"
        private const val NUM_IGNORED_KEY = "NUM_IGNORED"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun asBundle(response: ExportCredentialsResponse): Bundle =
            Bundle().apply {
                putInt(NUM_SUCCESS_KEY, response.numSuccess)
                putInt(NUM_FAILURE_KEY, response.numFailure)
                putInt(NUM_IGNORED_KEY, response.numIgnored)
            }
    }
}
