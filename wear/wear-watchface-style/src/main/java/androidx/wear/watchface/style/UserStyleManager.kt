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

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.UiThread

/**
 * In memory storage for user style choices which allows listeners to be registered to observe
 * style changes.
 */
class UserStyleManager(
    /**
     * The style categories (i.e the style schema) associated with this watch face, that the user
     * can configure. May be empty. The first entry in each Option list is that category's default
     * value.
     */
    val userStyleCategories: List<UserStyleCategory>
) {
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

    /** A listener for observing user style changes. */
    interface UserStyleListener {
        /** Called whenever the user style changes. */
        @UiThread
        fun onUserStyleChanged(userStyle: Map<UserStyleCategory, UserStyleCategory.Option>)
    }

    private val styleListeners = HashSet<UserStyleListener>()

    // The current style state which is initialized from the userStyleCategories.
    @SuppressWarnings("SyntheticAccessor")
    private val _style = HashMap<UserStyleCategory, UserStyleCategory.Option>().apply {
        for (category in userStyleCategories) {
            this[category] = category.options.first()
        }
    }

    /** The current user controlled style for rendering etc... */
    var userStyle: Map<UserStyleCategory, UserStyleCategory.Option>
        @UiThread
        get() = _style

        @UiThread
        set(style) {
            val serialized = HashMap<String, String>()
            var changed = false
            for ((category, option) in style) {
                // Ignore an unrecognized category.
                val styleCategory = _style[category] ?: continue
                if (styleCategory.id != option.id) {
                    changed = true
                }
                _style[category] = option
                serialized[category.id] = option.id
            }

            if (!changed) {
                return
            }

            for (styleListener in styleListeners) {
                styleListener.onUserStyleChanged(_style)
            }
        }

    /**
     * Adds a {@link UserStyleListener} which is called immediately and whenever the style changes.
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    fun addUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.add(userStyleListener)
        userStyleListener.onUserStyleChanged(_style)
    }

    /** Removes a {@link UserStyleListener} previously added by {@link #addUserStyleListener}. */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    fun removeUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.remove(userStyleListener)
    }
}
