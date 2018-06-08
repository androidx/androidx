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

package com.android.tools.build.jetifier.processor.transform.proguard.patterns

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Applies replacements on a matched string using the given [pattern] and its groups. Each group is
 * mapped using a lambda from [groupsMap]. If there is more results for each group it expands into
 * all possible replacements (Cartesian product) and returns multiple results.
 */
class GroupsReplacer(
        val pattern: Pattern,
        private val groupsMap: List<(String) -> List<String>>
) {

    /**
     * Takes the given [matcher] and replace its matched groups using mapping functions given in
     * [groupsMap].
     */
    fun runReplacements(matcher: Matcher): List<String> {
        val start = matcher.group(0)

        var results = mutableListOf<String>(start)
        var tempResults = mutableListOf<String>()

        // For each group, apply the corresponding replacement. Iterate backwards over replacements
        // to avoid having to recompute replacement string indexes, also we could replace something
        // twice
        for (i in groupsMap.size - 1 downTo 0) {
            val groupVal = matcher.group(i + 1) ?: continue
            val localStart = matcher.start(i + 1) - matcher.start()
            val localEnd = matcher.end(i + 1) - matcher.start()

            // Call the corresponding replacer for this group
            val replacements = groupsMap[i].invoke(groupVal)

            // Update the Cartesian product, copying each existing element of results and replacing
            // groupVal in each with a different element of replacements
            tempResults.clear()
            results.forEach {
                result -> replacements.forEach {
                    tempResults.add(
                        // Because we iterate in reverse, we know the index of groupVal in result
                        // will be the same as the index of groupVal in start
                        result.replaceRange(
                            startIndex = localStart,
                            endIndex = localEnd,
                            replacement = it
                        )
                    )
                }
            }

            results = tempResults.also { tempResults = results } // Swap
        }
        return results.toList()
    }
}