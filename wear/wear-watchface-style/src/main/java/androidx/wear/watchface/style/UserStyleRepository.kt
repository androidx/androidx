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
 * The users style choices represented as a map of [UserStyleSetting] to
 * [UserStyleSetting.Option].
 */
public class UserStyle(
    public val selectedOptions: Map<UserStyleSetting, UserStyleSetting.Option>
) {
    /**
     * Constructs a [UserStyle] from a Map<String, String> and the [UserStyleSchema]. Unrecognized
     * style settings will be ignored. Unlisted style settings will be initialized with that
     * settings default option.
     */
    public constructor(
        userStyle: Map<String, String>,
        styleSchema: UserStyleSchema
    ) : this(
        HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
            for (styleSetting in styleSchema.userStyleSettings) {
                val option = userStyle[styleSetting.id]
                if (option != null) {
                    this[styleSetting] = styleSetting.getSettingOptionForId(option)
                } else {
                    this[styleSetting] = styleSetting.getDefaultOption()
                }
            }
        }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(
        userStyle: UserStyleWireFormat,
        styleSchema: UserStyleSchema
    ) : this(userStyle.mUserStyle, styleSchema)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): UserStyleWireFormat =
        UserStyleWireFormat(toMap())

    /** Returns the style as a Map<String, String>. */
    public fun toMap(): Map<String, String> =
        selectedOptions.entries.associate { it.key.id to it.value.id }
}

/** Describes the list of [UserStyleSetting]s the user can configure. */
public class UserStyleSchema(
    /**
     * The user configurable style categories associated with this watch face. Empty if the watch
     * face doesn't support user styling.
     */
    public val userStyleSettings: List<UserStyleSetting>
) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(wireFormat: UserStyleSchemaWireFormat) : this(
        wireFormat.mSchema.map { UserStyleSetting.createFromWireFormat(it) }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): UserStyleSchemaWireFormat =
        UserStyleSchemaWireFormat(userStyleSettings.map { it.toWireFormat() })
}

/**
 * An in memory storage for user style choices represented as [UserStyle], listeners can be
 * registered to observe style changes. The UserStyleRepository is initialized with a
 * [UserStyleSchema].
 */
public class UserStyleRepository(
    /**
     * The [UserStyleSchema] for this UserStyleRepository which describes the available style
     * categories.
     */
    public val schema: UserStyleSchema
) {
    /** A listener for observing [UserStyle] changes. */
    public interface UserStyleListener {
        /** Called whenever the [UserStyle] changes. */
        @UiThread
        public fun onUserStyleChanged(userStyle: UserStyle)
    }

    private val styleListeners = HashSet<UserStyleListener>()

    /**
     * The current [UserStyle]. Assigning to this property triggers immediate [UserStyleListener]
     * callbacks if if any options have changed.
     */
    public var userStyle: UserStyle = UserStyle(
        HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
            for (setting in schema.userStyleSettings) {
                this[setting] = setting.getDefaultOption()
            }
        }
    )
        @UiThread
        get
        @UiThread
        set(style) {
            var changed = false
            val hashmap =
                field.selectedOptions as HashMap<UserStyleSetting, UserStyleSetting.Option>
            for ((setting, option) in style.selectedOptions) {
                // Ignore an unrecognized setting.
                val styleSetting = field.selectedOptions[setting] ?: continue
                if (styleSetting.id != option.id) {
                    changed = true
                }
                hashmap[setting] = option
            }

            if (!changed) {
                return
            }

            for (styleListener in styleListeners) {
                styleListener.onUserStyleChanged(field)
            }
        }

    /**
     * Adds a [UserStyleListener] which is called immediately and whenever the style changes.
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun addUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.add(userStyleListener)
        userStyleListener.onUserStyleChanged(userStyle)
    }

    /** Removes a [UserStyleListener] previously added by [addUserStyleListener]. */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun removeUserStyleListener(userStyleListener: UserStyleListener) {
        styleListeners.remove(userStyleListener)
    }
}
