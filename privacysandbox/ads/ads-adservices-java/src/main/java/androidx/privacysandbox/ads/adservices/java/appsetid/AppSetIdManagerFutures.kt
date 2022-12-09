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

package androidx.privacysandbox.ads.adservices.java.appsetid

import androidx.privacysandbox.ads.adservices.java.internal.asListenableFuture
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.LimitExceededException
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.privacysandbox.ads.adservices.appsetid.AppSetId
import androidx.privacysandbox.ads.adservices.appsetid.AppSetIdManager
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * AppSetIdManager provides APIs for app and ad-SDKs to access appSetId for non-monetizing purpose.
 * This class can be used by Java clients.
 */
abstract class AppSetIdManagerFutures internal constructor() {
    /**
     * Return the AppSetId.
     *
     * @throws SecurityException if caller is not authorized to call this API.
     * @throws IllegalStateException if this API is not available.
     * @throws LimitExceededException if rate limit was reached.
     */
    @DoNotInline
    abstract fun getAppSetIdAsync(): ListenableFuture<AppSetId>

    @SuppressLint("ClassVerificationFailure", "NewApi")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class Api33Ext4JavaImpl(
        private val mAppSetIdManager: AppSetIdManager
    ) : AppSetIdManagerFutures() {
        @DoNotInline
        override fun getAppSetIdAsync(): ListenableFuture<AppSetId> {
            return CoroutineScope(Dispatchers.Main).async {
                mAppSetIdManager.getAppSetId()
            }.asListenableFuture()
        }
    }

    companion object {
        /**
         *  Creates [AppSetIdManagerFutures].
         *
         *  @return AppSetIdManagerFutures object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        @androidx.annotation.OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun from(context: Context): AppSetIdManagerFutures? {
            // TODO: Add check SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 4
            return AppSetIdManager.obtain(context)?.let { Api33Ext4JavaImpl(it) }
        }
    }
}