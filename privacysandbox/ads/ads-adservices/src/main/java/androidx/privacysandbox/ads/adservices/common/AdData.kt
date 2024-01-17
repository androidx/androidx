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

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * Represents data specific to an ad that is necessary for ad selection and rendering.
 *
 * @param renderUri a URI pointing to the ad's rendering assets
 * @param metadata buyer ad metadata represented as a JSON string
 * @param adCounterKeys the set of keys used in counting events
 * @param adFilters all [AdFilters] associated with the ad
 * @param adRenderId ad render id for server auctions
 */
@OptIn(ExperimentalFeatures.Ext8OptIn::class, ExperimentalFeatures.Ext10OptIn::class)
@SuppressLint("ClassVerificationFailure")
class AdData
@ExperimentalFeatures.Ext10OptIn
public constructor(
    val renderUri: Uri,
    val metadata: String,
    val adCounterKeys: Set<Int> = setOf(),
    val adFilters: AdFilters? = null,
    val adRenderId: String? = null
) {
    /**
     * Represents data specific to an ad that is necessary for ad selection and rendering.
     *
     * @param renderUri a URI pointing to the ad's rendering assets
     * @param metadata buyer ad metadata represented as a JSON string
     * @param adCounterKeys the set of keys used in counting events
     * @param adFilters all [AdFilters] associated with the ad
     */
    @ExperimentalFeatures.Ext8OptIn
    constructor(
        renderUri: Uri,
        metadata: String,
        adCounterKeys: Set<Int> = setOf(),
        adFilters: AdFilters? = null
    ) : this(renderUri, metadata, adCounterKeys, adFilters, null)

    /**
     * Represents data specific to an ad that is necessary for ad selection and rendering.
     *
     * @param renderUri a URI pointing to the ad's rendering assets
     * @param metadata buyer ad metadata represented as a JSON string
     */
    constructor(
        renderUri: Uri,
        metadata: String,
    ) : this(renderUri, metadata, setOf(), null)

    /** Checks whether two [AdData] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdData) return false
        return renderUri == other.renderUri &&
            metadata == other.metadata &&
            adCounterKeys == other.adCounterKeys &&
            adFilters == other.adFilters &&
            adRenderId == other.adRenderId
    }

    /** Returns the hash of the [AdData] object's data. */
    override fun hashCode(): Int {
        var hash = renderUri.hashCode()
        hash = 31 * hash + metadata.hashCode()
        hash = 31 * hash + adCounterKeys.hashCode()
        hash = 31 * hash + adFilters.hashCode()
        hash = 31 * hash + adRenderId.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "AdData: renderUri=$renderUri, metadata='$metadata', " +
            "adCounterKeys=$adCounterKeys, adFilters=$adFilters, adRenderId=$adRenderId"
    }

    @SuppressLint("NewApi")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun convertToAdServices(): android.adservices.common.AdData {
        if (AdServicesInfo.adServicesVersion() >= 10 || AdServicesInfo.extServicesVersion() >= 10) {
            return Ext10Impl.convertAdData(this)
        } else if (AdServicesInfo.adServicesVersion() >= 8 ||
                AdServicesInfo.extServicesVersion() >= 9) {
            return Ext8Impl.convertAdData(this)
        }
        return Ext4Impl.convertAdData(this)
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    private class Ext10Impl private constructor() {
        companion object {
            fun convertAdData(adData: AdData): android.adservices.common.AdData {
                return android.adservices.common.AdData.Builder()
                    .setMetadata(adData.metadata)
                    .setRenderUri(adData.renderUri)
                    .setAdCounterKeys(adData.adCounterKeys)
                    .setAdFilters(adData.adFilters?.convertToAdServices())
                    .setAdRenderId(adData.adRenderId)
                    .build()
            }
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private class Ext8Impl private constructor() {
        companion object {
            fun convertAdData(adData: AdData): android.adservices.common.AdData {
                adData.adRenderId?.let { Log.w("AdData",
                    "adRenderId is ignored. Min version to use adRenderId is " +
                        "API 31 ext 10") }
                return android.adservices.common.AdData.Builder()
                    .setMetadata(adData.metadata)
                    .setRenderUri(adData.renderUri)
                    .setAdCounterKeys(adData.adCounterKeys)
                    .setAdFilters(adData.adFilters?.convertToAdServices())
                    .build()
            }
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private class Ext4Impl private constructor() {
        companion object {
            fun convertAdData(adData: AdData): android.adservices.common.AdData {
                if (adData.adCounterKeys.isNotEmpty()) { Log.w("AdData",
                    "adCounterKeys is ignored. Min version to use adCounterKeys is " +
                        "API 33 ext 8 or API 31/32 ext 9") }
                adData.adFilters?.let { Log.w("AdData",
                    "adFilters is ignored. Min version to use adFilters is " +
                        "API 33 ext 8 or API 31/32 ext 9") }
                adData.adRenderId?.let { Log.w("AdData",
                    "adRenderId is ignored. Min version to use adRenderId is " +
                        "API 31 ext 10") }
                return android.adservices.common.AdData.Builder()
                    .setMetadata(adData.metadata)
                    .setRenderUri(adData.renderUri)
                    .build()
            }
        }
    }
}
