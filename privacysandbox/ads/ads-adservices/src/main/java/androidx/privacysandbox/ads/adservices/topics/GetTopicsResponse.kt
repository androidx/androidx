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

import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import java.util.Objects

/** Represent the result from the getTopics API. */
@OptIn(ExperimentalFeatures.Ext11OptIn::class)
class GetTopicsResponse
@ExperimentalFeatures.Ext11OptIn
constructor(
    val topics: List<Topic>,
    val encryptedTopics: List<EncryptedTopic>,
) {
    constructor(topics: List<Topic>) : this(topics, listOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetTopicsResponse) return false
        if (topics.size != other.topics.size || encryptedTopics.size != other.encryptedTopics.size)
            return false
        return HashSet(this.topics) == HashSet(other.topics) &&
            HashSet(this.encryptedTopics) == HashSet(other.encryptedTopics)
    }

    override fun hashCode(): Int {
        return Objects.hash(topics, encryptedTopics)
    }

    override fun toString(): String {
        return "GetTopicsResponse: Topics=$topics, EncryptedTopics=$encryptedTopics"
    }
}
