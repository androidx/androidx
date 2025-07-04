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

package androidx.privacysandbox.ads.adservices.customaudience

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.content.Context
import android.os.LimitExceededException
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.privacysandbox.ads.adservices.internal.BackCompatManager

/** This class provides APIs for app and ad-SDKs to join / leave custom audiences. */
public abstract class CustomAudienceManager internal constructor() {
    /**
     * Adds the user to the given [CustomAudience].
     *
     * An attempt to register the user for a custom audience with the same combination of {@code
     * ownerPackageName}, {@code buyer}, and {@code name} will cause the existing custom audience's
     * information to be overwritten, including the list of ads data.
     *
     * Note that the ads list can be completely overwritten by the daily background fetch job.
     *
     * This call fails with an [SecurityException] if
     * <ol>
     * <li>the {@code ownerPackageName} is not calling app's package name and/or
     * <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * This call fails with an [IllegalArgumentException] if
     * <ol>
     * <li>the storage limit has been exceeded by the calling application and/or
     * <li>any URI parameters in the [CustomAudience] given are not authenticated with the
     *   [CustomAudience] buyer.
     * </ol>
     *
     * This call fails with [LimitExceededException] if the calling package exceeds the allowed rate
     * limits and is throttled.
     *
     * This call fails with an [IllegalStateException] if an internal service error is encountered.
     *
     * @param request The request to join custom audience.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public abstract suspend fun joinCustomAudience(request: JoinCustomAudienceRequest)

    /**
     * Adds the user to the [CustomAudience] fetched from a {@code fetchUri}.
     *
     * An attempt to register the user for a custom audience with the same combination of {@code
     * ownerPackageName}, {@code buyer}, and {@code name} will cause the existing custom audience's
     * information to be overwritten, including the list of ads data.
     *
     * Note that the ads list can be completely overwritten by the daily background fetch job.
     *
     * This call fails with an [SecurityException] if
     * <ol>
     * <li>the {@code ownerPackageName} is not calling app's package name and/or
     * <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * This call fails with an [IllegalArgumentException] if
     * <ol>
     * <li>the storage limit has been exceeded by the calling application and/or
     * <li>any URI parameters in the [CustomAudience] given are not authenticated with the
     *   [CustomAudience] buyer.
     * </ol>
     *
     * This call fails with [LimitExceededException] if the calling package exceeds the allowed rate
     * limits and is throttled.
     *
     * This call fails with an [IllegalStateException] if an internal service error is encountered.
     *
     * This call fails with an [UnsupportedOperationException] if the Android API level and
     * AdServices module versions don't support this API.
     *
     * @param request The request to fetch and join custom audience.
     */
    @ExperimentalFeatures.Ext10OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public abstract suspend fun fetchAndJoinCustomAudience(
        request: FetchAndJoinCustomAudienceRequest
    )

