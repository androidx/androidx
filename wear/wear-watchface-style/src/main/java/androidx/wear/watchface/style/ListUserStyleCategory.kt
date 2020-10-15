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
import androidx.wear.watchface.style.data.ListUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.ListUserStyleCategoryWireFormat.ListOptionWireFormat

/** A ListStyleCategory represents a category with options selected from a List. */
public open class ListUserStyleCategory : UserStyleCategory {

    @JvmOverloads
    public constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the userStyle selection UI. */
        displayName: String,

        /** Localized description string displayed under the displayName. */
        description: String,

        /** Icon for use in the userStyle selection UI. */
        icon: Icon?,

        /** List of all options for this ListUserStyleCategory. */
        options: List<ListOption>,

        /**
         * Used by the style configuration UI. Describes which rendering layers this style affects.
         */
        affectsLayers: Collection<Layer>,

        /** The default option, used when data isn't persisted. */
        defaultOption: ListOption = options.first()
    ) : super(
        id,
        displayName,
        description,
        icon,
        options,
        options.indexOf(defaultOption),
        affectsLayers
    )

    internal constructor(wireFormat: ListUserStyleCategoryWireFormat) : super(wireFormat)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toWireFormat(): ListUserStyleCategoryWireFormat =
        ListUserStyleCategoryWireFormat(
            id,
            displayName,
            description,
            icon,
            getWireFormatOptionsList(),
            defaultOptionIndex,
            affectsLayers.map { it.ordinal }
        )

    /**
     * Represents choice within a [ListUserStyleCategory], these must be enumerated up front.
     */
    public open class ListOption : Option {
        /** Localized human readable name for the setting, used in the style selection UI. */
        public val displayName: String

        /** Icon for use in the style selection UI. */
        public val icon: Icon?

        public constructor(id: String, displayName: String, icon: Icon?) : super(id) {
            this.displayName = displayName
            this.icon = icon
        }

        internal constructor(wireFormat: ListOptionWireFormat) : super(wireFormat.mId) {
            displayName = wireFormat.mDisplayName
            icon = wireFormat.mIcon
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): ListOptionWireFormat =
            ListOptionWireFormat(id, displayName, icon)
    }
}
