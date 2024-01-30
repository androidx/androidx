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

package androidx.privacysandbox.ads.adservices.topics

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
@SuppressLint("NewApi")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
open class TopicsManagerImplCommon(
    private val mTopicsManager: android.adservices.topics.TopicsManager
) : TopicsManager() {
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

    internal open fun convertRequest(
        request: GetTopicsRequest
    ): android.adservices.topics.GetTopicsRequest {
        return android.adservices.topics.GetTopicsRequest.Builder()
            .setAdsSdkName(request.adsSdkName)
            .build()
    }

    internal fun convertResponse(
        response: android.adservices.topics.GetTopicsResponse
    ): GetTopicsResponse {
        val topics = mutableListOf<Topic>()
        for (topic in response.topics) {
            topics.add(Topic(topic.taxonomyVersion, topic.modelVersion, topic.topicId))
        }
        return GetTopicsResponse(topics)
    }
}
