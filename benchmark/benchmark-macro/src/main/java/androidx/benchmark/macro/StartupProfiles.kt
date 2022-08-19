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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.benchmark.macro

import androidx.annotation.RestrictTo

private val PROFILE_RULE_REGEX = "(H?S?P?)L([^$;]*)(.*)".toRegex()

/**
 * Builds a startup profile for a given baseline profile.
 *
 * This startup profile can be used for dex layout optimizations.
 */
fun startupProfile(profile: String, includeStartupOnly: Boolean = false): String {
    val rules = profile.lines().mapNotNull { rule ->
        when (val result = PROFILE_RULE_REGEX.find(rule)) {
            null -> null
            else -> {
                val (flags, classPrefix, _) = result.destructured
                if (includeStartupOnly && !flags.contains("S")) {
                    null
                } else {
                    "SL$classPrefix;"
                }
            }
        }
    }
    val ruleSet = mutableSetOf<String>()
    val startupRules = mutableListOf<String>()
    // Try and keep the same order
    rules.forEach { rule ->
        if (!ruleSet.contains(rule)) {
            ruleSet += rule
            startupRules += rule
        }
    }
    return startupRules.joinToString(separator = "\n")
}
