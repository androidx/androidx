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

package androidx.privacysandbox.ads.adservices.adid

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.content.Context
import android.os.LimitExceededException
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.privacysandbox.ads.adservices.internal.BackCompatManager

/**
 * AdId Manager provides APIs for app and ad-SDKs to access advertising ID. The advertising ID is a
 * unique, per-device, user-resettable ID for advertising. It gives users better controls and
 * provides developers with a simple, standard system to continue to monetize their apps via
 * personalized ads (formerly known as interest-based ads).
 */
abstract class AdIdManager internal constructor() {
    /**
     * Return the AdId.
     *
     * @throws SecurityException if caller is not authorized to call this API.
     * @throws IllegalStateException if this API is not available.
     * @throws LimitExceededException if rate limit was reached.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_AD_ID)
    abstract suspend fun getAdId(): AdId

    companion object {
        /**
         * Creates [AdIdManager].
         *
         * @return AdIdManager object. If the device is running an incompatible build, the value
         *   returned is null.
         */
        @JvmStatic
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun obtain(context: Context): AdIdManager? {
            return if (AdServicesInfo.adServicesVersion() >= 4) {
                AdIdManagerApi33Ext4Impl(context)
            } else if (AdServicesInfo.extServicesVersionS() >= 9) {
                BackCompatManager.getManager(context, "AdIdManager") {
                    AdIdManagerApi31Ext9Impl(context)
                }
            } else {
                null
            }
        }
    }
}
