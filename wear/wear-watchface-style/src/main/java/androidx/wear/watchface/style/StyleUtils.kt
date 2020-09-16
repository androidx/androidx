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

import android.os.Bundle

/**
 * Collection of serialization helpers for {@link UserStyleCategory} and {@link
 * UserStyleCategory.Option}.
 */
class StyleUtils {
    companion object {
        /**
         * Serializes a List<{@link Option}> to the provided bundle.
         */
        @JvmStatic
        fun writeOptionListToBundle(options: List<UserStyleCategory.Option>, bundle: Bundle) {
            bundle.putParcelableArrayList(
                UserStyleCategory.KEY_OPTIONS,
                ArrayList(options.map { Bundle().apply { it.writeToBundle(this) } })
            )
        }

        /**
         * Deserializes a List<{@link Option}> from the provided bundle.
         */
        @JvmStatic
        fun readOptionsListFromBundle(bundle: Bundle) =
            (bundle.getParcelableArrayList<Bundle>(UserStyleCategory.KEY_OPTIONS))!!
                .map { UserStyleCategory.Option.createFromBundle(it) }

        /**
         * Serializes a Collection<{@link UserStyleCategory}> to a list of Bundles.
         */
        @JvmStatic
        fun userStyleCategoriesToBundles(categories: Collection<UserStyleCategory>) =
            categories.map { Bundle().apply { it.writeToBundle(this) } }

        /**
         * Deserializes a Collection<{@link UserStyleCategory}> from the provided bundle.
         */
        @JvmStatic
        fun bundlesToUserStyleCategoryList(categories: Collection<Bundle>) =
            categories.map { UserStyleCategory.createFromBundle(it) }

        /**
         * Serializes a Map<{@link UserStyleCategory}, {@link Option}> to the provided bundle.
         */
        @JvmStatic
        fun styleMapToBundle(userStyle: Map<UserStyleCategory, UserStyleCategory.Option>) =
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
        fun bundleToStyleMap(
            bundle: Bundle,
            schema: List<UserStyleCategory>
        ): MutableMap<UserStyleCategory, UserStyleCategory.Option> {
            return HashMap<UserStyleCategory, UserStyleCategory.Option>().apply {
                for (styleCategory in schema) {
                    this[styleCategory] =
                        styleCategory.getCategoryOptionForId(bundle.getString(styleCategory.id))
                }
            }
        }

        /**
         * Constructs a  Map<{@link UserStyleCategory}, {@link Option}> from a map of
         * UserStyleCategory id to Option id.
         */
        @JvmStatic
        fun idMapToStyleMap(
            idMap: Map<String, String>,
            schema: List<UserStyleCategory>
        ): MutableMap<UserStyleCategory, UserStyleCategory.Option> {
            return HashMap<UserStyleCategory, UserStyleCategory.Option>().apply {
                for (styleCategory in schema) {
                    this[styleCategory] =
                        styleCategory.getCategoryOptionForId(idMap[styleCategory.id])
                }
            }
        }
    }
}