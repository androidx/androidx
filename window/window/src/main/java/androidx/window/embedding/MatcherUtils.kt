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
import android.util.Log
import androidx.window.core.ExperimentalWindowApi

/**
 * Internal utils used for matching activities with embedding rules.
 */
@ExperimentalWindowApi
internal object MatcherUtils {
    /** Checks component match allowing wildcard patterns. */
    internal fun areComponentsMatching(
        activityComponent: ComponentName?,
        ruleComponent: ComponentName
    ): Boolean {
        if (activityComponent == null) {
            return ruleComponent.packageName == "*" && ruleComponent.className == "*"
        }
        require(
            !activityComponent.toString().contains("*")
        ) { "Wildcard can only be part of the rule." }

        val packagesMatch = activityComponent.packageName == ruleComponent.packageName ||
            wildcardMatch(activityComponent.packageName, ruleComponent.packageName)
        val classesMatch = activityComponent.className == ruleComponent.className ||
            wildcardMatch(activityComponent.className, ruleComponent.className)

        if (sDebugMatchers) {
            Log.d(
                sMatchersTag,
                "Checking match of $activityComponent with rule " +
                    "component $ruleComponent, " + "packages match: $packagesMatch, " +
                    "classes match: $classesMatch"
            )
        }
        return packagesMatch && classesMatch
    }

    /**
     * Returns `true` if [Activity.getComponentName] match or
     * [Component][android.content.Intent.getComponent] of [Activity.getIntent] match allowing
     * wildcard patterns.
     */
    internal fun areActivityOrIntentComponentsMatching(
        activity: Activity,
        ruleComponent: ComponentName
    ): Boolean {
        if (areComponentsMatching(activity.componentName, ruleComponent)) {
            return true
        }
        // Returns false if activity's intent doesn't exist or its intent's Component doesn't match.
        return activity.intent?.component ?.let {
                component -> areComponentsMatching(component, ruleComponent) } ?: false
    }

    /**
     * Checks if the provided name matches the pattern with a wildcard.
     * @return {@code true} if the pattern contains a wildcard, and the pattern matches the
     * provided name.
     */
    private fun wildcardMatch(name: String, pattern: String): Boolean {
        if (!pattern.contains("*")) {
            return false
        }
        if (pattern == "*") {
            return true
        }
        require(
            !(
                pattern.indexOf("*") != pattern.lastIndexOf("*") ||
                    !pattern.endsWith("*")
                )
        ) { "Name pattern with a wildcard must only contain a single wildcard in the end" }
        return name.startsWith(pattern.substring(0, pattern.length - 1))
    }

    internal const val sDebugMatchers = SplitController.sDebug
    internal const val sMatchersTag = "SplitRuleResolution"
}