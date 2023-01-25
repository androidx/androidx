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
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import kotlinx.coroutines.suspendCancellableCoroutine

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

    @SuppressLint("NewApi", "ClassVerificationFailure")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    private class Api33Ext4Impl(
        private val mTopicsManager: android.adservices.topics.TopicsManager
        ) : TopicsManager() {
        constructor(context: Context) : this(
            context.getSystemService<android.adservices.topics.TopicsManager>(
                android.adservices.topics.TopicsManager::class.java
            )
        )

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_TOPICS)
        override suspend fun getTopics(request: GetTopicsRequest): GetTopicsResponse {
            return convertResponse(getTopicsAsyncInternal(convertRequest(request)))
        }

        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_TOPICS)
        private suspend fun getTopicsAsyncInternal(
            getTopicsRequest: android.adservices.topics.GetTopicsRequest
        ): android.adservices.topics.GetTopicsResponse = suspendCancellableCoroutine { continuation
            ->
            mTopicsManager.getTopics(
                getTopicsRequest,
                Runnable::run,
                continuation.asOutcomeReceiver()
            )
        }

        private fun convertRequest(
            request: GetTopicsRequest
        ): android.adservices.topics.GetTopicsRequest {
            if (!request.shouldRecordObservation) {
                throw IllegalArgumentException("shouldRecordObservation not supported yet.")
            }
            return android.adservices.topics.GetTopicsRequest.Builder()
                .setAdsSdkName(request.adsSdkName)
                .build()
        }

        internal fun convertResponse(
            response: android.adservices.topics.GetTopicsResponse
        ): GetTopicsResponse {
            var topics = mutableListOf<Topic>()
            for (topic in response.topics) {
                topics.add(Topic(topic.taxonomyVersion, topic.modelVersion, topic.topicId))
            }
            return GetTopicsResponse(topics)
        }
    }

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
            return if (AdServicesInfo.version() >= 4) {
                Api33Ext4Impl(context)
            } else {
                null
            }
        }
    }
}