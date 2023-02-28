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
 * The property which describes an Enum parameter for {@code ActionCapability}.
 */
class EnumProperty<EnumT : Enum<EnumT>> internal constructor(
    override val possibleValues: List<EnumT>,
    override val isRequired: Boolean,
    override val isValueMatchRequired: Boolean,
    val enumType: Class<EnumT>,
) : ParamProperty<EnumT> {
    override val isProhibited = false

    /**
     * Builder for {@link EnumProperty}.
     */
    class Builder<EnumT : Enum<EnumT>>(
        private val enumType: Class<EnumT>,
    ) {
        private val possibleValues = mutableListOf<EnumT>()
        private var isValueMatchRequired = false
        private var isRequired = false

        /**
         * Adds all app supported entity for this enum parameter. If any supported enum value is
         * added
         * then the entity matched is reuqired.
         *
         * @param supportedEnumValues supported enum values.
         */
        fun addSupportedEnumValues(vararg supportedEnumValues: EnumT) = apply {
            this.possibleValues.addAll(supportedEnumValues)
            this.isValueMatchRequired = true
        }
        fun setRequired(isRequired: Boolean) = apply {
            this.isRequired = isRequired
        }

        /** Builds the property for this Enum parameter. */
        fun build() = EnumProperty<EnumT>(
            possibleValues.toList(),
            isRequired,
            isValueMatchRequired,
            enumType,
        )
    }
}
