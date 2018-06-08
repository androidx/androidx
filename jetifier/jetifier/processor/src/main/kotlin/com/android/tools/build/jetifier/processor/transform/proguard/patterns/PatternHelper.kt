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

import java.util.regex.Pattern

/**
 * Helps to build regular expression [Pattern]s defined with less verbose syntax.
 *
 * You can use following shortcuts:
 * '｟｠' - denotes a capturing group (normally '()' is capturing group)
 * '()' - denotes non-capturing group (normally (?:) is non-capturing group)
 * ' ' - denotes a whitespace characters (at least one)
 * ' *' - denotes a whitespace characters (any)
 * ';' - denotes ' *;'
 */
object PatternHelper {

    private val rewrites = listOf(
        " *" to "[\\s]*", // Optional space
        " " to "[\\s]+", // Space
        "｟" to "(", // Capturing group start
        "｠" to ")", // Capturing group end
        ";" to "[\\s]*;" // Allow spaces in front of ';'
    )

    /**
     * Transforms the given [toReplace] according to the rules defined in documentation of this
     * class and compiles it to a [Pattern].
     */
    fun build(toReplace: String, flags: Int = 0): Pattern {
        var result = toReplace
        result = result.replace("(?<!\\\\)\\(".toRegex(), "(?:")
        rewrites.forEach { result = result.replace(it.first, it.second) }
        return Pattern.compile(result, flags)
    }
}