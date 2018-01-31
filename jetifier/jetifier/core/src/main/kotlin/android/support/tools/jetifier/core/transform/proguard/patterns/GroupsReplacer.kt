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

package android.support.tools.jetifier.core.transform.proguard.patterns

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Applies replacements on a matched string using the given [pattern] and its groups. Each group is
 * mapped using a lambda from [groupsMap].
 */
class GroupsReplacer(val pattern: Pattern,
                     private val groupsMap: List<(String) -> String>) {

    /**
     * Takes the given [matcher] and replace its matched groups using mapping functions given in
     * [groupsMap].
     */
    fun runReplacements(matcher: Matcher) : String {
        var result = matcher.group(0)

        // We go intentionally backwards to replace using indexes
        for (i in groupsMap.size - 1 downTo 0) {
            val groupVal = matcher.group(i + 1) ?: continue
            val localStart = matcher.start(i + 1) - matcher.start()
            val localEnd =  matcher.end(i + 1) - matcher.start()

            result = result.replaceRange(
                startIndex = localStart,
                endIndex = localEnd,
                replacement = groupsMap[i].invoke(groupVal))
        }
        return result
    }

}