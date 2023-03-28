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

package androidx.credentials.provider.utils

import android.service.credentials.ClearCredentialStateRequest
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.provider.ProviderClearCredentialStateRequest

@RequiresApi(34)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ClearCredentialUtil {
    companion object {
        @JvmStatic
        internal fun convertToJetpackRequest(request: ClearCredentialStateRequest):
            ProviderClearCredentialStateRequest {
            return ProviderClearCredentialStateRequest(request.callingAppInfo)
        }
    }
}