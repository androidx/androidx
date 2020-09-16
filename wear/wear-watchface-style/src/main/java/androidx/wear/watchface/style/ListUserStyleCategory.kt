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
import android.os.Bundle

/** A ListStyleCategory represents a category with options selected from a List. */
open class ListUserStyleCategory :
    UserStyleCategory {

    internal companion object {
        internal const val CATEGORY_TYPE = "ListUserStyleCategory"
        internal const val OPTION_TYPE = "ListOption"
    }

    @JvmOverloads
    constructor (
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
         * Used by the style configuration UI. Describes which rendering layer this style affects.
         * Must be either 0 (for a style change with no visual effect, e.g. sound controls) or a
         * combination of {@link #LAYER_WATCH_FACE_BASE}, {@link #LAYER_COMPLICATONS}, {@link
         * #LAYER_UPPER}.
         */
        layerFlags: Int,

        /** The default option, used when data isn't persisted. */
        defaultOption: ListOption = options.first()
    ) : super(id, displayName, description, icon, options, defaultOption, layerFlags)

    internal constructor(bundle: Bundle) : super(bundle)

    /**
     * Represents choice within a {@link ListUserStyleCategory}, these must be enumerated up front.
     */
    open class ListOption : Option {
        /** Localized human readable name for the setting, used in the userStyle selection UI. */
        val displayName: String

        /**
         * Icon for use in the userStyle selection UI.
         */
        val icon: Icon?

        constructor(id: String, displayName: String, icon: Icon?) : super(id) {
            this.displayName = displayName
            this.icon = icon
        }

        internal companion object {
            internal const val KEY_DISPLAY_NAME = "KEY_DISPLAY_NAME"
            internal const val KEY_ICON = "KEY_ICON"
        }

        internal constructor(bundle: Bundle) : super(bundle) {
            displayName = bundle.getString(KEY_DISPLAY_NAME)!!
            icon = bundle.getParcelable(KEY_ICON)
        }

        final override fun writeToBundle(bundle: Bundle) {
            super.writeToBundle(bundle)
            bundle.putString(KEY_DISPLAY_NAME, displayName)
            bundle.putParcelable(KEY_ICON, icon)
        }

        override fun getOptionType() =
            OPTION_TYPE
    }

    override fun getCategoryType() =
        CATEGORY_TYPE
}
