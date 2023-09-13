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

package androidx.privacysandbox.ads.adservices.topics

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.content.Context
import android.os.LimitExceededException
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * TopicsManager provides APIs for App and Ad-Sdks to get the user interest topics in a privacy
 * preserving way.
 */
abstract class TopicsManager internal constructor() {
    /**
     * Return the topics.
     *
     * @param request The GetTopicsRequest for obtaining Topics.
     * @throws SecurityException if caller is not authorized to call this API.
     * @throws IllegalStateException if this API is not available.
     * @throws LimitExceededException if rate limit was reached.
     * @return GetTopicsResponse
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_TOPICS)
    abstract suspend fun getTopics(request: GetTopicsRequest): GetTopicsResponse

    companion object {
        /**
         *  Creates [TopicsManager].
         *
         *  @return TopicsManagerCompat object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun obtain(context: Context): TopicsManager? {
            return if (AdServicesInfo.adServicesVersion() >= 5) {
                TopicsManagerApi33Ext5Impl(context)
            } else if (AdServicesInfo.adServicesVersion() == 4) {
                TopicsManagerApi33Ext4Impl(context)
            } else if (AdServicesInfo.extServicesVersion() >= 9) {
                TopicsManagerApi31Ext9Impl(context)
            } else {
                null
            }
        }
    }
}
