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
import androidx.wear.watchface.style.data.BooleanUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.BooleanUserStyleSettingWireFormat.BooleanOptionWireFormat

/** A BooleanUserStyleSetting represents a setting with a true and a false setting. */
public class BooleanUserStyleSetting : UserStyleSetting {

    public constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the userStyle selection UI. */
        displayName: CharSequence,

        /** Localized description string displayed under the displayName. */
        description: CharSequence,

        /** Icon for use in the userStyle selection UI. */
        icon: Icon?,

        /** The default value for this BooleanUserStyleSetting. */
        defaultValue: Boolean,

        /**
         * Used by the style configuration UI. Describes which rendering layers this style affects.
         */
        affectsLayers: Collection<Layer>
    ) : super(
        id,
        displayName,
        description,
        icon,
        listOf(BooleanOption(true), BooleanOption(false)),
        when (defaultValue) {
            true -> 0
            false -> 1
        },
        affectsLayers
    )

    internal constructor(wireFormat: BooleanUserStyleSettingWireFormat) : super(wireFormat)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toWireFormat(): BooleanUserStyleSettingWireFormat =
        BooleanUserStyleSettingWireFormat(
            id,
            displayName,
            description,
            icon,
            getWireFormatOptionsList(),
            defaultOptionIndex,
            affectsLayers.map { it.ordinal }
        )

    /**
     * Returns the default value.
     */
    public fun getDefaultValue(): Boolean = (options[defaultOptionIndex] as BooleanOption).value

    /** Represents a true or false option in the [BooleanUserStyleSetting]. */
    public class BooleanOption : Option {
        public val value: Boolean

        public constructor(value: Boolean) : super(value.toString()) {
            this.value = value
        }

        internal constructor(wireFormat: BooleanOptionWireFormat) : super(wireFormat.mId) {
            value = wireFormat.mValue
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): BooleanOptionWireFormat = BooleanOptionWireFormat(id, value)
    }
}
