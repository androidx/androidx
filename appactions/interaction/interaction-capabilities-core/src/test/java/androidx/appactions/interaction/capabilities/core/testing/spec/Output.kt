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

class Output internal constructor(
    val optionalStringField: String?,
    val repeatedStringField: List<String>
) {
    override fun toString(): String {
        return "Output(optionalStringField=$optionalStringField, " +
            "repeatedStringField=$repeatedStringField)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Output

        if (optionalStringField != other.optionalStringField) return false
        if (repeatedStringField != other.repeatedStringField) return false
        return true
    }

    override fun hashCode(): Int {
        var result = optionalStringField.hashCode()
        result += 31 * repeatedStringField.hashCode()
        return result
    }

    class Builder {
        private var optionalStringField: String? = null
        private var repeatedStringField: List<String> = listOf()

        fun setOptionalStringField(optionalStringField: String): Builder =
            apply { this.optionalStringField = optionalStringField }

        fun setRepeatedStringField(repeatedStringField: List<String>): Builder = apply {
            this.repeatedStringField = repeatedStringField
        }

        fun build(): Output = Output(optionalStringField, repeatedStringField)
    }
}