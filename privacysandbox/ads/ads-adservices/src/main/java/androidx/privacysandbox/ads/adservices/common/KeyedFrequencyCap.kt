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
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import java.time.Duration

/**
 * A frequency cap for a specific ad counter key.
 *
 * Frequency caps define the maximum rate an event can occur within a given time interval. If the
 * frequency cap is exceeded, the associated ad will be filtered out of ad selection.
 *
 * @param adCounterKey The ad counter key that the frequency cap is applied to.
 * @param maxCount A render URL for the winning ad.
 * @param interval The caller adtech entity's [AdTechIdentifier].
 */
@ExperimentalFeatures.Ext8OptIn
class KeyedFrequencyCap public constructor(
    val adCounterKey: Int,
    val maxCount: Int,
    val interval: Duration
) {
    /** Checks whether two [KeyedFrequencyCap] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyedFrequencyCap) return false
        return this.adCounterKey == other.adCounterKey &&
            this.maxCount == other.maxCount &&
            this.interval == other.interval
    }

    /** Returns the hash of the [KeyedFrequencyCap] object's data. */
    override fun hashCode(): Int {
        var hash = adCounterKey.hashCode()
        hash = 31 * hash + maxCount.hashCode()
        hash = 31 * hash + interval.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "KeyedFrequencyCap: adCounterKey=$adCounterKey, maxCount=$maxCount, " +
            "interval=$interval"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.common.KeyedFrequencyCap {
        return android.adservices.common.KeyedFrequencyCap.Builder(
            adCounterKey,
            maxCount,
            interval)
            .build()
    }
}
