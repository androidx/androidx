/*
 * Copyright 2025 The Android Open Source Project
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
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

/**
 * Represents data specific to a component ad that is necessary for ad selection and rendering. This
 * is to support use case for ads composed of multiple pieces, such as an ad displaying multiple
 * products at once.
 *
 * @param renderUri a URI pointing to the component ads' rendering assets
 * @param adRenderId ad render id for server auctions
 */
@ExperimentalFeatures.Ext16OptIn
public class ComponentAdData
public constructor(public val renderUri: Uri, public val adRenderId: String) {

    /** Checks whether two [ComponentAdData] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentAdData) return false
        return renderUri == other.renderUri && adRenderId == other.adRenderId
    }

    /** Returns the hash of the [ComponentAdData] object's data. */
    override fun hashCode(): Int {
        var hash = renderUri.hashCode()
        hash = 31 * hash + adRenderId.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "ComponentAdData: renderUri=$renderUri, adRenderId='$adRenderId'"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 16)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 16)
    internal fun convertToAdServices(): android.adservices.common.ComponentAdData {
        return android.adservices.common.ComponentAdData(renderUri, adRenderId)
    }
}
