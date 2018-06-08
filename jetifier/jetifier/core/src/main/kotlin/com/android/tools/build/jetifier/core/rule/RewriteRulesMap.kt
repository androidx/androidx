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

package com.android.tools.build.jetifier.core.rule

import com.android.tools.build.jetifier.core.type.JavaType

/**
 * Contains all [RewriteRule]s.
 */
class RewriteRulesMap(val rewriteRules: List<RewriteRule>) {

    companion object {
        private const val TAG = "RewriteRulesMap"

        val EMPTY = RewriteRulesMap(emptyList())
    }

    constructor(vararg rules: RewriteRule) : this(rules.toList())

    val runtimeIgnoreRules = rewriteRules.filter { it.isRuntimeIgnoreRule() }.toSet()

    /**
     * Tries to rewrite the given given type using the rules. If
     */
    fun rewriteType(type: JavaType): JavaType? {
        // Try to find a rule
        for (rule in rewriteRules) {
            if (rule.isIgnoreRule()) {
                continue
            }
            val typeRewriteResult = rule.apply(type)
            if (typeRewriteResult.result == null) {
                continue
            }
            return typeRewriteResult.result
        }

        return null
    }

    fun reverse(): RewriteRulesMap {
        return RewriteRulesMap(rewriteRules
            .filter { !it.isIgnoreRule() }
            .map { it.reverse() }
            .toList())
    }

    fun appendRules(rules: List<RewriteRule>): RewriteRulesMap {
        return RewriteRulesMap(rewriteRules + rules)
    }

    fun toJson(): JsonData {
        return JsonData(rewriteRules.map { it.toJson() }.toSet())
    }

    /**
     * JSON data model for [RewriteRulesMap].
     */
    data class JsonData(val rules: Set<RewriteRule.JsonData>)
}