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

/**
 * Represents data specific to an ad that is necessary for ad selection and rendering.
 * @param renderUri a URI pointing to the ad's rendering assets
 * @param metadata buyer ad metadata represented as a JSON string
 */
class AdData public constructor(
    val renderUri: Uri,
    val metadata: String
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
}
