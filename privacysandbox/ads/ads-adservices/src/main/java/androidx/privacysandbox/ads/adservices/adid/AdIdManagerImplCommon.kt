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

package androidx.privacysandbox.ads.adservices.adid

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.core.os.asOutcomeReceiver
import kotlinx.coroutines.suspendCancellableCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("ClassVerificationFailure", "NewApi")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
open class AdIdManagerImplCommon(
    private val mAdIdManager: android.adservices.adid.AdIdManager
) : AdIdManager() {

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_AD_ID)
    override suspend fun getAdId(): AdId {
        return convertResponse(getAdIdAsyncInternal())
    }

    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_AD_ID)
    private suspend fun
        getAdIdAsyncInternal(): android.adservices.adid.AdId = suspendCancellableCoroutine {
            continuation ->
        mAdIdManager.getAdId(
            Runnable::run,
            continuation.asOutcomeReceiver()
        )
    }

    private fun convertResponse(response: android.adservices.adid.AdId): AdId {
        return AdId(response.adId, response.isLimitAdTrackingEnabled)
    }
}
