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

package androidx.privacysandbox.ads.adservices.java.topics

import android.adservices.common.AdServicesPermissions
import android.content.Context
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.java.internal.asListenableFuture
import androidx.privacysandbox.ads.adservices.topics.GetTopicsRequest
import androidx.privacysandbox.ads.adservices.topics.GetTopicsResponse
import androidx.privacysandbox.ads.adservices.topics.TopicsManager
import androidx.privacysandbox.ads.adservices.topics.TopicsManager.Companion.obtain
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * This provides APIs for App and Ad-Sdks to get the user interest topics in a privacy
 * preserving way. This class can be used by Java clients.
 */
abstract class TopicsManagerFutures internal constructor() {
    /**
     * Returns the topics.
     *
     * @param request The GetTopicsRequest for obtaining Topics.
     * @return ListenableFuture to get the Topics response.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_TOPICS)
    abstract fun getTopicsAsync(request: GetTopicsRequest): ListenableFuture<GetTopicsResponse>

    private class Api33Ext4JavaImpl(
        private val mTopicsManager: TopicsManager
    ) : TopicsManagerFutures() {
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_TOPICS)
        override fun getTopicsAsync(
            request: GetTopicsRequest
        ): ListenableFuture<GetTopicsResponse> {
            return CoroutineScope(Dispatchers.Main).async {
                mTopicsManager.getTopics(request)
            }.asListenableFuture()
        }
    }

    companion object {
        /**
         *  Creates [TopicsManagerFutures].
         *
         *  @return TopicsManagerFutures object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        fun from(context: Context): TopicsManagerFutures? {
            return obtain(context)?.let { Api33Ext4JavaImpl(it) }
        }
    }
}
