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

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.IllegalNodeException
import androidx.wear.watchface.complications.iterate
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.xmlpull.v1.XmlPullParserException

/**
 * An immutable representation of user style choices that maps each [UserStyleSetting] to
 * [UserStyleSetting.Option].
 *
 * This is intended for use by the WatchFace and entries are the same as the ones specified in the
 * [UserStyleSchema]. This means you can't serialize a UserStyle directly, instead you need to use a
 * [UserStyleData] (see [toUserStyleData]).
 *
 * To modify the user style, you should call [toMutableUserStyle] and construct a new [UserStyle]
 * instance with [MutableUserStyle.toUserStyle].
 *
 * @param selectedOptions The [UserStyleSetting.Option] selected for each [UserStyleSetting]
 * @param copySelectedOptions Whether to create a copy of the provided [selectedOptions]. If
 *   `false`, no mutable copy of the [selectedOptions] map should be retained outside this class.
 */
public class UserStyle
private constructor(
    selectedOptions: Map<UserStyleSetting, UserStyleSetting.Option>,
    copySelectedOptions: Boolean
) : Map<UserStyleSetting, UserStyleSetting.Option> {
    private val selectedOptions =
        if (copySelectedOptions) HashMap(selectedOptions) else selectedOptions

    /** Constructs a copy of the [UserStyle]. It is backed by the same map. */
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

    /** Constructs this UserStyle from data serialized to a [ByteArray] by [toByteArray]. */
    internal constructor(
        byteArray: ByteArray,
        styleSchema: UserStyleSchema
    ) : this(
        UserStyleData(
            HashMap<String, ByteArray>().apply {
                val bais = ByteArrayInputStream(byteArray)
                val reader = DataInputStream(bais)
                val numKeys = reader.readInt()
                for (i in 0 until numKeys) {
                    val key = reader.readUTF()
                    val numBytes = reader.readInt()
                    val value = ByteArray(numBytes)
                    reader.read(value, 0, numBytes)
                    put(key, value)
                }
                reader.close()
                bais.close()
            }
        ),
        styleSchema
    )

    /** The number of entries in the style. */
    override val size: Int by selectedOptions::size

    /**
     * Constructs a [UserStyle] from a [UserStyleData] and the [UserStyleSchema]. Unrecognized style
     * settings will be ignored. Unlisted style settings will be initialized with that setting's
     * default option.
     *
     * @param userStyle The [UserStyle] represented as a [UserStyleData].
     * @param styleSchema The [UserStyleSchema] for this UserStyle, describes how we interpret
     *   [userStyle].
     */
    @Suppress("Deprecation") // userStyleSettings
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toWireFormat(): UserStyleWireFormat = UserStyleWireFormat(toMap())

    /** Returns the style as a [UserStyleData]. */
    public fun toUserStyleData(): UserStyleData = UserStyleData(toMap())

    /** Returns a mutable instance initialized with the same mapping. */
    public fun toMutableUserStyle(): MutableUserStyle = MutableUserStyle(this)

    /** Returns the style as a [Map]<[String], [ByteArray]>. */
    private fun toMap(): Map<String, ByteArray> =
        selectedOptions.entries.associate { it.key.id.value to it.value.id.value }

    /** Returns the style encoded as a [ByteArray]. */
    internal fun toByteArray(): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = DataOutputStream(baos)
        writer.writeInt(selectedOptions.size)
        for ((key, value) in selectedOptions) {
            writer.writeUTF(key.id.value)
            writer.writeInt(value.id.value.size)
            writer.write(value.id.value, 0, value.id.value.size)
        }
        writer.close()
        baos.close()
        val ba = baos.toByteArray()
        return ba
    }

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
        "UserStyle[" +
            selectedOptions.entries.joinToString(transform = { "${it.key.id} -> ${it.value}" }) +
            "]"

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

/** A mutable [UserStyle]. This must be converted back to a [UserStyle] by calling [toUserStyle]. */
public class MutableUserStyle internal constructor(userStyle: UserStyle) :
    Iterable<Map.Entry<UserStyleSetting, UserStyleSetting.Option>> {
    /** The map from the available settings and the selected option. */
    private val selectedOptions =
        HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
            for ((setting, option) in userStyle) {
                this[setting] = option
            }
        }

    /** The number of entries in the style. */
    val size: Int
        get() = selectedOptions.size

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
     *   [setting].
     * @throws IllegalArgumentException if [setting] is not in the schema or if [option] is invalid
     *   for [setting].
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
     * @param settingId The [UserStyleSetting.Id] of the [UserStyleSetting] we're setting the
     *   [option] for, must be in the schema.
     * @param option the [UserStyleSetting.Option] we're setting. Must be a valid option for
     *   [settingId].
     * @throws IllegalArgumentException if [settingId] is not in the schema or if [option] is
     *   invalid for [settingId].
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
     *   schema.
     * @param optionId the [UserStyleSetting.Option.Id] for the [UserStyleSetting.Option] we're
     *   setting.
     * @throws IllegalArgumentException if [setting] is not in the schema or if [optionId] is
     *   unrecognized.
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
     *
     * @throws IllegalArgumentException if [settingId] is not in the schema or if [optionId] is
     *   unrecognized.
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
        "MutableUserStyle[" +
            selectedOptions.entries.joinToString(transform = { "${it.key.id} -> ${it.value}" }) +
            "]"
}

