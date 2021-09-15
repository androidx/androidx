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

import androidx.annotation.RestrictTo
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Proxy

/**
 * An immutable representation of user style choices that maps each [UserStyleSetting] to
 * [UserStyleSetting.Option].
 *
 * This is intended for use by the WatchFace and entries are the same as the ones specified in
 * the [UserStyleSchema]. This means you can't serialize a UserStyle directly, instead you need
 * to use a [UserStyleData] (see [toUserStyleData]).
 *
 * To modify the user style, you should call [toMutableUserStyle] and construct a new [UserStyle]
 * instance with [MutableUserStyle.toUserStyle].
 *
 * @param selectedOptions The [UserStyleSetting.Option] selected for each [UserStyleSetting]
 * @param copySelectedOptions Whether to create a copy of the provided [selectedOptions]. If
 * `false`, no mutable copy of the [selectedOptions] map should be retained outside this class.
 */
public class UserStyle private constructor(
    selectedOptions: Map<UserStyleSetting, UserStyleSetting.Option>,
    copySelectedOptions: Boolean
) : Map<UserStyleSetting, UserStyleSetting.Option> {
    private val selectedOptions =
        if (copySelectedOptions) HashMap(selectedOptions) else selectedOptions

    /**
     * Constructs a copy of the [UserStyle]. It is backed by the same map.
     */
    public constructor(userStyle: UserStyle) : this(userStyle.selectedOptions, false)

    /**
     * Constructs a [UserStyle] with the given selected options for each setting.
     *
     * A copy of the [selectedOptions] map will be created, so that changed to the map will not be
     * reflected by this object.
     */
    public constructor(
        selectedOptions: Map<UserStyleSetting, UserStyleSetting.Option>
    ) : this(selectedOptions, true)

    /** The number of entries in the style. */
    override val size: Int by selectedOptions::size

    /**
     * Constructs a [UserStyle] from a [UserStyleData] and the [UserStyleSchema]. Unrecognized
     * style settings will be ignored. Unlisted style settings will be initialized with that
     * setting's default option.
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

    /** Returns a mutable instance initialized with the same mapping. */
    public fun toMutableUserStyle(): MutableUserStyle = MutableUserStyle(this)

    /** Returns the style as a [Map]<[String], [ByteArray]>. */
    private fun toMap(): Map<String, ByteArray> =
        selectedOptions.entries.associate { it.key.id.value to it.value.id.value }

    /** Returns the [UserStyleSetting.Option] for [key] if there is one or `null` otherwise. */
    public override operator fun get(key: UserStyleSetting): UserStyleSetting.Option? =
        selectedOptions[key]

    /**
     * Returns the [UserStyleSetting.Option] for [settingId] if there is one or `null` otherwise.
     * Note this is an O(n) operation.
     */
    public operator fun get(settingId: UserStyleSetting.Id): UserStyleSetting.Option? =
        selectedOptions.firstNotNullOfOrNull { if (it.key.id == settingId) it.value else null }

    override fun toString(): String =
        "UserStyle[" + selectedOptions.entries.joinToString(
            transform = { "${it.key.id} -> ${it.value}" }
        ) + "]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserStyle

        if (selectedOptions != other.selectedOptions) return false

        return true
    }

    override fun hashCode(): Int {
        return selectedOptions.hashCode()
    }

    internal companion object {
        /**
         * Merges the content of [overrides] with [base].
         *
         * This function merges the content of [base] by overriding any setting that is in [base]
         * with the corresponding options from [overrides].
         *
         * Any setting in [overrides] that is not set in [base] will be ignored. Any setting that is
         * not present in [overrides] but it is in [base] will be kept unmodified.
         *
         * Returns the merged [UserStyle] or null if the merged [UserStyle] is not different from
         * [base], i.e., if applying the [overrides] does not change any of the [base] settings.
         */
        @JvmStatic
        internal fun merge(base: UserStyle, overrides: UserStyle): UserStyle? {
            // Created only if there are changes to apply.
            var merged: MutableUserStyle? = null
            for ((setting, option) in overrides.selectedOptions) {
                // Ignore an unrecognized setting.
                val currentOption = base[setting] ?: continue
                if (currentOption != option) {
                    merged = merged ?: base.toMutableUserStyle()
                    merged[setting] = option
                }
            }
            return merged?.toUserStyle()
        }
    }

    override val entries: Set<Map.Entry<UserStyleSetting, UserStyleSetting.Option>>
        get() = selectedOptions.entries

    override val keys: Set<UserStyleSetting>
        get() = selectedOptions.keys

    override val values: Collection<UserStyleSetting.Option>
        get() = selectedOptions.values

    override fun containsKey(key: UserStyleSetting): Boolean = selectedOptions.containsKey(key)

    override fun containsValue(value: UserStyleSetting.Option): Boolean =
        selectedOptions.containsValue(value)

    override fun isEmpty(): Boolean = selectedOptions.isEmpty()
}

