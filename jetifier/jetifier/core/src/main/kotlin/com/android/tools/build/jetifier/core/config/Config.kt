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

package com.android.tools.build.jetifier.core.config

import com.android.tools.build.jetifier.core.PackageMap
import com.android.tools.build.jetifier.core.pom.DependencyVersionsMap
import com.android.tools.build.jetifier.core.pom.PomRewriteRule
import com.android.tools.build.jetifier.core.proguard.ProGuardType
import com.android.tools.build.jetifier.core.proguard.ProGuardTypesMap
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.PackageName
import com.android.tools.build.jetifier.core.type.TypesMap
import com.google.gson.annotations.SerializedName
import java.util.regex.Pattern

/**
 * The main and only one configuration that is used by the tool and all its transformers.
 *
 * @param restrictToPackagePrefixes Package prefixes that limit the scope of the rewriting. In most
 *  cases the rules have priority over this. We use this mainly to determine if we are actually
 *  missing a rule in case we fail to rewrite.
 * @param reversedRestrictToPackagePrefixes Same as [restrictToPackagePrefixes] but used when
 *  running in reversed mode.
 * @param rulesMap Rules to scan support libraries to generate [TypesMap]
 * @param slRule List of rules used when rewriting the support library itself in the reversed mode
 *  to ignore packages that don't need rewriting anymore.
 * @param pomRewriteRules Rules to rewrite POM files
 * @param typesMap Map of all java types and fields to be used to rewrite libraries.
 * @param proGuardMap Proguard types map to be used for ProGuard files rewriting.
 * @param versionsMap Pre-defined maps of versions to be substituted in pom dependency rules.
 * @param packageMap Package map to be used to rewrite packages, used only during the support
 *  library rewrite.
 */
