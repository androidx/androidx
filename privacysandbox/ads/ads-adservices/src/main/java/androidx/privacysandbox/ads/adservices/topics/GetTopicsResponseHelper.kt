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

package androidx.privacysandbox.ads.adservices.topics

import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures

/** Helper class to consolidate conversion logic for GetTopicsResponse. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("ClassVerificationFailure")
object GetTopicsResponseHelper {
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertResponse(
        response: android.adservices.topics.GetTopicsResponse,
    ): GetTopicsResponse {
        val topics = mutableListOf<Topic>()
        for (topic in response.topics) {
            topics.add(Topic(topic.taxonomyVersion, topic.modelVersion, topic.topicId))
        }
        return GetTopicsResponse(topics)
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 11)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 11)
    @ExperimentalFeatures.Ext11OptIn
    internal fun convertResponseWithEncryptedTopics(
        response: android.adservices.topics.GetTopicsResponse,
    ): GetTopicsResponse {
        val topics = mutableListOf<Topic>()
        for (topic in response.topics) {
            topics.add(Topic(topic.taxonomyVersion, topic.modelVersion, topic.topicId))
        }
        val encryptedTopics = mutableListOf<EncryptedTopic>()
        for (encryptedTopic in response.encryptedTopics) {
            encryptedTopics.add(
                EncryptedTopic(
                    encryptedTopic.encryptedTopic,
                    encryptedTopic.keyIdentifier,
                    encryptedTopic.encapsulatedKey,
                ),
            )
        }
        return GetTopicsResponse(topics, encryptedTopics)
    }
}
