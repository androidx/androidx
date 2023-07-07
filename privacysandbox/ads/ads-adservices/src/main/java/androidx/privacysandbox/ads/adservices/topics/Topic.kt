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

/**
 * Represent the topic result from the getTopics API.
 *
 * @param taxonomyVersion the version of the taxonomy.
 * @param modelVersion the version of the model.
 * @param topicId the unique id of a topic.
 * See https://developer.android.com/design-for-safety/privacy-sandbox/guides/topics for details.
 */
class Topic public constructor(
    val taxonomyVersion: Long,
    val modelVersion: Long,
    val topicId: Int
) {
    override fun toString(): String {
        val taxonomyVersionString = "TaxonomyVersion=$taxonomyVersion" +
            ", ModelVersion=$modelVersion" +
            ", TopicCode=$topicId }"
        return "Topic { $taxonomyVersionString"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Topic) return false
        return this.taxonomyVersion == other.taxonomyVersion &&
            this.modelVersion == other.modelVersion &&
            this.topicId == other.topicId
    }

    override fun hashCode(): Int {
        var hash = taxonomyVersion.hashCode()
        hash = 31 * hash + modelVersion.hashCode()
        hash = 31 * hash + topicId.hashCode()
        return hash
    }
}
