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
 * [UserStyleSetting.Option]. This is intended for use by the WatchFace and the [selectedOptions]
 * map keys are the same objects as in the [UserStyleSchema]. This means you can't serialize a
 * UserStyle directly, instead you need to use a [UserStyleData] (see [toUserStyleData]).
 *
 * @param selectedOptions The [UserStyleSetting.Option] selected for each [UserStyleSetting]
 */
public class UserStyle(
    public val selectedOptions: Map<UserStyleSetting, UserStyleSetting.Option>
) {
    /**
     * Constructs a UserStyle with a deep copy of the [selectedOptions].
     *
     * @param userStyle The [UserStyle] to copy.
     */
    public constructor(userStyle: UserStyle) : this(HashMap(userStyle.selectedOptions))

    /**
     * Constructs a [UserStyle] from a [UserStyleData] and the [UserStyleSchema]. Unrecognized
     * style settings will be ignored. Unlisted style settings will be initialized with that
     * settings default option.
     *
     * @param userStyle The [UserStyle] represented as a [UserStyleData].
     * @param styleSchema The [UserStyleSchema] for this UserStyle, describes how we interpret
     * [userStyle].
     */
    public constructor(
        userStyle: UserStyleData,
        styleSchema: UserStyleSchema
    ) : this(
        HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
            for (styleSetting in styleSchema.userStyleSettings) {
                val option = userStyle.userStyleMap[styleSetting.id.value]
                if (option != null) {
                    this[styleSetting] = styleSetting.getSettingOptionForId(option)
                } else {
                    this[styleSetting] = styleSetting.defaultOption
                }
            }
        }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): UserStyleWireFormat = UserStyleWireFormat(toMap())

    /** Returns the style as a [UserStyleData]. */
    public fun toUserStyleData(): UserStyleData = UserStyleData(toMap())

    /** Returns the style as a [Map]<[String], [ByteArray]>. */
    private fun toMap(): Map<String, ByteArray> =
        selectedOptions.entries.associate { it.key.id.value to it.value.id.value }

    /** Returns the [UserStyleSetting.Option] for [setting] if there is one or `null` otherwise. */
    public operator fun get(setting: UserStyleSetting): UserStyleSetting.Option? =
        selectedOptions[setting]

    override fun toString(): String =
        "[" + selectedOptions.entries.joinToString(
            transform = { "${it.key.id} -> ${it.value}" }
        ) + "]"
}

/**
 * A form of [UserStyle] which is easy to serialize. This is intended for use by the watch face
 * clients and the editor where we can't practically use [UserStyle] due to it's limitations.
 */
public class UserStyleData(
    public val userStyleMap: Map<String, ByteArray>
) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(
        userStyle: UserStyleWireFormat
    ) : this(userStyle.mUserStyle)

    override fun toString(): String = "{" + userStyleMap.entries.joinToString(
        transform = {
            try {
                it.key + "=" + it.value.decodeToString()
            } catch (e: Exception) {
                it.key + "=" + it.value
            }
        }
    ) + "}"

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): UserStyleWireFormat = UserStyleWireFormat(userStyleMap)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserStyleData

        // Check if references are the same.
        if (userStyleMap == other.userStyleMap) return true

        // Check if contents are the same.
        if (userStyleMap.size != other.userStyleMap.size) return false

        for ((key, value) in userStyleMap) {
            val otherValue = other.userStyleMap[key] ?: return false
            if (!otherValue.contentEquals(value)) return false
        }

        return true
    }

    override fun hashCode(): Int {
        return userStyleMap.hashCode()
    }
}

/**
 * Describes the list of [UserStyleSetting]s the user can configure.
 *
 * @param userStyleSettings The user configurable style categories associated with this watch face.
 * Empty if the watch face doesn't support user styling. Note we allow at most one
 * [UserStyleSetting.ComplicationSlotsUserStyleSetting] and one
 * [UserStyleSetting.CustomValueUserStyleSetting]
 * in the list.
 */
public class UserStyleSchema(
    public val userStyleSettings: List<UserStyleSetting>
) {
    init {
        var complicationSlotsUserStyleSettingCount = 0
        var customValueUserStyleSettingCount = 0
        for (setting in userStyleSettings) {
            when (setting) {
                is UserStyleSetting.ComplicationSlotsUserStyleSetting ->
                    complicationSlotsUserStyleSettingCount++

                is UserStyleSetting.CustomValueUserStyleSetting ->
                    customValueUserStyleSettingCount++
            }
        }

        // This requirement makes it easier to implement companion editors.
        require(complicationSlotsUserStyleSettingCount <= 1) {
            "At most only one ComplicationSlotsUserStyleSetting is allowed"
        }

        // There's a hard limit to how big Schema + UserStyle can be and since this data is sent
        // over bluetooth to the companion there will be performance issues well before we hit
        // that the limit. As a result we want the total size of custom data to be kept small and
        // we are initially restricting there to be at most one CustomValueUserStyleSetting.
        require(customValueUserStyleSettingCount <= 1) {
            "At most only one CustomValueUserStyleSetting is allowed"
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(wireFormat: UserStyleSchemaWireFormat) : this(
        wireFormat.mSchema.map { UserStyleSetting.createFromWireFormat(it) }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): UserStyleSchemaWireFormat =
        UserStyleSchemaWireFormat(userStyleSettings.map { it.toWireFormat() })

    override fun toString(): String = "[" + userStyleSettings.joinToString() + "]"
}

/**
 * In memory storage for the current user style choices represented as [UserStyle], listeners can be
 * registered to observe style changes. The CurrentUserStyleRepository is initialized with a
 * [UserStyleSchema].
 *
 * @param schema The [UserStyleSchema] for this CurrentUserStyleRepository which describes the
 * available style categories.
 */
public class CurrentUserStyleRepository(
    public val schema: UserStyleSchema
) {
    /** A listener for observing [UserStyle] changes. */
    public interface UserStyleChangeListener {
        /** Called whenever the [UserStyle] changes. */
        @UiThread
        public fun onUserStyleChanged(userStyle: UserStyle)
    }

    private val styleListeners = HashSet<UserStyleChangeListener>()

    private val idToStyleSetting = schema.userStyleSettings.associateBy { it.id.value }

    /**
     * The current [UserStyle]. Assigning to this property triggers immediate
     * [UserStyleChangeListener] callbacks if if any options have changed.
     */
    public var userStyle: UserStyle = UserStyle(
        HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
            for (setting in schema.userStyleSettings) {
                this[setting] = setting.defaultOption
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
                val localSetting = idToStyleSetting[setting.id.value] ?: continue
                val styleSetting = field.selectedOptions[localSetting] ?: continue
                if (styleSetting.id.value != option.id.value) {
                    changed = true
                }
                hashmap[localSetting] = option
            }

            if (!changed) {
                return
            }

            for (styleListener in styleListeners) {
                styleListener.onUserStyleChanged(field)
            }
        }

    /**
     * Adds a [UserStyleChangeListener] which is called immediately and whenever the style changes.
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun addUserStyleChangeListener(userStyleChangeListener: UserStyleChangeListener) {
        styleListeners.add(userStyleChangeListener)
        userStyleChangeListener.onUserStyleChanged(userStyle)
    }

    /** Removes a [UserStyleChangeListener] previously added by [addUserStyleChangeListener]. */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun removeUserStyleChangeListener(userStyleChangeListener: UserStyleChangeListener) {
        styleListeners.remove(userStyleChangeListener)
    }
}