/**
 * A form of [UserStyle] which is easy to serialize. This is intended for use by the watch face
 * clients and the editor where we can't practically use [UserStyle] due to its limitations.
 */
public class UserStyleData(public val userStyleMap: Map<String, ByteArray>) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(userStyle: UserStyleWireFormat) : this(userStyle.mUserStyle)

    override fun toString(): String =
        "{" +
            userStyleMap.entries.joinToString(
                transform = {
                    try {
                        it.key + "=" + it.value.decodeToString()
                    } catch (e: Exception) {
                        it.key + "=" + it.value
                    }
                }
            ) +
            "}"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
 * Describes the list of [UserStyleSetting]s the user can configure. Note style schemas can be
 * hierarchical (see [UserStyleSetting.Option.childSettings]), editors should use
 * [rootUserStyleSettings] rather than [userStyleSettings] for populating the top level UI.
 *
 * @param userStyleSettings The user configurable style categories associated with this watch face.
 *   Empty if the watch face doesn't support user styling. Note we allow at most one
 *   [UserStyleSetting.CustomValueUserStyleSetting] in the list. Prior to android T ot most one
 *   [UserStyleSetting.ComplicationSlotsUserStyleSetting] is allowed, however from android T it's
 *   possible with hierarchical styles for there to be more than one, but at most one can be active
 *   at any given time.
 */
public class UserStyleSchema constructor(userStyleSettings: List<UserStyleSetting>) {
    public val userStyleSettings = userStyleSettings
        @Deprecated("use rootUserStyleSettings instead") get

    /** For use with hierarchical schemas, lists all the settings with no parent [Option]. */
    public val rootUserStyleSettings by lazy { userStyleSettings.filter { !it.hasParent } }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Throws(IOException::class, XmlPullParserException::class)
        fun inflate(
            resources: Resources,
            parser: XmlResourceParser,
            complicationScaleX: Float,
            complicationScaleY: Float
        ): UserStyleSchema {
            require(parser.name == "UserStyleSchema") { "Expected a UserStyleSchema node" }

            val idToSetting = HashMap<String, UserStyleSetting>()
            val userStyleSettings = ArrayList<UserStyleSetting>()

            // Parse the UserStyle declaration.
            parser.iterate {
                when (parser.name) {
                    "BooleanUserStyleSetting" ->
                        userStyleSettings.add(
                            UserStyleSetting.BooleanUserStyleSetting.inflate(resources, parser)
                        )
                    "ComplicationSlotsUserStyleSetting" ->
                        userStyleSettings.add(
                            UserStyleSetting.ComplicationSlotsUserStyleSetting.inflate(
                                resources,
                                parser,
                                complicationScaleX,
                                complicationScaleY
                            )
                        )
                    "DoubleRangeUserStyleSetting" ->
                        userStyleSettings.add(
                            UserStyleSetting.DoubleRangeUserStyleSetting.inflate(resources, parser)
                        )
                    "ListUserStyleSetting" ->
                        userStyleSettings.add(
                            UserStyleSetting.ListUserStyleSetting.inflate(
                                resources,
                                parser,
                                idToSetting
                            )
                        )
                    "LongRangeUserStyleSetting" ->
                        userStyleSettings.add(
                            UserStyleSetting.LongRangeUserStyleSetting.inflate(resources, parser)
                        )
                    else -> throw IllegalNodeException(parser)
                }
                idToSetting[userStyleSettings.last().id.value] = userStyleSettings.last()
            }

            return UserStyleSchema(userStyleSettings)
        }

