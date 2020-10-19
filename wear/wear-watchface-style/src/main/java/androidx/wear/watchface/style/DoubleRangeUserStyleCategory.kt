/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.style

import android.graphics.drawable.Icon
import androidx.annotation.RestrictTo
import androidx.wear.watchface.style.data.DoubleRangeUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.DoubleRangeUserStyleCategoryWireFormat.DoubleRangeOptionWireFormat

/**
 * A DoubleRangeUserStyleCategory represents a category with a [Double] value in the range
 * `[minimumValue .. maximumValue]`.
 */
public class DoubleRangeUserStyleCategory : UserStyleCategory {

    internal companion object {
        internal fun createOptionsList(
            minimumValue: Double,
            maximumValue: Double,
            defaultValue: Double
        ): List<DoubleRangeOption> {
            require(minimumValue < maximumValue)
            require(defaultValue >= minimumValue)
            require(defaultValue <= maximumValue)

            return if (defaultValue != minimumValue && defaultValue != maximumValue) {
                listOf(
                    DoubleRangeOption(minimumValue),
                    DoubleRangeOption(defaultValue),
                    DoubleRangeOption(maximumValue)
                )
            } else {
                listOf(DoubleRangeOption(minimumValue), DoubleRangeOption(maximumValue))
            }
        }
    }

    public constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the userStyle selection UI. */
        displayName: CharSequence,

        /** Localized description string displayed under the displayName. */
        description: CharSequence,

        /** Icon for use in the userStyle selection UI. */
        icon: Icon?,

        /** Minimum value (inclusive). */
        minimumValue: Double,

        /** Maximum value (inclusive). */
        maximumValue: Double,

        /** The default value for this DoubleRangeUserStyleCategory. */
        defaultValue: Double,

        /**
         * Used by the style configuration UI. Describes which rendering layers this style affects.
         */
        affectsLayers: Collection<Layer>
    ) : super(
        id,
        displayName,
        description,
        icon,
        createOptionsList(minimumValue, maximumValue, defaultValue),
        // The index of defaultValue can only ever be 0 or 1.
        when (defaultValue) {
            minimumValue -> 0
            else -> 1
        },
        affectsLayers
    )

    internal constructor(wireFormat: DoubleRangeUserStyleCategoryWireFormat) : super(wireFormat)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toWireFormat(): DoubleRangeUserStyleCategoryWireFormat =
        DoubleRangeUserStyleCategoryWireFormat(
            id,
            displayName,
            description,
            icon,
            getWireFormatOptionsList(),
            defaultOptionIndex,
            affectsLayers.map { it.ordinal }
        )

    /**
     * Represents an option as a [Double] in the range [minimumValue .. maximumValue].
     */
    public class DoubleRangeOption : Option {
        /* The value for this option. Must be within the range [minimumValue .. maximumValue]. */
        public val value: Double

        public constructor(value: Double) : super(value.toString()) {
            this.value = value
        }

        internal companion object {
            internal const val KEY_DOUBLE_VALUE = "KEY_DOUBLE_VALUE"
        }

        internal constructor(
            wireFormat: DoubleRangeUserStyleCategoryWireFormat.DoubleRangeOptionWireFormat
        ) : super(wireFormat.mId) {
            value = wireFormat.mValue
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): DoubleRangeOptionWireFormat =
            DoubleRangeOptionWireFormat(id, value)
    }

    /**
     * Returns the minimum value.
     */
    public fun getMinimumValue(): Double = (options.first() as DoubleRangeOption).value

    /**
     * Returns the maximum value.
     */
    public fun getMaximumValue(): Double = (options.last() as DoubleRangeOption).value

    /**
     * Returns the default value.
     */
    public fun getDefaultValue(): Double = (options[defaultOptionIndex] as DoubleRangeOption).value

    /**
     * We support all values in the range [min ... max] not just min & max.
     */
    override fun getOptionForId(optionId: String): Option =
        options.find { it.id == optionId } ?: checkedOptionForId(optionId)

    private fun checkedOptionForId(optionId: String): DoubleRangeOption {
        return try {
            val value = optionId.toDouble()
            if (value < getMinimumValue() || value > getMaximumValue()) {
                options[defaultOptionIndex] as DoubleRangeOption
            } else {
                DoubleRangeOption(value)
            }
        } catch (e: NumberFormatException) {
            options[defaultOptionIndex] as DoubleRangeOption
        }
    }
}