    /**
     * Allows the API caller to schedule a deferred Custom Audience update. For each update the user
     * will be able to join/leave a set of Custom Audiences or schedule a deferred Custom Audience
     * update.
     *
     * The API only guarantees a minimum delay before initiating the update. It does not guarantee a
     * maximum deadline for the update, nor does it guarantee that the update will occur at all, as
     * requests older than one day are removed. Scheduled updates could be batched and queued
     * together to preserve system resources, thus exact delay time is not guaranteed.
     *
     * In order to conserve system resources the API will make an update request only if the
     * following constraints are satisfied:
     * - The device battery is not low
     * - The device storage is not low
     *
     * When the deferred update is triggered, the API makes a POST request to the provided
     * `updateUri`. The request body contains a JSON representation of the Partial Custom Audience
     * list if provided by the caller.
     *
     * An example of request body when the API calls the `updateUri` containing list of Partial
     * Custom Audiences would look like:
     *
     *     {
     *         "partial_custom_audience_data": [
     *             {
     *                 "name": "running_shoes",
     *                 "activation_time": 1644375856883,
     *                 "expiration_time": 1644375908397
     *             },
     *             {
     *                 "name": "casual_shirt",
     *                 "user_bidding_signals": {"signal1": "value"}
     *             }
     *         ]
     *     }
     *
     * In response, the API expects a JSON in response with either join/leave or schedule:
     * - `join` and/or `leave` :
     *     - `join` : Should contain list containing full data for a [CustomAudience] object
     *     - `leave` : List of [CustomAudience] names that user is intended to be removed from
     *
     * An example of JSON in response with `join/leave` would look like:
     *
     *     {
     *         "join": [
     *             {
     *                 "name": "running-shoes",
     *                 "activation_time": 1644375856883,
     *                 "expiration_time": 1644375908397,
     *                 "user_bidding_signals": {"signal1": "value"},
     *                 "trusted_bidding_data": {
     *                     "trusted_bidding_uri": "https://example-dsp.com/...",
     *                     "trusted_bidding_keys": ["k1", "k2"]
     *                 },
     *                 "bidding_logic_uri": "https://example-dsp.com/...",
     *                 "ads": [
     *                     {
     *                         "render_uri": "https://example-dsp.com/...",
     *                         "metadata": {},
     *                         "ad_filters": {
     *                             "frequency_cap": {
     *                                 "win": [
     *                                     {
     *                                         "ad_counter_key": "key1",
     *                                         "max_count": 2,
     *                                         "interval_in_seconds": 60
     *                                     }
     *                                 ],
     *                                 "view": [
     *                                     {
     *                                         "ad_counter_key": "key2",
     *                                         "max_count": 10,
     *                                         "interval_in_seconds": 3600
     *                                     }
     *                                 ]
     *                             },
     *                             "app_install": {
     *                                 "package_names": ["package.name.one"]
     *                             }
     *                         }
     *                     }
     *                 ]
     *             }
     *         ],
     *         "leave": ["tennis_shoes", "formal_shirt"]
     *     }
     * - `schedule` : List of deferred Custom Audience update requests that the MMP intends to
     *   schedule for one or more DSPs
     *
     * An example of JSON in response with `schedule` would look like:
     *
     *     {
     *       "schedule": {
     *         "requests": [
     *           {
     *             "update_uri": "https://example-dsp.com/...",
     *             "min_delay": "30",
     *             "should_replace_pending_updates": true,
     *             "partial_custom_audience_data": [
     *               {
     *                 "name": "running-shoes",
     *                 "activation_time": 1644375856883,
     *                 "expiration_time": 1644375908397
     *               },
     *               {
     *                 "name": "casual_shirt",
     *                 "user_bidding_signals": {
     *                   "signal1": "value"
     *                 }
     *               }
     *             ],
     *             "leave": [
     *               "tennis_shoes",
     *               "formal_shirt"
     *             ]
     *           }
     *         ]
     *       }
     *     }
     *
     * An example of request body when the API calls the `updateUri` provided by the above schedule
     * request would look like:
     *
     *     {
     *         "partial_custom_audience_data": [
     *             {
     *                 "name": "running_shoes",
     *                 "activation_time": 1644375856883,
     *                 "expiration_time": 1644375908397
     *             },
     *             {
     *                 "name": "casual_shirt",
     *                 "user_bidding_signals": {"signal1": "value"}
     *             }
     *         ],
     *         "leave": ["tennis_shoes", "formal_shirt"]
     *     }
     *
     * An example of the JSON response from calling the `updateUri` would look the same as the JSON
     * response containing `join` and `leave` keys above.
     *
     * An attempt to register a user for a custom audience with the same `owner` (calling app),
     * `buyer` (inferred from `updateUri`) and `name` combination, the existing custom audience's
     * information will be overwritten, including the list of ads data.
     *
     * If information related to any [CustomAudience] to be joined is malformed, the deferred update
     * will silently ignore that [CustomAudience]. Similarly, if a scheduled request is malformed,
     * the scheduled update will silently ignore it.
     *
     * When removing a user from a [CustomAudience], this API deletes the existing [CustomAudience]
     * identified by the `owner` (calling app), `buyer` (inferred from the `updateUri`), and `name`.
     *
     * Any partial custom audience field set by the caller cannot be overridden by the
     * [CustomAudience] fetched from the `updateUri`. Given that multiple Custom Audiences could be
     * returned by a DSP, we will match the override restriction based on the names of the Custom
     * Audiences. A DSP may skip returning a full Custom Audience for any Partial Custom Audience in
     * the request.
     *
     * In case the API encounters transient errors while making the network call for update, like
     * 5xx, connection timeout, rate limit exceeded, it would defer the update to the next job.
     *
     * This call fails with an [SecurityException] if
     * 1. the `ownerPackageName` is not calling app's package name; and/or
     * 2. the buyer, inferred from `updateUri`, is not authorized to use the API.
     *
     * This call fails with an [IllegalArgumentException] if
     * 1. the provided `updateUri` is invalid or malformed.
     * 2. the provided `delayTime` is not within permissible bounds
     * 3. the combined size of [PartialCustomAudience] list is larger than allowed limits
     *
     * This call fails with [LimitExceededException] if the calling package exceeds the allowed rate
     * limits and is throttled.
     */
    @ExperimentalFeatures.Ext14OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public abstract suspend fun scheduleCustomAudienceUpdate(
        request: ScheduleCustomAudienceUpdateRequest
    )

    /**
     * Attempts to remove a user from a custom audience by deleting any existing [CustomAudience]
     * data, identified by {@code ownerPackageName}, {@code buyer}, and {@code name}.
     *
     * This call fails with an [SecurityException] if
     * <ol>
     * <li>the {@code ownerPackageName} is not calling app's package name; and/or
     * <li>the buyer is not authorized to use the API.
     * </ol>
     *
     * This call fails with [LimitExceededException] if the calling package exceeds the allowed rate
     * limits and is throttled.
     *
     * This call does not inform the caller whether the custom audience specified existed in
     * on-device storage. In other words, it will fail silently when a buyer attempts to leave a
     * custom audience that was not joined.
     *
     * @param request The request to leave custom audience.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    public abstract suspend fun leaveCustomAudience(request: LeaveCustomAudienceRequest)

    public companion object {
        /**
         * Creates [CustomAudienceManager].
         *
         * @return CustomAudienceManager object. If the device is running an incompatible build, the
         *   value returned is null.
         */
        @JvmStatic
        @SuppressLint("NewApi")
        public fun obtain(context: Context): CustomAudienceManager? {
            return if (AdServicesInfo.adServicesVersion() >= 4) {
                CustomAudienceManagerApi33Ext4Impl(context)
            } else if (AdServicesInfo.extServicesVersionS() >= 9) {
                BackCompatManager.getManager(context, "CustomAudienceManager") {
                    CustomAudienceManagerApi31Ext9Impl(context)
                }
            } else {
                null
            }
        }
    }
}
