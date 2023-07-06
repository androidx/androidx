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

package androidx.privacysandbox.ads.adservices.appsetid

import android.annotation.SuppressLint
import android.content.Context
import android.os.LimitExceededException
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * AppSetIdManager provides APIs for app and ad-SDKs to access appSetId for non-monetizing purpose.
 */
abstract class AppSetIdManager internal constructor() {
    /**
     * Retrieve the AppSetId.
     *
     * @throws [SecurityException] if caller is not authorized to call this API.
     * @throws [IllegalStateException] if this API is not available.
     * @throws [LimitExceededException] if rate limit was reached.
     */
    abstract suspend fun getAppSetId(): AppSetId

    @SuppressLint("ClassVerificationFailure", "NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    private class Api33Ext4Impl(
        private val mAppSetIdManager: android.adservices.appsetid.AppSetIdManager
    ) : AppSetIdManager() {
        constructor(context: Context) : this(
            context.getSystemService<android.adservices.appsetid.AppSetIdManager>(
                android.adservices.appsetid.AppSetIdManager::class.java
            )
        )

        @DoNotInline
        override suspend fun getAppSetId(): AppSetId {
            return convertResponse(getAppSetIdAsyncInternal())
        }

        private suspend fun getAppSetIdAsyncInternal(): android.adservices.appsetid.AppSetId =
            suspendCancellableCoroutine {
                    continuation ->
                mAppSetIdManager.getAppSetId(
                    Runnable::run,
                    continuation.asOutcomeReceiver()
                )
            }

        private fun convertResponse(response: android.adservices.appsetid.AppSetId): AppSetId {
            if (response.scope == android.adservices.appsetid.AppSetId.SCOPE_APP) {
                return AppSetId(response.id, AppSetId.SCOPE_APP)
            }
            return AppSetId(response.id, AppSetId.SCOPE_DEVELOPER)
        }
    }

    companion object {

        /**
         *  Creates [AppSetIdManager].
         *
         *  @return AppSetIdManager object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun obtain(context: Context): AppSetIdManager? {
            return if (AdServicesInfo.version() >= 4) {
                Api33Ext4Impl(context)
            } else {
                // TODO(b/261770989): Extend this to older versions.
                null
            }
        }
    }
}