        internal fun UserStyleSchemaWireFormat.toApiFormat(): List<UserStyleSetting> {
            val userStyleSettings = mSchema.map { UserStyleSetting.createFromWireFormat(it) }
            val wireUserStyleSettingsIterator = mSchema.iterator()
            for (setting in userStyleSettings) {
                val wireUserStyleSetting = wireUserStyleSettingsIterator.next()
                wireUserStyleSetting.mOptionChildIndices?.let {
                    // Unfortunately due to VersionedParcelable limitations, we can not extend the
                    // Options wire format (extending the contents of a list is not supported!!!).
                    // This means we need to encode/decode the childSettings in a round about way.
                    val optionsIterator = setting.options.iterator()
                    var option: Option? = null
                    for (childIndex in it) {
                        if (option == null) {
                            option = optionsIterator.next()
                        }
                        if (childIndex == -1) {
                            option = null
                        } else {
                            val childSettings = option.childSettings as ArrayList
                            val child = userStyleSettings[childIndex]
                            childSettings.add(child)
                            child.hasParent = true
                        }
                    }
                }
            }
            return userStyleSettings
        }
    }

    init {
        var complicationSlotsUserStyleSettingCount = 0
        var customValueUserStyleSettingCount = 0
        var displayNameIndex = 1
        for (setting in userStyleSettings) {
            // Provide the ordinal used by fallback descriptions for each setting.
            setting.setDisplayNameIndex(displayNameIndex++)

            when (setting) {
                is UserStyleSetting.ComplicationSlotsUserStyleSetting ->
                    complicationSlotsUserStyleSettingCount++
                is UserStyleSetting.CustomValueUserStyleSetting ->
                    customValueUserStyleSettingCount++
                is UserStyleSetting.LargeCustomValueUserStyleSetting ->
                    customValueUserStyleSettingCount++
                else -> {
                    // Nothing
                }
            }

            for (option in setting.options) {
                for (childSetting in option.childSettings) {
                    require(userStyleSettings.contains(childSetting)) {
                        "childSettings must be in the list of settings the UserStyleSchema is " +
                            "constructed with"
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            validateComplicationSettings(rootUserStyleSettings, null)
        } else {
            require(complicationSlotsUserStyleSettingCount <= 1) {
                "Prior to Android T, at most only one ComplicationSlotsUserStyleSetting is allowed"
            }
        }

        // There's a hard limit to how big Schema + UserStyle can be and since this data is sent
        // over bluetooth to the companion there will be performance issues well before we hit
        // that the limit. As a result we want the total size of custom data to be kept small and
        // we are initially restricting there to be at most one CustomValueUserStyleSetting.
        require(customValueUserStyleSettingCount <= 1) {
            "At most only one CustomValueUserStyleSetting is allowed"
        }
    }

    private fun validateComplicationSettings(
        settings: Collection<UserStyleSetting>,
        initialPrevSetting: UserStyleSetting.ComplicationSlotsUserStyleSetting?
    ) {
        var prevSetting = initialPrevSetting
        for (setting in settings) {
            if (setting is UserStyleSetting.ComplicationSlotsUserStyleSetting) {
                require(prevSetting == null) {
                    "From Android T multiple ComplicationSlotsUserStyleSettings are allowed, but" +
                        " at most one can be active for any permutation of UserStyle. Note: " +
                        "$setting and $prevSetting"
                }
                prevSetting = setting
            }
        }
        for (setting in settings) {
            for (option in setting.options) {
                if (option.childSettings.isNotEmpty()) {
                    validateComplicationSettings(option.childSettings, prevSetting)
                }
            }
        }
    }

    @Suppress("Deprecation") // userStyleSettings
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(wireFormat: UserStyleSchemaWireFormat) : this(wireFormat.toApiFormat())

    @Suppress("Deprecation") // userStyleSettings
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toWireFormat(): UserStyleSchemaWireFormat =
        UserStyleSchemaWireFormat(
            userStyleSettings.map { userStyleSetting ->
                val wireFormat = userStyleSetting.toWireFormat()
                // Unfortunately due to VersionedParcelable limitations, we can not extend the
                // Options wire format (extending the contents of a list is not supported!!!).
                // This means we need to encode/decode the childSettings in a round about way.
                val optionChildIndices = ArrayList<Int>()
                for (option in userStyleSetting.options) {
                    for (child in option.childSettings) {
                        optionChildIndices.add(userStyleSettings.indexOfFirst { it == child })
                    }
                    optionChildIndices.add(-1)
                }
                wireFormat.mOptionChildIndices = optionChildIndices
                wireFormat
            }
        )

    @Suppress("Deprecation") // userStyleSettings
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getDefaultUserStyle() =
        UserStyle(
            HashMap<UserStyleSetting, UserStyleSetting.Option>().apply {
                for (setting in userStyleSettings) {
                    this[setting] = setting.defaultOption
                }
            }
        )

    @Suppress("Deprecation") // userStyleSettings
    override fun toString(): String = "[" + userStyleSettings.joinToString() + "]"

    /**
     * Returns the [UserStyleSetting] whose [UserStyleSetting.Id] matches [settingId] or `null` if
     * none match.
     */
    @Suppress("Deprecation") // userStyleSettings
    operator fun get(settingId: UserStyleSetting.Id): UserStyleSetting? {
        // NB more than one match is not allowed, UserStyleSetting id's are required to be unique.
        return userStyleSettings.firstOrNull { it.id == settingId }
    }

    /**
     * Computes a SHA-1 [MessageDigest] hash of the [UserStyleSchema]. Note that for performance
     * reasons where possible the resource id or url for [Icon]s in the schema are used rather than
     * the image bytes. This means that this hash should be considered insensitive to changes to the
     * contents of icons between APK versions, which the developer should account for accordingly.
     */
    fun getDigestHash(): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        val digestOutputStream = DigestOutputStream(NullOutputStream(), md)

        @Suppress("Deprecation")
        for (setting in userStyleSettings) {
            setting.updateMessageDigest(digestOutputStream)
        }

        return md.digest()
    }

    private class NullOutputStream : OutputStream() {
        override fun write(value: Int) {}
    }

    private fun findActiveComplicationSetting(
        settings: Collection<UserStyleSetting>,
        userStyle: UserStyle
    ): UserStyleSetting.ComplicationSlotsUserStyleSetting? {
        for (setting in settings) {
            if (setting is UserStyleSetting.ComplicationSlotsUserStyleSetting) {
                return setting
            }
            findActiveComplicationSetting(userStyle[setting]!!.childSettings, userStyle)?.let {
                return it
            }
        }
        return null
    }

    /**
     * When a UserStyleSchema contains hierarchical styles, only part of it is deemed to be active
     * based on the user’s options in [userStyle]. Conversely if the UserStyleSchema doesn’t contain
     * any hierarchical styles then all of it is considered to be active all the time.
     *
     * From the active portion of the UserStyleSchema we only allow there to be at most one
     * [UserStyleSetting.ComplicationSlotsUserStyleSetting]. This function searches the active
     * portion of the UserStyleSchema for the [UserStyleSetting.ComplicationSlotsUserStyleSetting],
     * if one is found then it returns the selected [ComplicationSlotsOption] from that, based on
     * the [userStyle]. If a [UserStyleSetting.ComplicationSlotsUserStyleSetting] is not found in
     * the active portion of the UserStyleSchema it returns `null`.
     *
     * @param userStyle The [UserStyle] for which the function will search for the selected
     *   [ComplicationSlotsOption], if any.
     * @return The selected [ComplicationSlotsOption] based on the [userStyle] if any, or `null`
     *   otherwise.
     */
    public fun findComplicationSlotsOptionForUserStyle(
        userStyle: UserStyle
    ): ComplicationSlotsOption? =
        findActiveComplicationSetting(rootUserStyleSettings, userStyle)?.let {
            userStyle[it] as ComplicationSlotsOption
        }
}

/**
 * In memory storage for the current user style choices represented as a
 * [MutableStateFlow]<[UserStyle]>.
 *
 * @param schema The [UserStyleSchema] for this CurrentUserStyleRepository which describes the
 *   available style categories.
 */
public class CurrentUserStyleRepository(public val schema: UserStyleSchema) {
    // Mutable backing field for [userStyle].
    private val mutableUserStyle = MutableStateFlow(schema.getDefaultUserStyle())

    /**
     * The current [UserStyle]. If accessed from java, consider using
     * [androidx.lifecycle.FlowLiveDataConversions.asLiveData] to observe changes.
     */
    public val userStyle: StateFlow<UserStyle> by CurrentUserStyleRepository::mutableUserStyle

    /** The UserStyle options must be from the supplied [UserStyleSchema]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun updateUserStyle(newUserStyle: UserStyle) {
        validateUserStyle(newUserStyle)
        mutableUserStyle.value = newUserStyle
    }

    @Suppress("Deprecation") // userStyleSettings
    internal fun validateUserStyle(userStyle: UserStyle) {
        for ((key, value) in userStyle) {
            val setting = schema.userStyleSettings.firstOrNull { it == key }

            require(setting != null) {
                "UserStyleSetting $key is not a reference to a UserStyleSetting within " +
                    "the schema."
            }
            require(setting::class.java == value.getUserStyleSettingClass()) {
                "The option class (${value::class.java.canonicalName}) in $key must " +
                    "match the setting class " +
                    setting::class.java.canonicalName
            }
        }
    }
}