/**
 * A mutable [UserStyle]. This must be converted back to a [UserStyle] by calling [toUserStyle].
 */
public class MutableUserStyle internal constructor(userStyle: UserStyle) :
    Iterable<Map.Entry<UserStyleSetting, UserStyleSetting.Option>> {
    /** The map from the available settings and the selected option. */
    private val selectedOptions = HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
        for ((setting, option) in userStyle) {
            this[setting] = option
        }
    }

    /** The number of entries in the style. */
    val size: Int get() = selectedOptions.size

    /** Iterator over the elements of the user style. */
    override fun iterator(): Iterator<Map.Entry<UserStyleSetting, UserStyleSetting.Option>> =
        selectedOptions.iterator()

    /** Returns the [UserStyleSetting.Option] for [setting] if there is one or `null` otherwise. */
    public operator fun get(setting: UserStyleSetting): UserStyleSetting.Option? =
        selectedOptions[setting]

    /**
     * Returns the [UserStyleSetting.Option] for [settingId] if there is one or `null` otherwise.
     * Note this is an O(n) operation.
     */
    public operator fun get(settingId: UserStyleSetting.Id): UserStyleSetting.Option? =
        selectedOptions.firstNotNullOfOrNull { if (it.key.id == settingId) it.value else null }

    /**
     * Sets the [UserStyleSetting.Option] for [setting] to the given [option].
     *
     * @param setting The [UserStyleSetting] we're setting the [option] for, must be in the schema.
     * @param option the [UserStyleSetting.Option] we're setting. Must be a valid option for
     * [setting].
     * @throws IllegalArgumentException if [setting] is not in the schema or if [option] is invalid
     * for [setting].
     */
    public operator fun set(setting: UserStyleSetting, option: UserStyleSetting.Option) {
        require(selectedOptions.containsKey(setting)) { "Unknown setting $setting" }
        require(option.getUserStyleSettingClass() == setting::class.java) {
            "The option class (${option::class.java.canonicalName}) must match the setting class " +
                setting::class.java.canonicalName
        }
        selectedOptions[setting] = option
    }

    /**
     * Sets the [UserStyleSetting.Option] for the setting with the given [settingId] to the given
     * [option].
     *
     * @param settingId The [UserStyleSetting.Id] of the  [UserStyleSetting] we're setting the
     * [option] for, must be in the schema.
     * @param option the [UserStyleSetting.Option] we're setting. Must be a valid option for
     * [settingId].
     * @throws IllegalArgumentException if [settingId] is not in the schema or if [option] is
     * invalid for [settingId].
     */
    public operator fun set(settingId: UserStyleSetting.Id, option: UserStyleSetting.Option) {
        val setting = getSettingForId(settingId)
        require(setting != null) { "Unknown setting $settingId" }
        require(option.getUserStyleSettingClass() == setting::class.java) {
            "The option must be a subclass of the setting"
        }
        selectedOptions[setting] = option
    }

    /**
     * Sets the [UserStyleSetting.Option] for [setting] to the option with the given [optionId].
     *
     * @param setting The [UserStyleSetting] we're setting the [optionId] for, must be in the
     * schema.
     * @param optionId the [UserStyleSetting.Option.Id] for the [UserStyleSetting.Option] we're
     * setting.
     * @throws IllegalArgumentException if [setting] is not in the schema or if [optionId] is
     * unrecognized.
     */
    public operator fun set(setting: UserStyleSetting, optionId: UserStyleSetting.Option.Id) {
        require(selectedOptions.containsKey(setting)) { "Unknown setting $setting" }
        val option = getOptionForId(setting, optionId)
        require(option != null) { "Unrecognized optionId $optionId" }
        selectedOptions[setting] = option
    }

    /**
     * Sets the [UserStyleSetting.Option] for the setting with the given [settingId] to the option
     * with the given [optionId].
     * @throws IllegalArgumentException if [settingId] is not in the schema or if [optionId] is
     * unrecognized.
     */
    public operator fun set(settingId: UserStyleSetting.Id, optionId: UserStyleSetting.Option.Id) {
        val setting = getSettingForId(settingId)
        require(setting != null) { "Unknown setting $settingId" }
        val option = getOptionForId(setting, optionId)
        require(option != null) { "Unrecognized optionId $optionId" }
        selectedOptions[setting] = option
    }

    /** Converts this instance to an immutable [UserStyle] with the same mapping. */
    public fun toUserStyle(): UserStyle = UserStyle(selectedOptions)

    private fun getSettingForId(settingId: UserStyleSetting.Id): UserStyleSetting? {
        for (setting in selectedOptions.keys) {
            if (setting.id == settingId) {
                return setting
            }
        }
        return null
    }

    private fun getOptionForId(
        setting: UserStyleSetting,
        optionId: UserStyleSetting.Option.Id
    ): UserStyleSetting.Option? {
        for (option in setting.options) {
            if (option.id == optionId) {
                return option
            }
        }
        return null
    }

    override fun toString(): String =
        "MutableUserStyle[" + selectedOptions.entries.joinToString(
            transform = { "${it.key.id} -> ${it.value}" }
        ) + "]"
}

