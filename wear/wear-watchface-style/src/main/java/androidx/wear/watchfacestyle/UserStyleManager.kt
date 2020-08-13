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
    /** A listener for observing user style changes. */
    interface UserStyleListener {
        /** Called whenever the user style changes. */
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
        get() = _style
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
    fun addUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.add(userStyleListener)
        userStyleListener.onUserStyleChanged(_style)
    }

    /** Removes a {@link UserStyleListener} previously added by {@link #addUserStyleListener}. */
    fun removeUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.remove(userStyleListener)
    }
}