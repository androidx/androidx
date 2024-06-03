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
import android.content.Intent
import android.util.Log
import androidx.window.core.ActivityComponentInfo

/** Internal utils used for matching activities with embedding rules. */
internal object MatcherUtils {
    /** Checks component match allowing wildcard patterns. */
    internal fun areComponentsMatching(
        activityComponent: ActivityComponentInfo?,
        ruleComponent: ActivityComponentInfo
    ): Boolean {
        if (activityComponent == null) {
            return ruleComponent.packageName == "*" && ruleComponent.className == "*"
        }
        require(!activityComponent.toString().contains("*")) {
            "Wildcard can only be part of the rule."
        }

        val packagesMatch =
            activityComponent.packageName == ruleComponent.packageName ||
                wildcardMatch(activityComponent.packageName, ruleComponent.packageName)
        val classesMatch =
            activityComponent.className == ruleComponent.className ||
                wildcardMatch(activityComponent.className, ruleComponent.className)

        if (sDebugMatchers) {
            Log.d(
                sMatchersTag,
                "Checking match of $activityComponent with rule " +
                    "component $ruleComponent, " +
                    "packages match: $packagesMatch, " +
                    "classes match: $classesMatch"
            )
        }
        return packagesMatch && classesMatch
    }

    /**
     * Returns `true` if [Activity.getComponentName] match or [Activity.getIntent] match allowing
     * wildcard patterns.
     */
    internal fun isActivityMatching(
        activity: Activity,
        ruleComponent: ActivityComponentInfo
    ): Boolean {
        if (areComponentsMatching(ActivityComponentInfo(activity.componentName), ruleComponent)) {
            return true
        }
        // Returns false if activity's intent doesn't exist or its intent doesn't match.
        return activity.intent?.let { intent -> isIntentMatching(intent, ruleComponent) } ?: false
    }

    /**
     * Returns `true` if [Intent.getComponent] match or [Intent.getPackage] match allowing wildcard
     * patterns.
     */
    internal fun isIntentMatching(intent: Intent, ruleComponent: ActivityComponentInfo): Boolean {
        // Check if the component is matched.
        if (areComponentsMatching(intent.component?.let(::ActivityComponentInfo), ruleComponent)) {
            return true
        }
        if (intent.component != null) {
            // Only check package if the component is not set.
            return false
        }
        // Check if there is wildcard match for Intent that only specifies the packageName.
        val packageName = intent.`package` ?: return false
        return (packageName == ruleComponent.packageName ||
            wildcardMatch(packageName, ruleComponent.packageName)) && ruleComponent.className == "*"
    }

    /**
     * Checks if the provided name matches the pattern with a wildcard.
     *
     * @return {@code true} if the pattern contains a wildcard, and the pattern matches the provided
     *   name.
     */
    private fun wildcardMatch(name: String, pattern: String): Boolean {
        if (!pattern.contains("*")) {
            return false
        }
        if (pattern == "*") {
            return true
        }
        require(!(pattern.indexOf("*") != pattern.lastIndexOf("*") || !pattern.endsWith("*"))) {
            "Name pattern with a wildcard must only contain a single wildcard in the end"
        }
        return name.startsWith(pattern.substring(0, pattern.length - 1))
    }

    /** Makes sure the component name is in the correct format. */
    internal fun validateComponentName(packageName: String, className: String) {
        require(packageName.isNotEmpty()) { "Package name must not be empty" }
        require(className.isNotEmpty()) { "Activity class name must not be empty" }
        require(
            !(packageName.contains("*") && packageName.indexOf("*") != packageName.length - 1)
        ) {
            "Wildcard in package name is only allowed at the end."
        }
        require(!(className.contains("*") && className.indexOf("*") != className.length - 1)) {
            "Wildcard in class name is only allowed at the end."
        }
    }

    internal const val sDebugMatchers = SplitController.sDebug
    internal const val sMatchersTag = "SplitRuleResolution"
}
