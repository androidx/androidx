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
import androidx.wear.watchface.style.data.BooleanUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.ComplicationsUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.DoubleRangeUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.ListUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.LongRangeUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.UserStyleCategoryWireFormat

/**
 * Watch faces often have user configurable styles. The definition of what is a style is left up
 * to the watch face but it typically incorporates a variety of categories such as: color,
 * visual theme for watch hands, font, tick shape, complications, audio elements, etc...
 */
public abstract class UserStyleCategory(
    /** Identifier for the element, must be unique. */
    public val id: String,

    /** Localized human readable name for the element, used in the userStyle selection UI. */
    public val displayName: String,

    /** Localized description string displayed under the displayName. */
    public val description: String,

    /** Icon for use in the style selection UI. */
    public val icon: Icon?,

    /**
     * List of options for this UserStyleCategory. Depending on the type of UserStyleCategory this
     * may be an exhaustive list, or just examples to populate a ListView in case the
     * UserStyleCategory isn't supported by the UI (e.g. a new WatchFace with an old Companion).
     */
    public val options: List<Option>,

    /**
     * The default option index, used if nothing has been selected within the [options] list.
     */
    public val defaultOptionIndex: Int,

    /**
     * Used by the style configuration UI. Describes which rendering layers this style affects.
     */
    public val affectsLayers: Collection<Layer>
) {
    public companion object {

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun createFromWireFormat(
            wireFormat: UserStyleCategoryWireFormat
        ): UserStyleCategory = when (wireFormat) {
            is BooleanUserStyleCategoryWireFormat -> BooleanUserStyleCategory(wireFormat)

            is ComplicationsUserStyleCategoryWireFormat ->
                ComplicationsUserStyleCategory(wireFormat)

            is DoubleRangeUserStyleCategoryWireFormat ->
                DoubleRangeUserStyleCategory(wireFormat)

            is ListUserStyleCategoryWireFormat -> ListUserStyleCategory(wireFormat)

            is LongRangeUserStyleCategoryWireFormat -> LongRangeUserStyleCategory(wireFormat)

            else -> throw IllegalArgumentException(
                "Unknown StyleCategoryWireFormat " + wireFormat::javaClass.name
            )
        }
    }

    init {
        require(defaultOptionIndex >= 0 && defaultOptionIndex < options.size) {
            "defaultOptionIndex must be in the range [0 .. options.size)"
        }
    }

    internal fun getCategoryOptionForId(id: String?) =
        if (id == null) {
            options[defaultOptionIndex]
        } else {
            getOptionForId(id)
        }

    internal constructor(wireFormat: UserStyleCategoryWireFormat) : this(
        wireFormat.mId,
        wireFormat.mDisplayName,
        wireFormat.mDescription,
        wireFormat.mIcon,
        wireFormat.mOptions.map { Option.createFromWireFormat(it) },
        wireFormat.mDefaultOptionIndex,
        wireFormat.mAffectsLayers.map { Layer.values()[it] }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public abstract fun toWireFormat(): UserStyleCategoryWireFormat

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun getWireFormatOptionsList(): List<UserStyleCategoryWireFormat.OptionWireFormat> =
        options.map { it.toWireFormat() }

    /** Returns the default for when the user hasn't selected an option. */
    public fun getDefaultOption(): Option = options[defaultOptionIndex]

    /**
     * Represents a choice within a style category.
     *
     * @property id Machine readable identifier for the style setting.
     */
    public abstract class Option(
        /** Identifier for the option, must be unique within the UserStyleCategory. */
        public val id: String
    ) {
        public companion object {

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            public fun createFromWireFormat(
                wireFormat: UserStyleCategoryWireFormat.OptionWireFormat
            ): Option =
                when (wireFormat) {
                    is BooleanUserStyleCategoryWireFormat.BooleanOptionWireFormat ->
                        BooleanUserStyleCategory.BooleanOption(wireFormat)

                    is ComplicationsUserStyleCategoryWireFormat.ComplicationsOptionWireFormat ->
                        ComplicationsUserStyleCategory.ComplicationsOption(wireFormat)

                    is DoubleRangeUserStyleCategoryWireFormat.DoubleRangeOptionWireFormat ->
                        DoubleRangeUserStyleCategory.DoubleRangeOption(wireFormat)

                    is ListUserStyleCategoryWireFormat.ListOptionWireFormat ->
                        ListUserStyleCategory.ListOption(wireFormat)

                    is LongRangeUserStyleCategoryWireFormat.LongRangeOptionWireFormat ->
                        LongRangeUserStyleCategory.LongRangeOption(wireFormat)

                    else -> throw IllegalArgumentException(
                        "Unknown StyleCategoryWireFormat.OptionWireFormat " +
                            wireFormat::javaClass.name
                    )
                }
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public abstract fun toWireFormat(): UserStyleCategoryWireFormat.OptionWireFormat
    }

    /**
     * Translates an option name into an option. This will need to be overridden for userStyle
     * categories that can't sensibly be fully enumerated (e.g. a full 24-bit color picker).
     *
     * @param optionId The ID of the option
     * @return An [Option] corresponding to the name. This could either be one of the
     *     options from userStyleCategories or a newly constructed Option depending on the nature
     *     of the UserStyleCategory. If optionName is unrecognized then the default value for the
     *     category should be returned.
     */
    public open fun getOptionForId(optionId: String): Option =
        options.find { it.id == optionId } ?: options[defaultOptionIndex]
}
