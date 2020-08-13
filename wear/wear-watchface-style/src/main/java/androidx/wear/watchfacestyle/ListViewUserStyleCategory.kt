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

package androidx.wear.watchfacestyle

import android.graphics.drawable.Icon
import android.os.Bundle

/** A ListViewUserStyleCategory represents a category with options selected from a ListView. */
open class ListViewUserStyleCategory : UserStyleCategory {

    internal companion object {
        internal const val CATEGORY_TYPE = "ListViewUserStyleCategory"
        internal const val OPTION_TYPE = "ListViewOption"
    }

    constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the style selection UI. */
        displayName: String,

        /** Localized description string displayed under the displayName. */
        description: String,

        /** Icon for use in the style selection UI. */
        icon: Icon?,

        /** List of all options for this ListViewUserStyleCategory. */
        options: List<ListViewOption>
    ) : super(id, displayName, description, icon, options)

    internal constructor(bundle: Bundle) : super(bundle)

    /**
     * Represents a ListView choice within a style category, these must be enumerated up front.
     */
    open class ListViewOption : Option {
        /** Localized human readable name for the setting, used in the style selection UI. */
        val displayName: String

        /**
         * Icon for use in the style selection UI.
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

        override fun getOptionType() = OPTION_TYPE
    }

    override fun getCategoryType() = CATEGORY_TYPE
}
