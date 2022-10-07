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
 * Represents the request for the getTopics API.
 * @param sdkName The Ads SDK name. This must be called by SDKs running outside of the Sandbox.
 * Other clients must not call it.
 * @param shouldRecordObservation whether to record that the caller has observed the topics of the
 *     host app or not. This will be used to determine if the caller can receive the topic
 *     in the next epoch.
 */
class GetTopicsRequest public constructor(
    val sdkName: String,
    val shouldRecordObservation: Boolean = false
) {
    override fun toString(): String {
        return "GetTopicsRequest: " +
            "sdkName=$sdkName, shouldRecordObservation=$shouldRecordObservation"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetTopicsRequest) return false
        return this.sdkName == other.sdkName &&
            this.shouldRecordObservation == other.shouldRecordObservation
    }

    override fun hashCode(): Int {
        var hash = sdkName.hashCode()
        hash = 31 * hash + shouldRecordObservation.hashCode()
        return hash
    }

    /**
     * Builder for {@link GetTopicsRequest}.
     */
    public class Builder(
        private var sdkName: String = "",
        private var shouldRecordObservation: Boolean = true
    ) {

        /**
         * Set Ads Sdk Name.
         *
         * <p>This must be called by SDKs running outside of the Sandbox. Other clients must not
         * call it.
         *
         * @param sdkName the Ads Sdk Name.
         */
        fun setSdkName(sdkName: String): Builder = apply { this.sdkName = sdkName }

        /**
         * Set the Record Observation.
         *
         * @param shouldRecordObservation whether to record that the caller has observed the topics of the
         *     host app or not. This will be used to determine if the caller can receive the topic
         *     in the next epoch.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setShouldRecordObservation(shouldRecordObservation: Boolean): Builder = apply {
            this.shouldRecordObservation = shouldRecordObservation
        }

        /** Builds a {@link GetTopicsRequest} instance. */
        fun build(): GetTopicsRequest {
            check(sdkName.isNotEmpty()) { "sdkName must be set" }
            return GetTopicsRequest(sdkName, shouldRecordObservation)
        }
    }
}