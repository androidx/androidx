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

package android.support.tools.jetifier.core.config

import android.support.tools.jetifier.core.rules.RewriteRule
import android.support.tools.jetifier.core.transform.pom.PomRewriteRule
import android.support.tools.jetifier.core.map.TypesMap
import com.google.gson.annotations.SerializedName

/**
 * The main and only one configuration that is used by the tool and all its transformers.
 *
 * [restrictToPackagePrefixes] Package prefixes that limit the scope of the rewriting
 * [rewriteRules] Rules to scan support libraries to generate [TypesMap]
 * [pomRewriteRules] Rules to rewrite POM files
 * [typesMap] Map of all java types and fields to be used to rewrite libraries.
 */
data class Config(
        val restrictToPackagePrefixes: List<String>,
        val rewriteRules: List<RewriteRule>,
        val pomRewriteRules: List<PomRewriteRule>,
        val typesMap: TypesMap) {

    companion object {
        /** Path to the default config file located within the jar file. */
        const val DEFAULT_CONFIG_RES_PATH = "/default.config"
    }

    fun setNewMap(mappings: TypesMap) : Config {
        return Config(restrictToPackagePrefixes, rewriteRules, pomRewriteRules, mappings)
    }

    /** Returns JSON data model of this class */
    fun toJson() : JsonData {
        return JsonData(
            restrictToPackagePrefixes,
            rewriteRules.map { it.toJson() }.toList(),
            pomRewriteRules.map { it.toJson() }.toList(),
            typesMap.toJson()
        )
    }


    /**
     * JSON data model for [Config].
     */
    data class JsonData(
            @SerializedName("restrictToPackagePrefixes")
            val restrictToPackages: List<String?>,

            @SerializedName("rules")
            val rules: List<RewriteRule.JsonData?>,

            @SerializedName("pomRules")
            val pomRules: List<PomRewriteRule.JsonData?>,

            @SerializedName("map")
            val mappings: TypesMap.JsonData? = null) {

        /** Creates instance of [Config] */
        fun toConfig() : Config {
            return Config(
                restrictToPackages.filterNotNull(),
                rules.filterNotNull().map { it.toRule() },
                pomRules.filterNotNull().map { it.toRule() },
                mappings?.toMappings() ?: TypesMap.EMPTY
            )
        }
    }

}
