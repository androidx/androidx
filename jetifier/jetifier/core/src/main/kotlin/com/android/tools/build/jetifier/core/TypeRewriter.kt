/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.core

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.proguard.ProGuardType
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.utils.Log
import java.util.regex.Pattern

/**
 * Wraps capabilities of [TypesMap] and [RewriteRulesMap] into one place.
 */
class TypeRewriter(private val config: Config, private val useFallback: Boolean) {

    companion object {
        private const val TAG = "TypeRewriter"
    }

    // Merges all packages prefixes into one regEx pattern
    private val packagePrefixPattern = Pattern.compile(
            "^(" + config.restrictToPackagePrefixes.map { "($it)" }.joinToString("|") + ").*$")

    fun rewriteType(type: JavaType): JavaType? {
        if (!isEligibleForRewrite(type)) {
            return type
        }

        val result = config.typesMap.mapType(type)
        if (result != null) {
            Log.i(TAG, "Map: %s -> %s", type, result)
            return result
        }

        if (!useFallback) {
            Log.e(TAG, "No mapping for: " + type)
            return null
        }

        val rulesResult = config.rulesMap.rewriteType(type)
        if (rulesResult != null) {
            Log.i(TAG, "Using fallback: %s -> %s", type, rulesResult)
            return rulesResult
        }

        return null
    }

    /**
     * Returns whether the given type is eligible for rewrite.
     *
     * If not, the transformers should ignore it.
     */
    fun isEligibleForRewrite(type: JavaType): Boolean {
        if (!isEligibleForRewriteInternal(type.fullName)) {
            return false
        }

        val isIgnored = config.rulesMap.runtimeIgnoreRules
            .any { it.apply(type) == RewriteRule.TypeRewriteResult.IGNORED }
        return !isIgnored
    }

    /**
    * Returns whether the given ProGuard type reference is eligible for rewrite.
    *
    * Keep in mind that his has limited capabilities - mainly when * is used as a prefix. Rules
    * like *.v7 are not matched by prefix support.v7. So don't rely on it and use
    * the [ProGuardTypesMap] as first.
    */
    fun isEligibleForRewrite(type: ProGuardType): Boolean {
        if (!isEligibleForRewriteInternal(type.value)) {
            return false
        }

        val isIgnored = config.rulesMap.runtimeIgnoreRules.any { it.doesThisIgnoreProGuard(type) }
        return !isIgnored
    }

    private fun isEligibleForRewriteInternal(type: String): Boolean {
        if (config.restrictToPackagePrefixes.isEmpty()) {
            return false
        }
        return packagePrefixPattern.matcher(type).matches()
    }
}