/**
 * A form of [UserStyle] which is easy to serialize. This is intended for use by the watch face
 * clients and the editor where we can't practically use [UserStyle] due to its limitations.
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
 * [UserStyleSetting.CustomValueUserStyleSetting] in the list.
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
 * In memory storage for the current user style choices represented as a
 * [MutableStateFlow]<[UserStyle]>. The UserStyle options must be from the supplied
 * [UserStyleSchema].
 *
 * @param schema The [UserStyleSchema] for this CurrentUserStyleRepository which describes the
 * available style categories.
 */
public class CurrentUserStyleRepository(public val schema: UserStyleSchema) {
    private var wrappedUserStyle = MutableStateFlow(
        UserStyle(
            HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
                for (setting in schema.userStyleSettings) {
                    this[setting] = setting.defaultOption
                }
            }
        )
    )

    /**
     * The current [UserStyle]. If accessed from java, consider using
     * [androidx.wear.watchface.StateFlowCompatHelper] to observe callbacks.
     */
    // Unfortunately a dynamic proxy is the only way we can reasonably validate the UserStyle,
    // exceptions thrown within a coroutine are lost and the MutableStateFlow interface includes
    // internal unstable methods so we can't use a static proxy...
    @Suppress("BanUncheckedReflection", "UNCHECKED_CAST")
    public var userStyle: MutableStateFlow<UserStyle> = Proxy.newProxyInstance(
        MutableStateFlow::class.java.classLoader,
        arrayOf<Class<*>>(MutableStateFlow::class.java)
    ) { _, method, args ->
        if (args == null) {
            method?.invoke(wrappedUserStyle)
        } else {
            if (method?.name == "setValue") {
                validateUserStyle(args[0] as UserStyle)
            }
            method?.invoke(wrappedUserStyle, *args)
        }
    } as MutableStateFlow<UserStyle>

    internal fun validateUserStyle(userStyle: UserStyle) {
        for ((key, value) in userStyle) {
            val setting = schema.userStyleSettings.firstOrNull { it == key }

            require(setting != null) {
                "UserStyleSetting $key is not a reference to a UserStyleSetting within " +
                    "the schema."
            }
            require(setting::class.java == value.getUserStyleSettingClass()) {
                "The option class (${value::class.java.canonicalName}) in $key must " +
                    "match the setting class " + setting::class.java.canonicalName
            }
        }
    }
}
