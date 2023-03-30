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
 * The property which describes a parameter with String or Enum entity for {@code ActionCapability}.
 */
class StringOrEnumProperty<EnumT : Enum<EnumT>> internal constructor(
    override val possibleValues: List<StringOrEnumProperty.PossibleValue<EnumT>>,
    override val isRequired: Boolean,
    override val isValueMatchRequired: Boolean,
    val enumType: Class<EnumT>,
) : ParamProperty<StringOrEnumProperty.PossibleValue<EnumT>> {

    override val isProhibited = false

    /**
     * Represents a single possible value in StringOrEnumProperty.
     *
     * @param kind The Kind of PossibleValue.
     * @param stringValue the StringProperty.PossibleValue, corresponds to Kind.ENUM_VALUE.
     * @param enumValue
     */
    class PossibleValue<EnumT : Enum<EnumT>> internal constructor(
        val kind: Kind,
        val stringValue: StringProperty.PossibleValue?,
        val enumValue: EnumT?,
    ) {
        /**  */
        enum class Kind {
            STRING_VALUE,
            ENUM_VALUE,
        }

        companion object {
            /** Create a new StringOrEnumProperty.PossibleValue for Kind.STRING_VALUE. */
            @JvmStatic
            fun <EnumT : Enum<EnumT>> of(
                name: String,
                vararg alternateNames: String,
            ) = PossibleValue<EnumT>(
                Kind.STRING_VALUE,
                StringProperty.PossibleValue.of(name, *alternateNames),
                null,
            )

            /** Create a new StringOrEnumProperty.PossibleValue for Kind.ENUM_VALUE. */
            @JvmStatic
            fun <EnumT : Enum<EnumT>> of(enumValue: EnumT) = PossibleValue<EnumT>(
                Kind.ENUM_VALUE,
                null,
                enumValue,
            )
        }
    }

    /**
     * Builder for {@link StringOrEnumProperty}.
     */
    class Builder<EnumT : Enum<EnumT>> (
        private val enumType: Class<EnumT>,
    ) {
        private val possibleValues = mutableListOf<PossibleValue<EnumT>>()
        private var isRequired = false
        private var isValueMatchRequired = false

        /**
         * Adds a possible string value for this property.
         *
         * @param name           the possible string value.
         * @param alternateNames the alternative names for this value.
         */

        fun addPossibleValue(
            name: String,
            vararg alternateNames: String,
        ) = apply {
            possibleValues.add(PossibleValue.of(name, *alternateNames))
            this.isValueMatchRequired = true
        }

        /**
         * Adds possible Enum values for this parameter.
         *
         * @param enumValues possible enum entity values.
         */
        fun addPossibleValues(vararg enumValues: EnumT) = apply {
            enumValues.forEach {
                possibleValues.add(PossibleValue.of(it))
            }
            this.isValueMatchRequired = true
        }

        /** Sets whether or not this property requires a value for fulfillment. */
        fun setRequired(isRequired: Boolean) = apply {
            this.isRequired = isRequired
        }

        /**
         * Sets whether matching a possible value is required for this parameter. Note that this
         * value
         * can be overrided by assistant.
         *
         * @param isValueMatchRequired whether value match is required
         */
        fun setValueMatchRequired(isValueMatchRequired: Boolean) = apply {
            this.isValueMatchRequired = isValueMatchRequired
        }

        /** Builds the property for this Entity or Enum parameter. */
        fun build() =
            StringOrEnumProperty(
                possibleValues.toList(),
                isRequired,
                isValueMatchRequired,
                enumType,
            )
    }
}
