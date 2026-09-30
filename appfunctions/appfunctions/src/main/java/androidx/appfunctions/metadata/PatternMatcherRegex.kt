/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.metadata

import android.annotation.SuppressLint
import android.os.PatternMatcher

/**
 * Merges these matchers into a single regular expression, joined as alternatives, that matches a
 * value iff any of the matchers matches it.
 *
 * Returns null if the list is empty or contains a matcher whose type this library version cannot
 * convert (for example, a type added in a newer SDK), as if there were no pattern.
 */
@SuppressLint("InlinedApi") // AppFunctions requires API 34+.
internal fun List<PatternMatcher>.toRegexPatternOrNull(): String? {
    val alternatives = map { matcher ->
        when (matcher.type) {
            PatternMatcher.PATTERN_LITERAL -> "^${escapeRegex(matcher.path)}$"
            PatternMatcher.PATTERN_PREFIX -> "^${escapeRegex(matcher.path)}"
            PatternMatcher.PATTERN_SUFFIX -> "${escapeRegex(matcher.path)}$"
            PatternMatcher.PATTERN_SIMPLE_GLOB -> "^${simpleGlobToRegex(matcher.path)}$"
            PatternMatcher.PATTERN_ADVANCED_GLOB -> "^${advancedGlobToRegex(matcher.path)}$"
            // Unknown to this library version, so there is no equivalent regex.
            else -> return null
        }
    }
    return when (alternatives.size) {
        0 -> null
        1 -> alternatives.single()
        else -> alternatives.joinToString("|") { "(?:$it)" }
    }
}

private const val REGEX_SPECIAL_CHARS = "\\^$.|?*+()[]{}/"

private fun escapeRegex(value: String): String = buildString {
    value.forEach { appendEscapedRegexChar(it) }
}

private fun StringBuilder.appendEscapedRegexChar(c: Char) {
    if (c in REGEX_SPECIAL_CHARS) append('\\')
    append(c)
}

/**
 * Converts a [PatternMatcher.PATTERN_SIMPLE_GLOB]: `.` matches any character, `*` repeats the
 * preceding character zero or more times, and `\` escapes the next character.
 */
private fun simpleGlobToRegex(glob: String): String = buildString {
    var i = 0
    while (i < glob.length) {
        val c = glob[i]
        when {
            c == '\\' && i + 1 < glob.length -> appendEscapedRegexChar(glob[++i])
            c == '.' -> append('.')
            c == '*' && i > 0 -> append('*')
            else -> appendEscapedRegexChar(c)
        }
        i++
    }
}

/**
 * Converts a [PatternMatcher.PATTERN_ADVANCED_GLOB]. Its `.`, `*`, `+`, `{m,n}` and `[...]` syntax
 * matches regex syntax, `\` escapes the next character literally, and every other character is a
 * literal.
 */
private fun advancedGlobToRegex(glob: String): String = buildString {
    var i = 0
    var inCharClass = false
    while (i < glob.length) {
        val c = glob[i]
        when {
            c == '\\' && i + 1 < glob.length -> appendEscapedRegexChar(glob[++i])
            inCharClass -> {
                when (c) {
                    ']' -> {
                        inCharClass = false
                        append(c)
                    }
                    '-' -> append(c)
                    '^' -> if (glob[i - 1] == '[') append(c) else append("\\^")
                    else -> appendEscapedRegexChar(c)
                }
            }
            c == '[' -> {
                inCharClass = true
                append(c)
            }
            c in ".*+{},0123456789" -> append(c)
            else -> appendEscapedRegexChar(c)
        }
        i++
    }
}
