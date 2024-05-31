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

package androidx.privacysandbox.ads.adservices.adid

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.internal.asAdServicesOutcomeReceiver
import kotlinx.coroutines.suspendCancellableCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("ClassVerificationFailure", "NewApi")
@RequiresExtension(extension = Build.VERSION_CODES.R, version = 11)
open class AdIdManagerApi30Ext11Impl(context: Context) : AdIdManager() {
    private val mAdIdManager: android.adservices.adid.AdIdManager =
        android.adservices.adid.AdIdManager.get(context)

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_AD_ID)
    override suspend fun getAdId(): AdId {
        return convertResponse(getAdIdAsyncInternal())
    }

    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_AD_ID)
    private suspend fun getAdIdAsyncInternal(): android.adservices.adid.AdId =
        suspendCancellableCoroutine { continuation ->
            mAdIdManager.getAdId(Runnable::run, continuation.asAdServicesOutcomeReceiver())
        }

    private fun convertResponse(response: android.adservices.adid.AdId): AdId {
        return AdId(response.adId, response.isLimitAdTrackingEnabled)
    }
}
