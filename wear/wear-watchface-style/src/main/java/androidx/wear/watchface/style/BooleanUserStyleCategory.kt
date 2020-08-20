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

/** A BooleanUserStyleCategory represents a category with a true and a false setting. */
class BooleanUserStyleCategory :
    UserStyleCategory {

    internal companion object {
        internal const val CATEGORY_TYPE = "BooleanUserStyleCategory"
        internal const val OPTION_TYPE = "BooleanOption"
    }

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
        defaultValue: Boolean
    ) : super(
        id,
        displayName,
        description,
        icon,
        listOf(BooleanOption(true), BooleanOption(false)),
        BooleanOption(defaultValue)
    )

    internal constructor(bundle: Bundle) : super(bundle)

    /** Represents a true or false option in the {@link BooleanUserStyleCategory}. */
    open class BooleanOption :
        Option {
        val value: Boolean

        constructor(value: Boolean) : super(value.toString()) {
            this.value = value
        }

        internal constructor(bundle: Bundle) : super(bundle) {
            value = id.toBoolean()
        }

        final override fun writeToBundle(bundle: Bundle) {
            super.writeToBundle(bundle)
        }

        override fun getOptionType() = OPTION_TYPE
    }

    override fun getCategoryType() = CATEGORY_TYPE
}
