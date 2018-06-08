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

import com.android.tools.build.jetifier.processor.transform.proguard.patterns.GroupsReplacer
import com.android.tools.build.jetifier.processor.transform.proguard.patterns.PatternHelper
import java.util.regex.Pattern

/**
 * Parses and rewrites ProGuard rules that contain class filters. See ProGuard documentation
 * https://www.guardsquare.com/en/proguard/manual/usage#filters
 */
class ProGuardClassFilterParser(private val mapper: ProGuardTypesMapper) {

    companion object {
        private const val RULES = "(adaptclassstrings|dontnote|dontwarn)"
    }

    val replacer = GroupsReplacer(
        pattern = PatternHelper.build("^ *-$RULES ｟[^-]+｠ *$", Pattern.MULTILINE),
        groupsMap = listOf(
            { filter: String -> listOf(rewriteClassFilter(filter)) }
        )
    )

    private fun rewriteClassFilter(classFilter: String): String {
        return classFilter
            .splitToSequence(",")
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { replaceTypeInClassFilter(it) }
            .flatten()
            .distinct()
            .joinToString(separator = ", ")
    }

    /**
     * Given a package name matcher that matches several pre-renamed class names, returns several
     * package name matches that collectively match all of the possible pos-renamed names of those
     * classes.
     */
    private fun replaceTypeInClassFilter(type: String): List<String> {
        if (!type.startsWith('!')) {
            return mapper.replaceType(type)
        }

        val withoutNegation = type.substring(1, type.length)
        return mapper.replaceType(withoutNegation)
            .map { '!' + it }
            .toList()
    }
}