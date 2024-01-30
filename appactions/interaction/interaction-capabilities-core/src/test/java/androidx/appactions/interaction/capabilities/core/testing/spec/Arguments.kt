/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.testing.spec

class Arguments internal constructor(
    val requiredStringField: String?,
    val optionalStringField: String?,
    val repeatedStringField: List<String>
) {
    override fun toString(): String {
        return "Arguments(requiredStringField=$requiredStringField, " +
            "optionalStringField=$optionalStringField, " +
            "repeatedStringField=$repeatedStringField, " +
            ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Arguments

        if (requiredStringField != other.requiredStringField) return false
        if (optionalStringField != other.optionalStringField) return false
        if (repeatedStringField != other.repeatedStringField) return false
        return true
    }

    override fun hashCode(): Int {
        var result = requiredStringField.hashCode()
        result += 31 * optionalStringField.hashCode()
        result += 31 * repeatedStringField.hashCode()
        return result
    }

    class Builder {
        private var requiredStringField: String? = null
        private var optionalStringField: String? = null
        private var repeatedStringField: List<String> = listOf()

        fun setRequiredStringField(requiredStringField: String): Builder =
            apply { this.requiredStringField = requiredStringField }

        fun setOptionalStringField(optionalStringField: String): Builder =
            apply { this.optionalStringField = optionalStringField }

        fun setRepeatedStringField(repeatedStringField: List<String>): Builder =
            apply { this.repeatedStringField = repeatedStringField }

        fun build(): Arguments =
            Arguments(requiredStringField, optionalStringField, repeatedStringField)
    }
}
