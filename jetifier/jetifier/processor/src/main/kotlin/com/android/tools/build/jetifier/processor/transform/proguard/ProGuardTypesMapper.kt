/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.proguard

import com.android.tools.build.jetifier.core.proguard.ProGuardType
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.build.jetifier.processor.cartesianProduct
import com.android.tools.build.jetifier.processor.transform.TransformationContext

/**
 * Maps ProGuard types using [TypesMap] and [ProGuardTypesMap].
 */
class ProGuardTypesMapper(private val context: TransformationContext) {

    companion object {
        const val TAG = "ProGuardTypesMapper"

        val INNER_SUFFIXES = listOf("$*", "$**")
    }

    private val config = context.config

    /**
     * Replaces the given ProGuard type that was parsed from the ProGuard file (thus having '.' as
     * a separator.
     */
    fun replaceType(typeToReplace: String): List<String> {
        val type = ProGuardType.fromDotNotation(typeToReplace)
        if (type.isTrivial()) {
            return listOf(typeToReplace)
        }

        val javaType = type.toJavaType()
        if (javaType != null) {
            val result = context.typeRewriter.rewriteType(javaType)
            if (result != null) {
                return listOf(result.toDotNotation())
            }

            context.reportNoProGuardMappingFoundFailure(TAG, javaType.toString())
            return listOf(typeToReplace)
        }

        // Type contains wildcards - try custom rules map
        val result = config.proGuardMap.mapType(type)
        if (result != null) {
            Log.i(TAG, "map: %s -> %s", type, result.joinToString(", "))
            return result.map { it.toDotNotation() }.toList()
        }

        // Check fox simple suffix
        for (innerSuffix in INNER_SUFFIXES) {
            if (!typeToReplace.endsWith(innerSuffix)) {
                continue
            }
            // Try to replace without suffix
            val strippedType = ProGuardType.fromDotNotation(typeToReplace.removeSuffix(innerSuffix))
            val strippedJavaType = strippedType.toJavaType()
            if (strippedJavaType != null) {
                val result = context.typeRewriter.rewriteType(strippedJavaType)
                if (result != null) {
                    val newType = result.toDotNotation() + innerSuffix
                    Log.i(TAG, "map: %s -> %s", typeToReplace, newType)
                    return listOf(newType)
                }
            }
        }

        val results = tryResolveWildcardsAndRemapAndSimplify(type)
        if (results != null) {
            Log.i(TAG, "guessed: %s -> %s", typeToReplace, results.joinToString(","))
            return results.toList()
        }

        // Report error only when we are sure
        if (config.isEligibleForRewrite(type)) {
            context.reportNoProGuardMappingFoundFailure(TAG, type.toString())
        }
        return listOf(typeToReplace)
    }

    /**
     * Solver that takes the given ProGuard selector and runs it on all the types we have in
     * the map. The given subset is then mapped to the new world (androidx usually). From that we
     * then try to generate a shortest possible rule that covers the new set.
     *
     * Example:
     * 1) For given android.support.annotation.** we would generate a set like:
     * "android/support/annotation/AnimRes"
     * "android/support/annotation/AnimatorRes"
     * "android/support/annotation/AnyRes"
     * ... and others
     *
     * 2) Based on the rules such set is then mapped to:
     * "androidx/annotation/AnimRes"
     * "androidx/annotation/AnimatorRes"
     * "androidx/annotation/AnyRes"
     * ... and others
     *
     * 3) We then take each type and try to generate a following set of rules:
     *
     * a) "androidx/annotation/AnimRes*" - this matches only one instance
     * b) "androidx/annotation/Anim*"- this matches more cases
     * c) "androidx/annotation/\**" - this matches the whole set!
     * d) "androidx/\**" - this matches more than we need -> fallback to c)
     */
    private fun tryResolveWildcardsAndRemapAndSimplify(typeToReplace: ProGuardType): Set<String>? {
        val setToMatch = config.typesMap.matchOldProguardForNewTypes(typeToReplace)
        if (setToMatch.isEmpty()) {
            return null
        }

        if (setToMatch.size == 1) {
            // The selector matches just one type so map it directly to that type
            val suffixesToAppend = listOf("*", "**")
            for (suffix in suffixesToAppend) {
                if (typeToReplace.value.endsWith(suffix)) {
                    return setOf(setToMatch.single().toDotNotation() + suffix)
                }
            }

            return setOf(setToMatch.single().toDotNotation())
        }

        val prefixes = mutableSetOf<String>()
        val newSet = mutableSetOf<String>()

        setToMatch.forEach {
            type -> run {
                if (prefixes.any { type.fullName.startsWith(it) }) {
                    // Type already covered
                    return@run
                }

                var candidate: String? = null
                var candidatePrefix: String? = null

                val selectors = generateProguardRulesFromType(type)

                for (selector in selectors) {
                    var selectorPrefix = selector.replace("*", "")

                    val foundSet = config.typesMap.findAllTypesPrefixedWith(selectorPrefix)

                    if (setToMatch.size >= foundSet.size && setToMatch.containsAll(foundSet)) {
                        // We got a candidate
                        candidate = selector
                        candidatePrefix = selectorPrefix
                        if (setToMatch.size == foundSet.size) {
                            break
                        }
                    } else {
                        break
                    }
                }

                if (candidate != null) {
                    prefixes.add(candidatePrefix!!)
                    newSet.add(candidate.replace('/', '.'))
                } else {
                    // We failed
                    return null
                }
            }
        }

        Log.v(TAG, "Guessed %s to be %s", typeToReplace.value, newSet.joinToString(","))
        return newSet
    }

    /**
     * Generates all possible places where to put ** wildcard to create a general ProGuard rule
     * from the given type.
     *
     * E.g. for android/support/HelloWorld generates a list in the following order:
     * android.support.HelloWorld*
     * android.support.Hello*
     * android.support.**
     * android.**
     */
    private fun generateProguardRulesFromType(type: JavaType): List<String> {
        val result = mutableListOf<String>()
        var lastSegment = ""

        // Generate from packages
        type.fullName
            .split("/")
            .dropLast(1) // drops class name
            .forEach { lastSegment += "$it/"; result.add(lastSegment + "**") }

        // Generate from the class name
        type.fullName
            .substringAfterLast("/") // grabs class name
            .split("(?=\\p{Upper})".toRegex())
            .forEach { lastSegment += it; result.add(lastSegment + "*") }

        result.reverse()
        return result
    }

    /**
     * Replaces the given arguments list used in a ProGuard method rule. Argument must be separated
     * with ','. The method also accepts '...' symbol as defined in the spec.
     */
    fun replaceMethodArgs(argsTypes: String): List<String> {
        if (argsTypes.isEmpty() || argsTypes == "...") {
            return listOf(argsTypes)
        }

        return argsTypes
            .splitToSequence(",")
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { replaceType(it) }
            .toList()
            .cartesianProduct()
            .map { it.joinToString(separator = ", ") }
            .toList()
    }
}