data class Config(
    val restrictToPackagePrefixes: Set<String>,
    val reversedRestrictToPackagePrefixes: Set<String>,
    val rulesMap: RewriteRulesMap,
    val slRules: List<RewriteRule>,
    val pomRewriteRules: Set<PomRewriteRule>,
    val typesMap: TypesMap,
    val proGuardMap: ProGuardTypesMap,
    val versionsMap: DependencyVersionsMap,
    val packageMap: PackageMap = PackageMap(PackageMap.DEFAULT_RULES)
) {

    init {
        // Verify pom rules
        val testSet = mutableSetOf<String>()
        pomRewriteRules.forEach {
            val raw = "${it.from.groupId}:${it.from.artifactId}"
            if (!testSet.add(raw)) {
                throw IllegalArgumentException("Artifact '$raw' is defined twice in pom rules!")
            }
        }
    }

    // Merges all packages prefixes into one regEx pattern
    private val packagePrefixPattern = Pattern.compile(
        "^(" + restrictToPackagePrefixes.map { "($it)" }.joinToString("|") + ").*$")

    val restrictToPackagePrefixesWithDots: List<String> = restrictToPackagePrefixes
        .map { it.replace("/", ".") }

    companion object {
        /** Path to the default config file located within the jar file. */
        const val DEFAULT_CONFIG_RES_PATH = "/default.generated.config"

        val EMPTY = fromOptional()

        fun fromOptional(
            restrictToPackagePrefixes: Set<String> = emptySet(),
            reversedRestrictToPackagesPrefixes: Set<String> = emptySet(),
            rulesMap: RewriteRulesMap = RewriteRulesMap.EMPTY,
            slRules: List<RewriteRule> = emptyList(),
            pomRewriteRules: Set<PomRewriteRule> = emptySet(),
            typesMap: TypesMap = TypesMap.EMPTY,
            proGuardMap: ProGuardTypesMap = ProGuardTypesMap.EMPTY,
            versionsMap: DependencyVersionsMap = DependencyVersionsMap.EMPTY,
            packageMap: PackageMap = PackageMap.EMPTY
        ): Config {
            return Config(
                restrictToPackagePrefixes = restrictToPackagePrefixes,
                reversedRestrictToPackagePrefixes = reversedRestrictToPackagesPrefixes,
                rulesMap = rulesMap,
                slRules = slRules,
                pomRewriteRules = pomRewriteRules,
                typesMap = typesMap,
                proGuardMap = proGuardMap,
                versionsMap = versionsMap,
                packageMap = packageMap
            )
        }
    }

    fun setNewMap(mappings: TypesMap): Config {
        return Config(
            restrictToPackagePrefixes = restrictToPackagePrefixes,
            reversedRestrictToPackagePrefixes = reversedRestrictToPackagePrefixes,
            rulesMap = rulesMap,
            slRules = slRules,
            pomRewriteRules = pomRewriteRules,
            typesMap = mappings,
            proGuardMap = proGuardMap,
            versionsMap = versionsMap,
            packageMap = packageMap
        )
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

        val isIgnored = rulesMap.runtimeIgnoreRules
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

        val isIgnored = rulesMap.runtimeIgnoreRules.any { it.doesThisIgnoreProGuard(type) }
        return !isIgnored
    }

    fun isEligibleForRewrite(type: PackageName): Boolean {
        if (!isEligibleForRewriteInternal(type.fullName + "/")) {
            return false
        }

        val javaType = JavaType(type.fullName + "/")
        val isIgnored = rulesMap.runtimeIgnoreRules
            .any { it.apply(javaType) == RewriteRule.TypeRewriteResult.IGNORED }
        return !isIgnored
    }

    private fun isEligibleForRewriteInternal(type: String): Boolean {
        if (restrictToPackagePrefixes.isEmpty()) {
            return false
        }
        return packagePrefixPattern.matcher(type).matches()
    }

    /** Returns JSON data model of this class */
    fun toJson(): JsonData {
        return JsonData(
            restrictToPackagePrefixes.toList(),
            reversedRestrictToPackagePrefixes.toList(),
            rulesMap.toJson().rules.toList(),
            slRules.map { it.toJson() }.toList(),
            pomRewriteRules.map { it.toJson() }.toList(),
            versionsMap.data,
            typesMap.toJson(),
            proGuardMap.toJson()
        )
    }

    /**
     * JSON data model for [Config].
     */
    data class JsonData(
        @SerializedName("restrictToPackagePrefixes")
        val restrictToPackages: List<String?>,

        @SerializedName("reversedRestrictToPackagePrefixes")
        val reversedRestrictToPackages: List<String?>,

        @SerializedName("rules")
        val rules: List<RewriteRule.JsonData?>?,

        @SerializedName("slRules")
        val slRules: List<RewriteRule.JsonData?>?,

        @SerializedName("pomRules")
        val pomRules: List<PomRewriteRule.JsonData?>,

        @SerializedName("versions")
        val versions: Map<String, Map<String, String>>? = null,

        @SerializedName("map")
        val mappings: TypesMap.JsonData? = null,

        @SerializedName("proGuardMap")
        val proGuardMap: ProGuardTypesMap.JsonData? = null
    ) {
        /** Creates instance of [Config] */
        fun toConfig(): Config {
            return Config(
                restrictToPackagePrefixes = restrictToPackages.filterNotNull().toSet(),
                reversedRestrictToPackagePrefixes = reversedRestrictToPackages
                    .filterNotNull().toSet(),
                rulesMap = rules
                    ?.let { RewriteRulesMap(it.filterNotNull().map { it.toRule() }.toList()) }
                    ?: RewriteRulesMap.EMPTY,
                slRules = slRules
                    ?.let { it.filterNotNull().map { it.toRule() }.toList() }
                    ?: emptyList(),
                pomRewriteRules = pomRules.filterNotNull().map { it.toRule() }.toSet(),
                versionsMap = versions
                    ?.let { DependencyVersionsMap(versions) }
                    ?: DependencyVersionsMap.EMPTY,
                typesMap = mappings?.toMappings() ?: TypesMap.EMPTY,
                proGuardMap = proGuardMap?.toMappings() ?: ProGuardTypesMap.EMPTY
            )
        }
    }
}
