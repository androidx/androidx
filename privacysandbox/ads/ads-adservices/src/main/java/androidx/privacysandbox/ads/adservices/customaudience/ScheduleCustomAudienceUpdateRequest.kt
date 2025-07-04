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

package androidx.privacysandbox.ads.adservices.customaudience

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import java.time.Duration

/**
 * The request object wrapping the required and optional parameters to schedule a deferred update
 * for a buyer ad tech's custom audiences.
 *
 * @param updateUri The [Uri] from which the update for the buyer's custom audiences will be
 *   fetched.
 * @param minDelay The [Duration] representing the minimum of the delay before the service fetches
 *   updates for the buyer ad tech's custom audiences.
 * @param partialCustomAudienceList The list of [PartialCustomAudience] objects which are sent along
 *   with the request to download the updates for the buyer ad tech's custom audiences.
 * @param shouldReplacePendingUpdates If any pending scheduled updates should be canceled and
 *   replaced with the update detailed in the current [ScheduleCustomAudienceUpdateRequest]. If it
 *   is false and there are previously requested updates still pending for the same buyer in the
 *   same app, a call to [CustomAudienceManager.scheduleCustomAudienceUpdate] with this
 *   [ScheduleCustomAudienceUpdateRequest] will fail.
 */
@ExperimentalFeatures.Ext14OptIn
public class ScheduleCustomAudienceUpdateRequest
@JvmOverloads
constructor(
    public val updateUri: Uri,
    public val minDelay: Duration,
    public val partialCustomAudienceList: List<PartialCustomAudience>,
    @get:JvmName("shouldReplacePendingUpdates")
    public val shouldReplacePendingUpdates: Boolean = false,
) {
    /**
     * Checks whether two ScheduleCustomAudienceUpdateRequest objects contain the same information.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScheduleCustomAudienceUpdateRequest) return false
        return this.updateUri == other.updateUri &&
            this.minDelay == other.minDelay &&
            this.partialCustomAudienceList == other.partialCustomAudienceList &&
            this.shouldReplacePendingUpdates == other.shouldReplacePendingUpdates
    }

    /** Returns the hash of the ScheduleCustomAudienceUpdateRequest object's data. */
    override fun hashCode(): Int {
        var hash = updateUri.hashCode()
        hash = 31 * hash + minDelay.hashCode()
        hash = 31 * hash + partialCustomAudienceList.hashCode()
        hash = 31 * hash + shouldReplacePendingUpdates.hashCode()
        return hash
    }

    override fun toString(): String {
        return "ScheduleCustomAudienceUpdateRequest: updateUri=$updateUri, " +
            "minDelay=$minDelay, partialCustomAudienceList=$partialCustomAudienceList, " +
            "shouldReplacePendingUpdates=$shouldReplacePendingUpdates"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
    @Suppress("deprecation") // suppress warning of deprecated Builder
    internal fun convertToAdServices():
        android.adservices.customaudience.ScheduleCustomAudienceUpdateRequest {
        return android.adservices.customaudience.ScheduleCustomAudienceUpdateRequest.Builder(
                updateUri,
                minDelay,
                partialCustomAudienceList.map { it.convertToAdServices() },
            )
            .setShouldReplacePendingUpdates(shouldReplacePendingUpdates)
            .build()
    }
}
