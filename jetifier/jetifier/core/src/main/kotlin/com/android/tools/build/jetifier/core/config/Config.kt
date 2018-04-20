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
import com.android.tools.build.jetifier.core.pom.PomRewriteRule
import com.android.tools.build.jetifier.core.proguard.ProGuardTypesMap
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.TypesMap
import com.google.gson.annotations.SerializedName

/**
 * The main and only one configuration that is used by the tool and all its transformers.
 *
 * [restrictToPackagePrefixes] Package prefixes that limit the scope of the rewriting
 * [rulesMap] Rules to scan support libraries to generate [TypesMap]
 * [slRules] List of rules used when rewriting the support library itself in the reversed mode to
 * ignore packages that don't need rewriting anymore.
 * [pomRewriteRules] Rules to rewrite POM files
 * [typesMap] Map of all java types and fields to be used to rewrite libraries.
 * [packageMap] Package map to be used to rewrite packages, used only during the support library
 * rewrite.
 */
data class Config(
    val restrictToPackagePrefixes: Set<String>,
    val rulesMap: RewriteRulesMap,
    val slRules: List<RewriteRule>,
    val pomRewriteRules: Set<PomRewriteRule>,
    val typesMap: TypesMap,
    val proGuardMap: ProGuardTypesMap,
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

    val restrictToPackagePrefixesWithDots: List<String> = restrictToPackagePrefixes
            .map { it.replace("/", ".") }

    companion object {
        /** Path to the default config file located within the jar file. */
        const val DEFAULT_CONFIG_RES_PATH = "/default.generated.config"

        val EMPTY = Config(
            restrictToPackagePrefixes = emptySet(),
            rulesMap = RewriteRulesMap.EMPTY,
            slRules = emptyList(),
            pomRewriteRules = emptySet(),
            typesMap = TypesMap.EMPTY,
            proGuardMap = ProGuardTypesMap.EMPTY,
            packageMap = PackageMap.EMPTY
        )
    }

    fun setNewMap(mappings: TypesMap): Config {
        return Config(restrictToPackagePrefixes, rulesMap, slRules, pomRewriteRules,
            mappings, proGuardMap)
    }

    /** Returns JSON data model of this class */
    fun toJson(): JsonData {
        return JsonData(
            restrictToPackagePrefixes.toList(),
            rulesMap.toJson().rules.toList(),
            slRules.map { it.toJson() }.toList(),
            pomRewriteRules.map { it.toJson() }.toList(),
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

            @SerializedName("rules")
            val rules: List<RewriteRule.JsonData?>?,

            @SerializedName("slRules")
            val slRules: List<RewriteRule.JsonData?>?,

            @SerializedName("pomRules")
            val pomRules: List<PomRewriteRule.JsonData?>,

            @SerializedName("map")
            val mappings: TypesMap.JsonData? = null,

            @SerializedName("proGuardMap")
            val proGuardMap: ProGuardTypesMap.JsonData? = null
    ) {
        /** Creates instance of [Config] */
        fun toConfig(): Config {

            return Config(
                restrictToPackagePrefixes = restrictToPackages.filterNotNull().toSet(),
                rulesMap = RewriteRulesMap(
                    rules?.filterNotNull()?.map { it.toRule() }?.toList() ?: emptyList()),
                slRules = slRules?.filterNotNull()?.map { it.toRule() }?.toList() ?: emptyList(),
                pomRewriteRules = pomRules.filterNotNull().map { it.toRule() }.toSet(),
                typesMap = mappings?.toMappings() ?: TypesMap.EMPTY,
                proGuardMap = proGuardMap?.toMappings() ?: ProGuardTypesMap.EMPTY
            )
        }
    }
}
