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

package androidx.privacysandbox.ads.adservices.java.customaudience

import android.adservices.common.AdServicesPermissions
import android.content.Context
import android.os.LimitExceededException
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudienceManager
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudienceManager.Companion.obtain
import androidx.privacysandbox.ads.adservices.customaudience.JoinCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.LeaveCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.java.internal.asListenableFuture
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * This class provides APIs for app and ad-SDKs to join / leave custom audiences.
 * This class can be used by Java clients.
 */
abstract class CustomAudienceManagerFutures internal constructor() {

    /**
     * Adds the user to the given [CustomAudience].
     *
     * An attempt to register the user for a custom audience with the same combination of {@code
     * ownerPackageName}, {@code buyer}, and {@code name} will cause the existing custom audience's
     * information to be overwritten, including the list of ads data.
     *
     * Note that the ads list can be completely overwritten by the daily background fetch job.
     *
     * This call fails with an [SecurityException] if
     *
     * <ol>
     *   <li>the {@code ownerPackageName} is not calling app's package name and/or
     *   <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * This call fails with an [IllegalArgumentException] if
     *
     * <ol>
     *   <li>the storage limit has been exceeded by the calling application and/or
     *   <li>any URI parameters in the [CustomAudience] given are not authenticated with the
     *       [CustomAudience] buyer.
     * </ol>
     *
     * This call fails with [LimitExceededException] if the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * This call fails with an [IllegalStateException] if an internal service error is
     * encountered.
     *
     * @param request The request to join custom audience.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun joinCustomAudienceAsync(
        request: JoinCustomAudienceRequest
    ): ListenableFuture<Unit>

    /**
     * Attempts to remove a user from a custom audience by deleting any existing
     * [CustomAudience] data, identified by {@code ownerPackageName}, {@code buyer}, and {@code
     * name}.
     *
     * This call fails with an [SecurityException] if
     *
     * <ol>
     *   <li>the {@code ownerPackageName} is not calling app's package name; and/or
     *   <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * This call fails with [LimitExceededException] if the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * This call does not inform the caller whether the custom audience specified existed in
     * on-device storage. In other words, it will fail silently when a buyer attempts to leave a
     * custom audience that was not joined.
     *
     * @param request The request to leave custom audience.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun leaveCustomAudienceAsync(
        request: LeaveCustomAudienceRequest
    ): ListenableFuture<Unit>

    private class Api33Ext4JavaImpl(
        private val mCustomAudienceManager: CustomAudienceManager?
    ) : CustomAudienceManagerFutures() {
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun joinCustomAudienceAsync(
            request: JoinCustomAudienceRequest
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Default).async {
                mCustomAudienceManager!!.joinCustomAudience(request)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun leaveCustomAudienceAsync(
            request: LeaveCustomAudienceRequest
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Default).async {
                mCustomAudienceManager!!.leaveCustomAudience(request)
            }.asListenableFuture()
        }
    }

    companion object {
        /**
         *  Creates [CustomAudienceManagerFutures].
         *
         *  @return CustomAudienceManagerFutures object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        fun from(context: Context): CustomAudienceManagerFutures? {
            return obtain(context)?.let { Api33Ext4JavaImpl(it) }
        }
    }
}
