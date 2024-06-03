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
 * Filter for [ActivityRule] and [SplitPlaceholderRule] that checks for component name match when a
 * new activity is started. If the filter matches the started activity [Intent], the activity will
 * then apply the rule based on the match result. This filter allows a wildcard symbol in the end or
 * instead of the package name, and a wildcard symbol in the end or instead of the class name.
 */
class ActivityFilter
internal constructor(
    /**
     * Component name in the intent for the activity. Must be non-empty. Can contain a single
     * wildcard at the end. Supported formats:
     * - package/class
     * - `package/*`
     * - `package/suffix.*`
     * - `*/*`
     */
    internal val activityComponentInfo: ActivityComponentInfo,
    /**
     * Action used for activity launch intent.
     *
     * If it is not `null`, the [ActivityFilter] will check the activity [Intent.getAction] besides
     * the component name. If it is `null`, [Intent.getAction] will be ignored.
     */
    val intentAction: String?
) {

    /**
     * Constructs a new [ActivityFilter] using a [ComponentName] and an [Intent] action.
     *
     * @param componentName Component name in the intent for the activity. Must be non-empty. Can
     *   contain a single wildcard at the end. Supported formats:
     *     - package/class
     *     - `package/*`
     *     - `package/suffix.*`
     *     - `*/*`
     *
     * @param intentAction Action used for activity launch intent. If it is not `null`, the
     *   [ActivityFilter] will check the activity [Intent.getAction] besides the component name. If
     *   it is `null`, [Intent.getAction] will be ignored.
     */
    constructor(
        componentName: ComponentName,
        intentAction: String?
    ) : this(ActivityComponentInfo(componentName), intentAction)

    init {
        validateComponentName(activityComponentInfo.packageName, activityComponentInfo.className)
    }

    /**
     * Returns `true` if the [ActivityFilter] matches this [Intent]. If the [ActivityFilter] is
     * created with an intent action, the filter will also compare it with [Intent.getAction].
     *
     * @param intent the [Intent] to test against.
     */
    fun matchesIntent(intent: Intent): Boolean {
        val match =
            if (!isIntentMatching(intent, activityComponentInfo)) {
                false
            } else {
                intentAction == null || intentAction == intent.action
            }
        if (sDebugMatchers) {
            val matchString = if (match) "MATCH" else "NO MATCH"
            Log.w(sMatchersTag, "Checking filter $this against intent $intent:  $matchString")
        }
        return match
    }

    /**
     * Returns `true` if the [ActivityFilter] matches this [Activity]. If the [ActivityFilter] is
     * created with an intent action, the filter will also compare it with [Intent.getAction] of
     * [Activity.getIntent].
     *
     * @param activity the [Activity] to test against.
     */
    fun matchesActivity(activity: Activity): Boolean {
        val match =
            isActivityMatching(activity, activityComponentInfo) &&
                (intentAction == null || intentAction == activity.intent?.action)
        if (sDebugMatchers) {
            val matchString = if (match) "MATCH" else "NO MATCH"
            Log.w(sMatchersTag, "Checking filter $this against activity $activity:  $matchString")
        }
        return match
    }

    /** [ComponentName] that the [ActivityFilter] will use to match [Activity] and [Intent]. */
    val componentName: ComponentName
        get() {
            return ComponentName(activityComponentInfo.packageName, activityComponentInfo.className)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityFilter) return false

        if (activityComponentInfo != other.activityComponentInfo) return false
        if (intentAction != other.intentAction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activityComponentInfo.hashCode()
        result = 31 * result + (intentAction?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ActivityFilter(componentName=$activityComponentInfo, intentAction=$intentAction)"
    }
}
