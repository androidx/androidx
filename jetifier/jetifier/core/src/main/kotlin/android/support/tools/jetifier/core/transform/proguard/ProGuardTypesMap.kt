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

/**
 * Contains custom mappings to map support library types referenced in ProGuard to new ones.
 */
data class ProGuardTypesMap(val rules: Map<ProGuardType, ProGuardType>) {

    companion object {
        val EMPTY = ProGuardTypesMap(emptyMap())
    }

    /** Returns JSON data model of this class */
    fun toJson() : JsonData {
        return JsonData(rules.map { it.key.value to it.value.value }.toMap())
    }

    /**
     * JSON data model for [ProGuardTypesMap].
     */
    data class JsonData(val rules: Map<String, String>)  {

        /** Creates instance of [ProGuardTypesMap] */
        fun toMappings() : ProGuardTypesMap {
            return ProGuardTypesMap(rules.map { ProGuardType(it.key) to ProGuardType(it.value) }.toMap())
        }
    }
}