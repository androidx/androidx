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

package com.android.tools.build.jetifier.core.proguard

import com.android.tools.build.jetifier.core.utils.Log

/**
 * Contains custom mappings to map support library types referenced in ProGuard to new ones.
 */
data class ProGuardTypesMap(private val rules: Map<ProGuardType, Set<ProGuardType>>) {

    companion object {
        const val TAG = "ProGuardTypesMap"

        val EMPTY = ProGuardTypesMap(emptyMap())
    }

    private val expandedRules: Map<ProGuardType, Set<ProGuardType>> by lazy {
        val expandedMap = mutableMapOf<ProGuardType, Set<ProGuardType>>()
        rules.forEach { (from, to) ->
            if (from.needsExpansion() || to.any { it.needsExpansion() }) {
                ProGuardType.EXPANSION_TOKENS.forEach {
                    t -> expandedMap.put(from.expandWith(t), to.map { it.expandWith(t) }.toSet())
                }
            } else {
                expandedMap.put(from, to)
            }
        }
        expandedMap
    }

    constructor(vararg rules: Pair<ProGuardType, ProGuardType>) :
        this(rules.map { it.first to setOf(it.second) }.toMap())

    /** Returns JSON data model of this class */
    fun toJson(): JsonData {
        return JsonData(rules.map { it.key.value to it.value.map { it.value }.toList() }.toMap())
    }

    fun mapType(type: ProGuardType): Set<ProGuardType>? {
        return expandedRules[type]
    }

    /**
     * JSON data model for [ProGuardTypesMap].
     */
    data class JsonData(val rules: Map<String, List<String>>) {

        /** Creates instance of [ProGuardTypesMap] */
        fun toMappings(): ProGuardTypesMap {
            return ProGuardTypesMap(rules
                .map { ProGuardType(it.key) to it.value.map { ProGuardType(it) }.toSet() }
                .toMap())
        }
    }

    /**
     * Creates reversed version of this map (values become keys). If there are multiple keys mapped
     * to the same value only the first value is used and warning message is printed.
     */
    fun reverseMap(): ProGuardTypesMap {
        val reversed = mutableMapOf<ProGuardType, ProGuardType>()
        for ((from, to) in rules) {
            if (to.size > 1) {
                // Skip reversal of a set
                continue
            }

            val conflictFrom = reversed[to.single()]
            if (conflictFrom != null) {
                // Conflict - skip
                Log.v(TAG, "Conflict: %s -> (%s, %s)", to, from, conflictFrom)
                continue
            }
            reversed[to.single()] = from
        }

        return ProGuardTypesMap(reversed
            .map { it.key to setOf(it.value) }
            .toMap())
    }
}