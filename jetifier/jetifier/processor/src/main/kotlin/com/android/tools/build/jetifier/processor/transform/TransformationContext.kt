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

package com.android.tools.build.jetifier.processor.transform

import com.android.tools.build.jetifier.core.TypeRewriter
import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.pom.DependencyVersions
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.utils.Log
import java.nio.file.Path
import java.util.regex.Pattern

/**
 * Context to share the transformation state between individual [Transformer]s.
 */
class TransformationContext(
    val config: Config,
    val rewritingSupportLib: Boolean = false,
    val isInReversedMode: Boolean = false,
    /**
     * Whether to use fallback if type in our scope is missing instead of throwing an exception.
     */
    val useFallbackIfTypeIsMissing: Boolean = true,
    /**
     * Whether packages such as "android.support.v4" should be allowed. We throw an exception
     * otherwise.
     */
    val allowAmbiguousPackages: Boolean = false,
    val versions: DependencyVersions = DependencyVersions.EMPTY
) {

    // Merges all packages prefixes into one regEx pattern
    val packagePrefixPattern = Pattern.compile(
        "^(" + config.restrictToPackagePrefixes.map { "($it)" }.joinToString("|") + ").*$"
    )

    val typeRewriter: TypeRewriter = TypeRewriter(config, useFallbackIfTypeIsMissing)

    /**
     * Whether to skip verification of dependency version match in pom files.
     */
    val ignorePomVersionCheck = true

    /** Counter for [reportNoMappingFoundFailure] calls. */
    var mappingNotFoundFailuresCount = 0
        private set

    /** Counter for [reportNoProGuardMappingFoundFailure] calls. */
    var proGuardMappingNotFoundFailuresCount = 0
        private set

    /** Counter for [reportNoPackageMappingFoundFailure] calls. */
    var packageMappingNotFoundFailuresCounts = 0

    /** Total amount of errors found during the transformation process */
    fun errorsTotal() = mappingNotFoundFailuresCount + proGuardMappingNotFoundFailuresCount +
        packageMappingNotFoundFailuresCounts

    /**
     * Reports that there was a reference found that satisfies [isEligibleForRewrite] but no
     * mapping was found to rewrite it.
     */
    fun reportNoMappingFoundFailure(tag: String, type: JavaType) {
        if (!useFallbackIfTypeIsMissing || (rewritingSupportLib && isInReversedMode)) {
            mappingNotFoundFailuresCount++
            Log.e(tag, "No mapping for: " + type)
        } else {
            Log.w(tag, "No mapping for: " + type)
        }
    }

    /**
     * Reports that there was a reference found in a ProGuard file that satisfies
     * [isEligibleForRewrite] but no mapping was found to rewrite it.
     */
    fun reportNoProGuardMappingFoundFailure(tag: String, type: String) {
        if (!useFallbackIfTypeIsMissing || (rewritingSupportLib && isInReversedMode)) {
            proGuardMappingNotFoundFailuresCount++
            Log.e(tag, "No mapping for: " + type)
        } else {
            Log.w(tag, "No mapping for: " + type)
        }
    }

    /**
     * Reports that there was a package reference found in a manifest file during a support library
     * artifact rewrite but no mapping was found for it.
     */
    fun reportNoPackageMappingFoundFailure(tag: String, packageName: String, filePath: Path) {
        if (rewritingSupportLib && isInReversedMode) {
            // Ignore for SL de-jetification
            return
        }

        if (!useFallbackIfTypeIsMissing) {
            packageMappingNotFoundFailuresCounts++
            Log.w(
                tag, "No mapping for package '%s' in '%s', keeping identity", packageName,
                filePath
            )
        } else {
            Log.w(
                tag, "No mapping for package '%s' in '%s', keeping identity", packageName,
                filePath
            )
        }
    }

    fun reportUnreadableKotlinModule(tag: String, filePath: Path) {
        Log.e(tag, "Unreadable kotlin module medata file: %s", filePath)
    }
}