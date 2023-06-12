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

package androidx.privacysandbox.ads.adservices.common

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Represents data specific to an ad that is necessary for ad selection and rendering.
 * @param renderUri a URI pointing to the ad's rendering assets
 * @param metadata buyer ad metadata represented as a JSON string
 */
class AdData public constructor(
    val renderUri: Uri = Uri.EMPTY,
    val metadata: String = ""
    ) {

    /** Checks whether two [AdData] objects contain the same information.  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdData) return false
        return this.renderUri == other.renderUri &&
            this.metadata == other.metadata
    }

    /** Returns the hash of the [AdData] object's data.  */
    override fun hashCode(): Int {
        var hash = renderUri.hashCode()
        hash = 31 * hash + metadata.hashCode()
        return hash
    }

    /** Overrides the toString method.  */
    override fun toString(): String {
        return "AdData: renderUri=$renderUri, metadata='$metadata'"
    }

    /** Builder for [AdData] objects. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public class Builder {
        private var renderUri: Uri = Uri.EMPTY
        private var metadata: String = ""

        /**
         * Sets the URI that points to the ad's rendering assets. The URI must use HTTPS.
         *
         * @param renderUri a URI pointing to the ad's rendering assets
         */
        fun setRenderUri(renderUri: Uri): Builder = apply {
            this.renderUri = renderUri
        }

        /**
         * Sets the buyer ad metadata used during the ad selection process.
         *
         * @param metadata The metadata should be a valid JSON object serialized as a string.
         * Metadata represents ad-specific bidding information that will be used during ad selection
         * as part of bid generation and used in buyer JavaScript logic, which is executed in an
         * isolated execution environment.
         *
         * If the metadata is not a valid JSON object that can be consumed by the buyer's JS, the
         * ad will not be eligible for ad selection.
         */
        fun setMetadata(metadata: String): Builder = apply {
            this.metadata = metadata
        }

        /**
         * Builds an instance of [AdData]
         */
        fun build(): AdData {
            return AdData(renderUri, metadata)
        }
    }
}