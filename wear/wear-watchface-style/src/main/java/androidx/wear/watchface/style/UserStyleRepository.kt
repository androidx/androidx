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
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat

/**
 * The users style choices represented as a map of [UserStyleCategory] to
 * [UserStyleCategory.Option].
 */
public class UserStyle(public val options: Map<UserStyleCategory, UserStyleCategory.Option>) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(
        userStyle: UserStyleWireFormat,
        userStyleCategories: List<UserStyleCategory>
    ) : this(
        HashMap<UserStyleCategory, UserStyleCategory.Option>().apply {
            for (styleCategory in userStyleCategories) {
                val option = userStyle.mUserStyle[styleCategory.id] ?: continue
                this[styleCategory] = styleCategory.getCategoryOptionForId(option)
            }
        }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): UserStyleWireFormat =
        UserStyleWireFormat(options.entries.associate { it.key.id to it.value.id })
}

/**
 * In memory storage for user style choices which allows listeners to be registered to observe
 * style changes.
 */
public class UserStyleRepository(
    /**
     * The style categories (i.e the style schema) associated with this watch face, that the user
     * can configure. May be empty. The first entry in each Option list is that category's default
     * value.
     */
    public val userStyleCategories: List<UserStyleCategory>
) {
    /** A listener for observing user style changes. */
    public interface UserStyleListener {
        /** Called whenever the user style changes. */
        @UiThread
        public fun onUserStyleChanged(userStyle: UserStyle)
    }

    private val styleListeners = HashSet<UserStyleListener>()

    // The current style state which is initialized from the userStyleCategories.
    @SuppressWarnings("SyntheticAccessor")
    private val _style = UserStyle(
        HashMap<UserStyleCategory, UserStyleCategory.Option>().apply {
            for (category in userStyleCategories) {
                this[category] = category.getDefaultOption()
            }
        }
    )

    /** The current user controlled style for rendering etc... */
    public var userStyle: UserStyle
        @UiThread
        get() = _style
        @UiThread
        set(style) {
            var changed = false
            val hashmap = _style.options as HashMap<UserStyleCategory, UserStyleCategory.Option>
            for ((category, option) in style.options) {
                // Ignore an unrecognized category.
                val styleCategory = _style.options[category] ?: continue
                if (styleCategory.id != option.id) {
                    changed = true
                }
                hashmap[category] = option
            }

            if (!changed) {
                return
            }

            for (styleListener in styleListeners) {
                styleListener.onUserStyleChanged(_style)
            }
        }

    /**
     * Adds a [UserStyleListener] which is called immediately and whenever the style changes.
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun addUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.add(userStyleListener)
        userStyleListener.onUserStyleChanged(_style)
    }

    /** Removes a [UserStyleListener] previously added by [addUserStyleListener]. */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun removeUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.remove(userStyleListener)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toSchemaWireFormat(): UserStyleSchemaWireFormat =
        UserStyleSchemaWireFormat(userStyleCategories.map { it.toWireFormat() })
}
