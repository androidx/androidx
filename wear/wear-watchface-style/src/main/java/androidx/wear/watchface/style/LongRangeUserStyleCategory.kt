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
import androidx.wear.watchface.style.data.LongRangeUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.LongRangeUserStyleCategoryWireFormat.LongRangeOptionWireFormat

/**
 * A LongRangeUserStyleCategory represents a category with a [Long] value in the range
 * [minimumValue .. maximumValue].
 */
public class LongRangeUserStyleCategory : UserStyleCategory {

    internal companion object {
        internal fun createOptionsList(
            minimumValue: Long,
            maximumValue: Long,
            defaultValue: Long
        ): List<LongRangeOption> {
            require(minimumValue < maximumValue)
            require(defaultValue >= minimumValue)
            require(defaultValue <= maximumValue)

            return if (defaultValue != minimumValue && defaultValue != maximumValue) {
                listOf(
                    LongRangeOption(minimumValue),
                    LongRangeOption(defaultValue),
                    LongRangeOption(maximumValue)
                )
            } else {
                listOf(
                    LongRangeOption(minimumValue),
                    LongRangeOption(maximumValue)
                )
            }
        }
    }

    public constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the userStyle selection UI. */
        displayName: String,

        /** Localized description string displayed under the displayName. */
        description: String,

        /** Icon for use in the userStyle selection UI. */
        icon: Icon?,

        /** Minimum value (inclusive). */
        minimumValue: Long,

        /** Maximum value (inclusive). */
        maximumValue: Long,

        /** The default value for this LongRangeUserStyleCategory. */
        defaultValue: Long,

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

    internal constructor(wireFormat: LongRangeUserStyleCategoryWireFormat) : super(wireFormat)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toWireFormat(): LongRangeUserStyleCategoryWireFormat =
        LongRangeUserStyleCategoryWireFormat(
            id,
            displayName,
            description,
            icon,
            getWireFormatOptionsList(),
            defaultOptionIndex,
            affectsLayers.map { it.ordinal }
        )

    /**
     * Represents an option a [Long] in the range [minimumValue .. maximumValue].
     */
    public class LongRangeOption : Option {
        /* The value for this option. Must be within the range [minimumValue..maximumValue]. */
        public val value: Long

        public constructor(value: Long) : super(value.toString()) {
            this.value = value
        }

        internal companion object {
            internal const val KEY_LONG_VALUE = "KEY_LONG_VALUE"
        }

        internal constructor(wireFormat: LongRangeOptionWireFormat) : super(wireFormat.mId) {
            value = wireFormat.mValue
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): LongRangeOptionWireFormat =
            LongRangeOptionWireFormat(id, value)
    }

    /**
     * Returns the minimum value.
     */
    public fun getMinimumValue(): Long = (options.first() as LongRangeOption).value

    /**
     * Returns the maximum value.
     */
    public fun getMaximumValue(): Long = (options.last() as LongRangeOption).value

    /**
     * Returns the default value.
     */
    public fun getDefaultValue(): Long = (options[defaultOptionIndex] as LongRangeOption).value

    /**
     * We support all values in the range [min ... max] not just min & max.
     */
    override fun getOptionForId(optionId: String): Option =
        options.find { it.id == optionId } ?: checkedOptionForId(optionId)

    private fun checkedOptionForId(optionId: String): LongRangeOption {
        return try {
            val value = optionId.toLong()
            if (value < getMinimumValue() || value > getMaximumValue()) {
                options[defaultOptionIndex] as LongRangeOption
            } else {
                LongRangeOption(value)
            }
        } catch (e: NumberFormatException) {
            options[defaultOptionIndex] as LongRangeOption
        }
    }
}
