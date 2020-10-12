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

/** A BooleanUserStyleCategory represents a category with a true and a false setting. */
class BooleanUserStyleCategory : UserStyleCategory {

    constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the userStyle selection UI. */
        displayName: String,

        /** Localized description string displayed under the displayName. */
        description: String,

        /** Icon for use in the userStyle selection UI. */
        icon: Icon?,

        /** The default value for this BooleanUserStyleCategory. */
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

    internal constructor(wireFormat: BooleanUserStyleCategoryWireFormat) : super(wireFormat)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toWireFormat() =
        BooleanUserStyleCategoryWireFormat(
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
    fun getDefaultValue() = (options[defaultOptionIndex] as BooleanOption).value

    /** Represents a true or false option in the [BooleanUserStyleCategory]. */
    open class BooleanOption : Option {
        val value: Boolean

        constructor(value: Boolean) : super(value.toString()) {
            this.value = value
        }

        internal constructor(
            wireFormat: BooleanUserStyleCategoryWireFormat.BooleanOptionWireFormat
        ) : super(wireFormat.mId) {
            value = wireFormat.mValue
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat() =
            BooleanUserStyleCategoryWireFormat.BooleanOptionWireFormat(id, value)
    }
}
