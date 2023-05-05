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

import androidx.appactions.interaction.capabilities.core.impl.BuilderOf

class GenericEntityArguments internal constructor(
    val singularField: TestEntity?,
    val optionalField: TestEntity?,
    val repeatedField: List<TestEntity>
) {
    override fun toString(): String {
        return "GenericEntityArguments(singularField=$singularField, " +
            "optionalField=$optionalField, " +
            "repeatedField=$repeatedField, " +
            ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GenericEntityArguments

        if (singularField != other.singularField) return false
        if (optionalField != other.optionalField) return false
        if (repeatedField != other.repeatedField) return false
        return true
    }

    override fun hashCode(): Int {
        var result = singularField.hashCode()
        result += 31 * optionalField.hashCode()
        result += 31 * repeatedField.hashCode()
        return result
    }

    class Builder : BuilderOf<GenericEntityArguments> {
        private var singularField: TestEntity? = null
        private var optionalField: TestEntity? = null
        private var repeatedField: List<TestEntity> = listOf()

        fun setSingularField(singularField: TestEntity): Builder =
            apply { this.singularField = singularField }

        fun setOptionalField(optionalField: TestEntity): Builder =
            apply { this.optionalField = optionalField }

        fun setRepeatedField(repeatedField: List<TestEntity>): Builder =
            apply { this.repeatedField = repeatedField }

        override fun build(): GenericEntityArguments =
            GenericEntityArguments(singularField, optionalField, repeatedField)
    }
}