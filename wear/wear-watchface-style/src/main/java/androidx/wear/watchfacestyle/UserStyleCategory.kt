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
import androidx.annotation.CallSuper

/**
 * Watch faces often have user configurable styles. The definition of what is a style is left up
 * to the watch face but it typically incorporates a variety of categories such as: color,
 * visual theme for watch hands, font, tick shape, complications, audio elements, etc...
 */
abstract class UserStyleCategory(
    /** Identifier for the element, must be unique. */
    val id: String,

    /** Localized human readable name for the element, used in the userStyle selection UI. */
    val displayName: String,

    /** Localized description string displayed under the displayName. */
    val description: String,

    /** Icon for use in the userStyle selection UI. */
    val icon: Icon?,

    /**
     * List of options for this UserStyleCategory. Depending on the type of UserStyleCategory this
     * may be an exhaustive list, or just examples to populate a ListView in case the
     * UserStyleCategory isn't supported by the UI (e.g. a new WatchFace with an old Companion).
     */
    val options: List<Option>
) {
    companion object {
        private const val KEY_CATEGORY_TYPE = "KEY_CATEGORY_TYPE"
        private const val KEY_STYLE_CATEGORY_ID = "KEY_STYLE_CATEGORY_ID"
        private const val KEY_DISPLAY_NAME = "KEY_DISPLAY_NAME"
        private const val KEY_DESCRIPTION = "KEY_DESCRIPTION"
        private const val KEY_ICON = "KEY_ICON"
        private const val KEY_OPTIONS = "KEY_OPTIONS"

        /**
         * Constructs an {@link UserStyleCategory} serialized in a {@link Bundle}.
         *
         * @param bundle The {@link Bundle} containing a serialized {@link UserStyleCategory}
         * @return The deserialized {@link UserStyleCategory}
         * @throws IllegalArgumentException if the Bundle contains unrecognized data.
         */
        @JvmStatic
        fun createFromBundle(bundle: Bundle): UserStyleCategory {
            return when (val styleCategoryClass = bundle.getString(KEY_CATEGORY_TYPE)!!) {
                ListViewUserStyleCategory.CATEGORY_TYPE -> ListViewUserStyleCategory(bundle)
                else -> throw IllegalArgumentException(
                    "Unknown UserStyleCategory class " + styleCategoryClass
                )
            }
        }

        /**
         * Serializes a List<{@link Option}> to the provided bundle.
         */
        @JvmStatic
        fun writeOptionListToBundle(options: List<Option>, bundle: Bundle) {
            bundle.putParcelableArrayList(
                KEY_OPTIONS,
                ArrayList(options.map { Bundle().apply { it.writeToBundle(this) } })
            )
        }

        /**
         * Deserializes a List<{@link Option}> from the provided bundle.
         */
        @JvmStatic
        fun readOptionsListFromBundle(bundle: Bundle) =
            (bundle.getParcelableArrayList<Bundle>(KEY_OPTIONS))!!
                .map { Option.createFromBundle(it) }

        /**
         * Serializes a List<{@link UserStyleCategory}> to the provided bundle.
         */
        @JvmStatic
        fun userStyleCategoryListToBundles(categories: List<UserStyleCategory>) =
            categories.map { Bundle().apply { it.writeToBundle(this) } }

        /**
         * Deserializes a List<{@link UserStyleCategory}> from the provided bundle.
         */
        @JvmStatic
        fun bundlesToUserStyleCategoryLists(categories: List<Bundle>) =
            categories.map { createFromBundle(it) }

        /**
         * Serializes a Map<{@link UserStyleCategory}, {@link Option}> to the provided bundle.
         */
        @JvmStatic
        fun styleMapToBundle(userStyle: Map<UserStyleCategory, Option>) =
            Bundle().apply {
                for ((styleCategory, categoryOption) in userStyle) {
                    putString(styleCategory.id, categoryOption.id)
                }
            }

        /**
         * Deserializes a Map<{@link UserStyleCategory}, {@link Option}> from the provided bundle.
         * Only categories from the schema are deserialized.
         */
        @JvmStatic
        fun bundleToStyleMap(bundle: Bundle, schema: List<UserStyleCategory>) =
            HashMap<UserStyleCategory, Option>().apply {
                for (styleCategory in schema) {
                    val optionId =
                        bundle.getString(styleCategory.id) ?: styleCategory.options.first().id
                    val option = styleCategory.getOptionForId(optionId)
                    this[styleCategory] = option
                }
            }

        /**
         * Constructs a  Map<{@link UserStyleCategory}, {@link Option}> from a map of
         * UserStyleCategory id to Option id.
         */
        fun idMapToStyleMap(idMap: Map<String, String>, schema: List<UserStyleCategory>) =
            HashMap<UserStyleCategory, Option>().apply {
                for (styleCategory in schema) {
                    val optionId = idMap[styleCategory.id] ?: styleCategory.options.first().id
                    val option = styleCategory.getOptionForId(optionId)
                    this[styleCategory] = option
                }
            }
    }

    internal constructor(bundle: Bundle) : this(
        bundle.getString(KEY_STYLE_CATEGORY_ID)!!,
        bundle.getString(KEY_DISPLAY_NAME)!!,
        bundle.getString(KEY_DESCRIPTION)!!,
        bundle.getParcelable(KEY_ICON),
        readOptionsListFromBundle(bundle)
    )

    @CallSuper
    open fun writeToBundle(bundle: Bundle) {
        bundle.putString(KEY_CATEGORY_TYPE, getCategoryType())
        bundle.putString(KEY_STYLE_CATEGORY_ID, id)
        bundle.putString(KEY_DISPLAY_NAME, displayName)
        bundle.putString(KEY_DESCRIPTION, description)
        bundle.putParcelable(KEY_ICON, icon)
        writeOptionListToBundle(options, bundle)
    }

    /**
     * Represents a choice within a style category.
     *
     * @property id Machine readable identifier for the style setting.
     */
    abstract class Option(
        /** Identifier for the option, must be unique within the UserStyleCategory. */
        val id: String
    ) {
        companion object {
            private const val KEY_OPTION_TYPE = "KEY_OPTION_TYPE"
            private const val KEY_OPTION_ID = "KEY_OPTION_ID"

            /**
             * Constructs an {@link Option} serialized in a {@link Bundle}.
             *
             * @param bundle The {@link Bundle} containing a serialized {@link Option}
             * @return The deserialized {@link Option}
             * @throws IllegalArgumentException if the Bundle contains unrecognized data.
             */
            @JvmStatic
            fun createFromBundle(bundle: Bundle): Option {
                return when (val optionClass = bundle.getString(KEY_OPTION_TYPE)!!) {
                    ListViewUserStyleCategory.OPTION_TYPE ->
                        ListViewUserStyleCategory.ListViewOption(bundle)
                    else -> throw IllegalArgumentException(
                        "Unknown UserStyleCategory.Option class " + optionClass
                    )
                }
            }
        }

        internal constructor(bundle: Bundle) : this(bundle.getString(KEY_OPTION_ID)!!)

        @CallSuper
        open fun writeToBundle(bundle: Bundle) {
            bundle.putString(KEY_OPTION_TYPE, getOptionType())
            bundle.putString(KEY_OPTION_ID, id)
        }

        /** @return The type name which is used when unbundeling. */
        abstract fun getOptionType(): String
    }

    /**
     * Translates an option name into an option. This will need to be overridden for userStyle
     * categories that can't sensibly be fully enumerated (e.g. a full 24-bit color picker).
     *
     * @param optionId The ID of the option
     * @return An {@link Option} corresponding to the name. This could either be one of the
     *     options from userStyleCategories or a newly constructed Option depending on the nature
     *     of the UserStyleCategory. If optionName is unrecognized then the default value for the
     *     category should be returned.
     */
    open fun getOptionForId(optionId: String) =
        options.find { it.id == optionId } ?: options.first()

    /** @return The type name which is used by the UI to work out which widget to use. */
    abstract fun getCategoryType(): String
}
