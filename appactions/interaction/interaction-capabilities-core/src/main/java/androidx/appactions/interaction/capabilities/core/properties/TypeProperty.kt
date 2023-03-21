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

package androidx.appactions.interaction.capabilities.core.properties

/** The property which describes a complex type. */
class TypeProperty<T> internal constructor(
    private val possibleValueSupplier: () -> List<T>,
    override val isRequired: Boolean,
    override val isValueMatchRequired: Boolean,
    override val isProhibited: Boolean,
) : ParamProperty<T> {
    override val possibleValues: List<T>
        get() = possibleValueSupplier()

    /** Builder for {@link TypeProperty}. */
    class Builder<T> {
        private var possibleValueSupplier: () -> List<T> = { emptyList<T>() }
        private var isRequired = false
        private var isValueMatchRequired = false
        private var isProhibited = false

        /**
         * Sets one or more possible values for this parameter.
         *
         * @param values the possible values.
         */
        fun setPossibleValues(vararg values: T) = apply {
            this.possibleValueSupplier = { values.asList() }
        }

        /**
         * Sets a supplier of possible values for this parameter.
         *
         * @param supplier the supplier of possible values.
         */
        fun setPossibleValueSupplier(supplier: () -> List<T>) = apply {
            this.possibleValueSupplier = supplier
        }

        /** Sets whether or not this property requires a value for fulfillment. */
        fun setRequired(isRequired: Boolean) = apply {
            this.isRequired = isRequired
        }

        /**
         * Sets whether or not this property requires that the value for this property must match
         * one of
         * the Entity in the defined possible entities.
         */
        fun setValueMatchRequired(isValueMatchRequired: Boolean) = apply {
            this.isValueMatchRequired = isValueMatchRequired
        }

        /**
         * Sets whether this property is prohibited in the response.
         *
         * @param isProhibited Whether this property is prohibited in the response.
         */
        fun setProhibited(isProhibited: Boolean) = apply {
            this.isProhibited = isProhibited
        }

        /** Builds the property for this entity parameter. */
        fun build() = TypeProperty(
            this.possibleValueSupplier,
            this.isRequired,
            this.isValueMatchRequired,
            this.isProhibited,
        )
    }
}
