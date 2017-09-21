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

package android.support.tools.jetifier.core.rules

import java.util.regex.Pattern

/**
 * Rule that rewrites a Java type or field based on the given arguments.
 *
 * @param from Regular expression where packages are separated via '/' and inner class separator
 * is "$". Used to match the input type.
 * @param to A string to be used as a replacement if the 'from' pattern is matched. It can also
 * apply groups matched from the original pattern using {x} annotation, e.g. {0}.
 * @param fieldSelectors Collection of regular expressions that are used to match fields. If the
 * type is matched (using 'from') and the field is matched (or the list of fields selectors is
 * empty) the field's type gets rewritten according to the 'to' parameter.
 */
class RewriteRule(from: String, to: String, fieldSelectors: List<String> = emptyList()) {

    // Escape '$' so we don't conflict with regular expression symbols.
    private val inputPattern = Pattern.compile("^${from.replace("$", "\\$")}$")
    private val outputPattern = to.replace("$", "\$")

    private val fields = fieldSelectors.map { Pattern.compile("^$it$") }

    /**
     * Rewrites the given java type. Returns null if this rule is not applicable for the given type.
     */
    fun apply(input: JavaType): JavaType? {
        if (fields.isNotEmpty()) {
            return null
        }

        return applyInternal(input)
    }

    /**
     * Rewrites the given field type. Returns null if this rule is not applicable for the given
     * type.
     */
    fun apply(inputField: JavaField) : JavaField? {
        val typeRewriteResult = applyInternal(inputField.owner) ?: return null

        val isFieldInTheFilter = fields.isEmpty()
                || fields.any { it.matcher(inputField.name).matches() }
        if (isFieldInTheFilter) {
            return inputField.renameOwner(typeRewriteResult)
        }

        return null
    }

    private fun applyInternal(input: JavaType): JavaType? {
        val matcher = inputPattern.matcher(input.fullName)
        if (!matcher.matches()) {
            return null
        }

        var result = outputPattern
        for (i in 0..matcher.groupCount() - 1) {
            result = result.replace("{$i}", matcher.group(i + 1))
        }

        return JavaType(result)
    }

    override fun toString() : String {
        // TODO: Improve
        return "$inputPattern -> $outputPattern " + fields.joinToString { it.toString() }
    }
}


