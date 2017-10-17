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

import com.google.gson.annotations.SerializedName
import java.util.regex.Pattern

/**
 * Rule that rewrites a Java type or field based on the given arguments.
 *
 * Used in the preprocessor when generating [TypesMap].
 *
 * @param from Regular expression where packages are separated via '/' and inner class separator
 * is "$". Used to match the input type.
 * @param to A string to be used as a replacement if the 'from' pattern is matched. It can also
 * apply groups matched from the original pattern using {x} annotation, e.g. {0}.
 * @param fieldSelectors Collection of regular expressions that are used to match fields. If the
 * type is matched (using 'from') and the field is matched (or the list of fields selectors is
 * empty) the field's type gets rewritten according to the 'to' parameter.
 */
class RewriteRule(
        private val from: String,
        private val to: String,
        private val fieldSelectors: List<String> = emptyList()) {

    // We escape '$' so we don't conflict with regular expression symbols.
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
        return "$inputPattern -> $outputPattern " + fields.joinToString { it.toString() }
    }

    /** Returns JSON data model of this class */
    fun toJson() : JsonData {
        return JsonData(from, to, fieldSelectors)
    }


    /**
     * JSON data model for [RewriteRule].
     */
    data class JsonData(
            @SerializedName("from")
            val from: String,

            @SerializedName("to")
            val to: String,

            @SerializedName("fieldSelectors")
            val fieldSelectors: List<String>? = null)  {

        /** Creates instance of [RewriteRule] */
        fun toRule() : RewriteRule {
            return RewriteRule(from, to, fieldSelectors.orEmpty())
        }
    }

}


