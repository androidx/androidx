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

package androidx.core.telecom.test.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallException
import androidx.core.telecom.internal.AddCallResult
import androidx.core.telecom.internal.ConnectionServiceAdapter
import androidx.core.telecom.internal.JetpackConnectionService
import androidx.core.telecom.internal.TelecomManagerAdapter
import androidx.core.telecom.util.ExperimentalAppActions

@RequiresApi(Build.VERSION_CODES.O)
internal class ConnectionServiceFailPlatformSide : ConnectionServiceAdapter {

    @ExperimentalAppActions
    override fun createConnectionRequest(
        telecomManager: TelecomManagerAdapter,
        pendingConnectionRequest: JetpackConnectionService.PendingConnectionRequest
    ) {
        pendingConnectionRequest.completableDeferred?.complete(
            AddCallResult.Error(CallException.ERROR_UNKNOWN))
    }

    override fun getConnectionService(): JetpackConnectionService? {
        return null
    }
}
