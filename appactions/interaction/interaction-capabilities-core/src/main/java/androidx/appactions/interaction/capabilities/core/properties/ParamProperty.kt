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

/**
 * Configure parameters for the capability such as providing possible values of some type, or
 * marking a parameter as required for execution.
 */
class ParamProperty<T>
internal constructor(
    private val possibleValueSupplier: () -> List<T>,
    /** Indicates that a value for this property is required to be present for fulfillment. */
    @get:JvmName("isRequired")
    val isRequired: Boolean,
    /**
     * Indicates that a match of possible value for the given property must be present. Defaults to
     * false.
     *
     * <p>If true, Assistant skips the capability if there is no match.
     */
    @get:JvmName("isValueMatchRequired")
    val isValueMatchRequired: Boolean,
    /**
     * If true, the {@code Capability} will be rejected by assistant if corresponding param is set
     * in argument. And the value of |isRequired| and |entityMatchRequired| will also be ignored by
     * assistant.
     */
    @get:JvmName("isProhibited")
    val isProhibited: Boolean,
) {
    /** The current list of possible values for this parameter, can change over time. */
    val possibleValues: List<T>
        get() = possibleValueSupplier()

    /** Builder for {@link ParamProperty}. */
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
        fun setRequired(required: Boolean) = apply { this.isRequired = required }

        /**
         * Sets whether or not this property requires that the value for this property must match
         * one of the Entity in the defined possible entities.
         */
        fun setValueMatchRequired(valueMatchRequired: Boolean) = apply {
            this.isValueMatchRequired = valueMatchRequired
        }

        /**
         * Sets whether this property is prohibited in the response.
         *
         * @param prohibited Whether this property is prohibited in the response.
         */
        fun setProhibited(prohibited: Boolean) = apply { this.isProhibited = prohibited }

        /** Builds the property for this entity parameter. */
        fun build() =
            ParamProperty(
                this.possibleValueSupplier,
                this.isRequired,
                this.isValueMatchRequired,
                this.isProhibited,
            )
    }
}
