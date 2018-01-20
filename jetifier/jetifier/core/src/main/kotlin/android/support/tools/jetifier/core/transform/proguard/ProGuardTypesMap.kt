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

package android.support.tools.jetifier.core.transform.proguard

import android.support.tools.jetifier.core.utils.Log

/**
 * Contains custom mappings to map support library types referenced in ProGuard to new ones.
 */
data class ProGuardTypesMap(val rules: Map<ProGuardType, ProGuardType>) {

    companion object {
        const val TAG = "ProGuardTypesMap"

        val EMPTY = ProGuardTypesMap(emptyMap())
    }

    /** Returns JSON data model of this class */
    fun toJson(): JsonData {
        return JsonData(rules.map { it.key.value to it.value.value }.toMap())
    }

    /**
     * JSON data model for [ProGuardTypesMap].
     */
    data class JsonData(val rules: Map<String, String>) {

        /** Creates instance of [ProGuardTypesMap] */
        fun toMappings(): ProGuardTypesMap {
            return ProGuardTypesMap(
                rules.map { ProGuardType(it.key) to ProGuardType(it.value) }.toMap())
        }
    }

    /**
     * Creates reversed version of this map (values become keys). Throws exception if the map does
     * not satisfy that.
     */
    fun reverseMapOrDie(): ProGuardTypesMap {
        val reversed = mutableMapOf<ProGuardType, ProGuardType>()
        for ((from, to) in rules) {
            val conflictFrom = reversed[to]
            if (conflictFrom != null) {
                Log.e(TAG, "Conflict: %s -> (%s, %s)", to, from, conflictFrom)
                continue
            }
            reversed[to] = from
        }

        if (rules.size != reversed.size || rules.size != reversed.size) {
            throw IllegalArgumentException("Types map is not reversible as conflicts were found! " +
                "See the log for more details.")
        }

        return ProGuardTypesMap(reversed)
    }
}