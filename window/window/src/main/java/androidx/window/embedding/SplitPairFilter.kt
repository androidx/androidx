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
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.MatcherUtils.areComponentsMatching
import androidx.window.embedding.MatcherUtils.sDebugMatchers
import androidx.window.embedding.MatcherUtils.sMatchersTag

/**
 * Filter used to find if a pair of activities should be put in a split. Applied to the base /
 * primary activity and an intent starting a secondary activity.
 */
@ExperimentalWindowApi
class SplitPairFilter(
    /**
     * Component name of the primary activity in the split. Must be non-empty. Can contain a single
     * wildcard at the end.
     * Supported formats: "package/class", "package&#47;&#42;", "package/suffix.&#42;",
     * "&#42;&#47;&#42;"
     */
    val primaryActivityName: ComponentName,
    /**
     * Component name in the intent for the secondary activity in the split. Must be non-empty.
     * Can contain a single wildcard at the end.
     * Supported formats:
     * <li>package/class</li>
     * <li>package/\*</li>
     * <li>package/suffix.\*</li>
     * <li>\*\/\*</li>
     */
    val secondaryActivityName: ComponentName,
    /**
     * Action used for secondary activity launch.
     */
    val secondaryActivityIntentAction: String?
) {
    fun matchesActivityPair(primaryActivity: Activity, secondaryActivity: Activity): Boolean {
        // Check if the activity component names match
        var match = areComponentsMatching(primaryActivity.componentName, primaryActivityName) &&
            areComponentsMatching(secondaryActivity.componentName, secondaryActivityName)
        // If the intent is not empty - check that the rest of the filter fields match
        if (secondaryActivity.intent != null) {
            match = match && matchesActivityIntentPair(primaryActivity, secondaryActivity.intent)
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

    fun matchesActivityIntentPair(
        primaryActivity: Activity,
        secondaryActivityIntent: Intent
    ): Boolean {
        val inPrimaryActivityName = primaryActivity.componentName
        val match = if (
            !areComponentsMatching(inPrimaryActivityName, primaryActivityName)
        ) {
            false
        } else if (
            !areComponentsMatching(secondaryActivityIntent.component, secondaryActivityName)
        ) {
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

    init {
        val primaryPackageName = primaryActivityName.packageName
        val primaryClassName = primaryActivityName.className
        val secondaryPackageName = secondaryActivityName.packageName
        val secondaryClassName = secondaryActivityName.className
        require(
            !(primaryPackageName.isEmpty() || secondaryPackageName.isEmpty())
        ) { "Package name must not be empty" }
        require(
            !(primaryClassName.isEmpty() || secondaryClassName.isEmpty())
        ) { "Activity class name must not be empty." }
        require(
            !(
                primaryPackageName.contains("*") &&
                    primaryPackageName.indexOf("*") != primaryPackageName.length - 1
                )
        ) { "Wildcard in package name is only allowed at the end." }
        require(
            !(
                primaryClassName.contains("*") &&
                    primaryClassName.indexOf("*") != primaryClassName.length - 1
                )
        ) { "Wildcard in class name is only allowed at the end." }
        require(
            !(
                secondaryPackageName.contains("*") &&
                    secondaryPackageName.indexOf("*") != secondaryPackageName.length - 1
                )
        ) { "Wildcard in package name is only allowed at the end." }
        require(
            !(
                secondaryClassName.contains("*") &&
                    secondaryClassName.indexOf("*") != secondaryClassName.length - 1
                )
        ) { "Wildcard in class name is only allowed at the end." }
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