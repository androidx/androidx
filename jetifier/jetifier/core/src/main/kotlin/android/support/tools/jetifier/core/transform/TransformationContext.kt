/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.rules.RewriteRule.TypeRewriteResult
import android.support.tools.jetifier.core.transform.proguard.ProGuardType
import java.util.regex.Pattern

/**
 * Context to share the transformation state between individual [Transformer]s.
 */
class TransformationContext(val config: Config) {

    // Merges all packages prefixes into one regEx pattern
    private val packagePrefixPattern = Pattern.compile(
        "^(" + config.restrictToPackagePrefixes.map { "($it)" }.joinToString("|") + ").*$")

    /** Counter for [reportNoMappingFoundFailure] calls. */
    var mappingNotFoundFailuresCount = 0
        private set

    /** Counter for [reportNoProGuardMappingFoundFailure] calls. */
    var proGuardMappingNotFoundFailuresCount = 0
        private set

    private var runtimeIgnoreRules = config.rewriteRules
        .filter { it.isRuntimeIgnoreRule() }
        .toTypedArray()

    /** Returns whether any errors were found during the transformation process */
    fun wasErrorFound() = mappingNotFoundFailuresCount > 0
        || proGuardMappingNotFoundFailuresCount > 0

    /**
     * Returns whether the given type is eligible for rewrite.
     *
     * If not, the transformers should ignore it.
     */
    fun isEligibleForRewrite(type: JavaType): Boolean {
        if (!isEligibleForRewriteInternal(type.fullName)) {
            return false
        }

        val isIgnored = runtimeIgnoreRules.any { it.apply(type) == TypeRewriteResult.IGNORED }
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

        val isIgnored = runtimeIgnoreRules.any { it.doesThisIgnoreProGuard(type) }
        return !isIgnored
    }

    private fun isEligibleForRewriteInternal(type: String): Boolean {
        if (config.restrictToPackagePrefixes.isEmpty()) {
            return false
        }
        return packagePrefixPattern.matcher(type).matches()
    }

    /**
     * Used to report that there was a reference found that satisfies [isEligibleForRewrite] but no
     * mapping was found to rewrite it.
     */
    fun reportNoMappingFoundFailure() {
        mappingNotFoundFailuresCount++
    }

    /**
     * Used to report that there was a reference found in the ProGuard file that satisfies
     * [isEligibleForRewrite] but no mapping was found to rewrite it.
     */
    fun reportNoProGuardMappingFoundFailure() {
        proGuardMappingNotFoundFailuresCount++
    }
}