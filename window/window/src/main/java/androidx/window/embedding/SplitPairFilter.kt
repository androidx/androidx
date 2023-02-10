/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window.embedding

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.window.core.ActivityComponentInfo
import androidx.window.embedding.MatcherUtils.isActivityMatching
import androidx.window.embedding.MatcherUtils.isIntentMatching
import androidx.window.embedding.MatcherUtils.sDebugMatchers
import androidx.window.embedding.MatcherUtils.sMatchersTag
import androidx.window.embedding.MatcherUtils.validateComponentName

/**
 * Filter for [SplitPairRule] and used to find if a pair of activities should be put in a split.
 * It is used when a new activity is started from the primary activity.
 * If the filter matches the primary [Activity.getComponentName] and the new started activity
 * [Intent], it matches the [SplitPairRule] that holds this filter.
 *
 * @param primaryActivityName Component name of the primary activity in the split. Must be
 * non-empty. Can contain a single wildcard at the end.
 * Supported formats:
 * - package/class
 * - `package/*`
 * - `package/suffix.*`
 * - `*/*`
 * @param secondaryActivityName Component name of the secondary activity in the split. Must be
 * non-empty. Can contain a single wildcard at the end.
 * Supported formats:
 * - package/class
 * - `package/*`
 * - `package/suffix.*`
 * - `*/*`
 * @param secondaryActivityIntentAction action used for secondary activity launch Intent. If it is
 * not `null`, the [SplitPairFilter] will check the activity [Intent.getAction] besides the
 * component name. If it is `null`, [Intent.getAction] will be ignored.
 */
class SplitPairFilter(
    /**
     * Component name of the primary activity in the split. Must be non-empty. Can contain a single
     * wildcard at the end.
     * Supported formats:
     * - package/class
     * - `package/*`
     * - `package/suffix.*`
     * - `*/*`
     */
    val primaryActivityName: ComponentName,
    /**
     * Component name of the secondary activity in the split. Must be non-empty. Can contain a
     * single wildcard at the end.
     * Supported formats:
     * - package/class
     * - `package/*`
     * - `package/suffix.*`
     * - `*/*`
     */
    val secondaryActivityName: ComponentName,
    /**
     * Action used for secondary activity launch Intent.
     *
     * If it is not `null`, the [SplitPairFilter] will check the activity [Intent.getAction] besides
     * the component name. If it is `null`, [Intent.getAction] will be ignored.
     */
    val secondaryActivityIntentAction: String?
) {

    init {
        validateComponentName(primaryActivityName.packageName, primaryActivityName.className)
        validateComponentName(secondaryActivityName.packageName, secondaryActivityName.className)
    }

    private val primaryActivityInfo: ActivityComponentInfo
        get() = ActivityComponentInfo(primaryActivityName)

    private val secondaryActivityInfo: ActivityComponentInfo
        get() = ActivityComponentInfo(secondaryActivityName)

    /**
     * Returns `true` if this [SplitPairFilter] matches [primaryActivity] and [secondaryActivity].
     * If the [SplitPairFilter] is created with [secondaryActivityIntentAction], the filter will
     * also compare it with [Intent.getAction] of [Activity.getIntent] of [secondaryActivity].
     *
     * @param primaryActivity the [Activity] to test against with the [primaryActivityName]
     * @param secondaryActivity the [Activity] to test against with the [secondaryActivityName]
     */
    fun matchesActivityPair(primaryActivity: Activity, secondaryActivity: Activity): Boolean {
        // Check if the activity component names match
        val match = if (!isActivityMatching(primaryActivity, primaryActivityInfo)) {
            false
        } else if (!isActivityMatching(secondaryActivity, secondaryActivityInfo)) {
            false
        } else {
            secondaryActivityIntentAction == null ||
                secondaryActivityIntentAction == secondaryActivity.intent?.action
        }

        if (sDebugMatchers) {
            val matchString = if (match) "MATCH" else "NO MATCH"
            Log.d(
                sMatchersTag,
                "Checking filter $this against activity pair: (${primaryActivity.componentName}, " +
                    "${secondaryActivity.componentName}) - $matchString"
            )
        }
        return match
    }

    /**
     * Returns `true` if this [SplitPairFilter] matches the [primaryActivity] and the
     * [secondaryActivityIntent]
     * If the [SplitPairFilter] is created with [secondaryActivityIntentAction], the filter will
     * also compare it with [Intent.getAction] of the [secondaryActivityIntent].
     *
     * @param primaryActivity the [Activity] to test against with the [primaryActivityName]
     * @param secondaryActivityIntent the [Intent] to test against with the [secondaryActivityName]
     */
    fun matchesActivityIntentPair(
        primaryActivity: Activity,
        secondaryActivityIntent: Intent
    ): Boolean {
        val match = if (!isActivityMatching(primaryActivity, primaryActivityInfo)) {
            false
        } else if (!isIntentMatching(secondaryActivityIntent, secondaryActivityInfo)) {
            false
        } else {
            secondaryActivityIntentAction == null ||
                secondaryActivityIntentAction == secondaryActivityIntent.action
        }
        if (sDebugMatchers) {
            val matchString = if (match) "MATCH" else "NO MATCH"
            Log.w(
                sMatchersTag,
                "Checking filter $this against activity-intent pair: " +
                    "(${primaryActivity.componentName}, $secondaryActivityIntent) - $matchString"
            )
        }
        return match
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitPairFilter) return false

        if (primaryActivityName != other.primaryActivityName) return false
        if (secondaryActivityName != other.secondaryActivityName) return false
        if (secondaryActivityIntentAction != other.secondaryActivityIntentAction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryActivityName.hashCode()
        result = 31 * result + secondaryActivityName.hashCode()
        result = 31 * result + (secondaryActivityIntentAction?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "SplitPairFilter{primaryActivityName=$primaryActivityName, " +
            "secondaryActivityName=$secondaryActivityName, " +
            "secondaryActivityAction=$secondaryActivityIntentAction}"
    }
}