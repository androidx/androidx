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
import androidx.window.embedding.MatcherUtils.sDebugMatchers
import androidx.window.embedding.MatcherUtils.sMatchersTag

/**
 * Filter for [ActivityRule] that checks for component name match. Allows a wildcard symbol in the
 * end or instead of the package name, and a wildcard symbol in the end or instead of the class
 * name.
 */
@ExperimentalWindowApi
class ActivityFilter(
    /**
     * Component name in the intent for the activity. Must be non-empty. Can contain a single
     * wildcard at the end. Supported formats:
     * <li>package/class</li>
     * <li>package/\*</li>
     * <li>package/suffix.\*</li>
     * <li>\*\/\*</li>
     */
    val componentName: ComponentName,
    /**
     * Action used for activity launch intent.
     */
    val intentAction: String?
) {
    init {
        val packageName = componentName.packageName
        val className = componentName.className
        require(
            packageName.isNotEmpty()
        ) { "Package name must not be empty" }
        require(
            className.isNotEmpty()
        ) { "Activity class name must not be empty." }
        require(
            !(
                packageName.contains("*") &&
                    packageName.indexOf("*") != packageName.length - 1
                )
        ) { "Wildcard in package name is only allowed at the end." }
        require(
            !(
                className.contains("*") &&
                    className.indexOf("*") != className.length - 1
                )
        ) { "Wildcard in class name is only allowed at the end." }
    }

    fun matchesIntent(intent: Intent): Boolean {
        val match =
            if (!MatcherUtils.areComponentsMatching(intent.component, componentName)) {
                false
            } else {
                intentAction == null || intentAction == intent.action
            }
        if (sDebugMatchers) {
            val matchString = if (match) "MATCH" else "NO MATCH"
            Log.w(
                sMatchersTag,
                "Checking filter $this against intent $intent:  $matchString"
            )
        }
        return match
    }

    fun matchesActivity(activity: Activity): Boolean {
        // Check if the activity component names match
        var match = MatcherUtils.areComponentsMatching(activity.componentName, componentName)
        // If the intent is not empty - check that the rest of the filter fields match
        if (activity.intent != null) {
            match = match && matchesIntent(activity.intent)
        }
        if (sDebugMatchers) {
            val matchString = if (match) "MATCH" else "NO MATCH"
            Log.w(
                sMatchersTag,
                "Checking filter $this against activity $activity:  $matchString"
            )
        }
        return match
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityFilter) return false

        if (componentName != other.componentName) return false
        if (intentAction != other.intentAction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = componentName.hashCode()
        result = 31 * result + (intentAction?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ActivityFilter(componentName=$componentName, intentAction=$intentAction)"
    }
}