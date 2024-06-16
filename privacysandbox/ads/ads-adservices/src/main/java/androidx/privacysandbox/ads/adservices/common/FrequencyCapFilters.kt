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

package androidx.privacysandbox.ads.adservices.common

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.IntDef
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

/**
 * A container for the ad filters that are based on frequency caps.
 *
 * Frequency caps filters combine an event type with a list of [KeyedFrequencyCap] objects to define
 * a collection of ad filters. If any of these frequency caps are exceeded for a given ad, the ad
 * will be removed from the group of ads submitted to a buyer adtech's bidding function.
 *
 * @param keyedFrequencyCapsForWinEvents The list of frequency caps applied to events which
 *   correlate to a win as interpreted by an adtech.
 * @param keyedFrequencyCapsForImpressionEvents The list of frequency caps applied to events which
 *   correlate to an impression as interpreted by an adtech.
 * @param keyedFrequencyCapsForViewEvents The list of frequency caps applied to events which
 *   correlate to a view as interpreted by an adtech.
 * @param keyedFrequencyCapsForClickEvents The list of frequency caps applied to events which
 *   correlate to a click as interpreted by an adtech.
 */
@ExperimentalFeatures.Ext8OptIn
class FrequencyCapFilters
@JvmOverloads
public constructor(
    val keyedFrequencyCapsForWinEvents: List<KeyedFrequencyCap> = listOf(),
    val keyedFrequencyCapsForImpressionEvents: List<KeyedFrequencyCap> = listOf(),
    val keyedFrequencyCapsForViewEvents: List<KeyedFrequencyCap> = listOf(),
    val keyedFrequencyCapsForClickEvents: List<KeyedFrequencyCap> = listOf()
) {
    /** Checks whether two [FrequencyCapFilters] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrequencyCapFilters) return false
        return this.keyedFrequencyCapsForWinEvents == other.keyedFrequencyCapsForWinEvents &&
            this.keyedFrequencyCapsForImpressionEvents ==
                other.keyedFrequencyCapsForImpressionEvents &&
            this.keyedFrequencyCapsForViewEvents == other.keyedFrequencyCapsForViewEvents &&
            this.keyedFrequencyCapsForClickEvents == other.keyedFrequencyCapsForClickEvents
    }

    /** Returns the hash of the [FrequencyCapFilters] object's data. */
    override fun hashCode(): Int {
        var hash = keyedFrequencyCapsForWinEvents.hashCode()
        hash = 31 * hash + keyedFrequencyCapsForImpressionEvents.hashCode()
        hash = 31 * hash + keyedFrequencyCapsForViewEvents.hashCode()
        hash = 31 * hash + keyedFrequencyCapsForClickEvents.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "FrequencyCapFilters: " +
            "keyedFrequencyCapsForWinEvents=$keyedFrequencyCapsForWinEvents, " +
            "keyedFrequencyCapsForImpressionEvents=$keyedFrequencyCapsForImpressionEvents, " +
            "keyedFrequencyCapsForViewEvents=$keyedFrequencyCapsForViewEvents, " +
            "keyedFrequencyCapsForClickEvents=$keyedFrequencyCapsForClickEvents"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        Companion.AD_EVENT_TYPE_WIN,
        Companion.AD_EVENT_TYPE_VIEW,
        Companion.AD_EVENT_TYPE_CLICK,
        Companion.AD_EVENT_TYPE_IMPRESSION
    )
    annotation class AdEventType

    companion object {
        /**
         * Represents the Win event for ads that were selected as winners in ad selection.
         *
         * The WIN ad event type is automatically populated within the Protected Audience service
         * for any winning ad which is returned from Protected Audience ad selection.
         *
         * It should not be used to manually update an ad counter histogram.
         */
        public const val AD_EVENT_TYPE_WIN: Int =
            android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_WIN

        /**
         * Represents the Impression event type which correlate to an impression as interpreted by
         * an adtech.
         */
        public const val AD_EVENT_TYPE_IMPRESSION: Int =
            android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION

        /** Represents the View event type which correlate to a view as interpreted by an adtech. */
        public const val AD_EVENT_TYPE_VIEW: Int =
            android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_VIEW

        /**
         * Represents the Click event type which correlate to a click as interpreted by an adtech.
         */
        public const val AD_EVENT_TYPE_CLICK: Int =
            android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_CLICK
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.common.FrequencyCapFilters {
        return android.adservices.common.FrequencyCapFilters.Builder()
            .setKeyedFrequencyCapsForWinEvents(keyedFrequencyCapsForWinEvents.convertToAdServices())
            .setKeyedFrequencyCapsForImpressionEvents(
                keyedFrequencyCapsForImpressionEvents.convertToAdServices()
            )
            .setKeyedFrequencyCapsForViewEvents(
                keyedFrequencyCapsForViewEvents.convertToAdServices()
            )
            .setKeyedFrequencyCapsForClickEvents(
                keyedFrequencyCapsForClickEvents.convertToAdServices()
            )
            .build()
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private fun List<KeyedFrequencyCap>.convertToAdServices():
        MutableList<android.adservices.common.KeyedFrequencyCap> {
        val result = mutableListOf<android.adservices.common.KeyedFrequencyCap>()
        for (keyedFrequencyCap in this) {
            result.add(keyedFrequencyCap.convertToAdServices())
        }
        return result
    }
}
