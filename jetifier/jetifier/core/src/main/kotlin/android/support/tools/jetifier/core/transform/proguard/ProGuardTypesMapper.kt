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

import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.utils.Log

/**
 * Maps ProGuard types using [TypesMap] and [ProGuardTypesMap].
 */
class ProGuardTypesMapper(private val context: TransformationContext) {

    companion object {
        const val TAG = "ProGuardTypesMapper"
    }

    private val config = context.config

    /**
     * Replaces the given ProGuard type that was parsed from the ProGuard file (thus having '.' as
     * a separator.
     */
    fun replaceType(typeToReplace: String) : String {
        val type = ProGuardType.fromDotNotation(typeToReplace)
        if (type.isTrivial()) {
            return typeToReplace
        }

        val javaType = type.toJavaType()
        if (javaType != null) {
            // We are dealing with an explicit type definition
            if (!context.isEligibleForRewrite(javaType)) {
                return typeToReplace
            }

            val result = config.typesMap.types[javaType]
            if (result == null) {
                context.reportNoProGuardMappingFoundFailure()
                Log.e(TAG, "No mapping for: " + type)
                return typeToReplace
            }

            Log.i(TAG, "  map: %s -> %s", type, result)
            return result.toDotNotation()
        }

        // Type contains wildcards - try custom rules map
        val result = config.proGuardMap.rules[type]
        if (result != null) {
            Log.i(TAG, "  map: %s -> %s", type, result)
            return result.toDotNotation()
        }

        // Report error only when we are sure
        if (context.isEligibleForRewrite(type)) {
            context.reportNoProGuardMappingFoundFailure()
            Log.e(TAG, "No mapping for: " + type)
        }
        return typeToReplace
    }

    /**
     * Replaces the given arguments list used in a ProGuard method rule. Argument must be separated
     * with ','. The method also accepts '...' symbol as defined in the spec.
     */
    fun replaceMethodArgs(argsTypes: String) : String {
        if (argsTypes.isEmpty() || argsTypes == "...") {
            return argsTypes
        }

        return argsTypes
            .splitToSequence(",")
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { replaceType(it) }
            .joinToString(separator = ", ")
    }
}