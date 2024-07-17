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

package androidx.privacysandbox.ads.adservices.signals

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import kotlinx.coroutines.suspendCancellableCoroutine

@ExperimentalFeatures.Ext12OptIn
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi", "ClassVerificationFailure")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 12)
open class ProtectedSignalsManagerImpl(
    private val protectedSignalsManager: android.adservices.signals.ProtectedSignalsManager
) : ProtectedSignalsManager() {
    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_PROTECTED_SIGNALS)
    override suspend fun updateSignals(request: UpdateSignalsRequest) {
        suspendCancellableCoroutine { continuation ->
            protectedSignalsManager.updateSignals(
                convertUpdateRequest(request),
                Runnable::run,
                continuation.asOutcomeReceiver()
            )
        }
    }

    private fun convertUpdateRequest(
        request: UpdateSignalsRequest
    ): android.adservices.signals.UpdateSignalsRequest {
        return android.adservices.signals.UpdateSignalsRequest.Builder(request.updateUri).build()
    }
}
