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
import androidx.annotation.RestrictTo
import androidx.wear.complications.ComplicationSlotBounds
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.Id.Companion.MAX_LENGTH
import androidx.wear.watchface.style.data.BooleanOptionWireFormat
import androidx.wear.watchface.style.data.BooleanUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.ComplicationOverlayWireFormat
import androidx.wear.watchface.style.data.ComplicationsOptionWireFormat
import androidx.wear.watchface.style.data.ComplicationsUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.CustomValueOptionWireFormat
import androidx.wear.watchface.style.data.CustomValueUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.DoubleRangeOptionWireFormat
import androidx.wear.watchface.style.data.DoubleRangeUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.ListOptionWireFormat
import androidx.wear.watchface.style.data.ListUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.LongRangeOptionWireFormat
import androidx.wear.watchface.style.data.LongRangeUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.OptionWireFormat
import androidx.wear.watchface.style.data.UserStyleSettingWireFormat
import java.nio.ByteBuffer
import java.security.InvalidParameterException

/**
 * Watch faces often have user configurable styles, the definition of what is a style is left up to
 * the watch face but it typically incorporates a variety of settings such as: color, visual theme
 * for watch hands, font, tick shape, complication slots, audio elements, etc...
 *
 * A UserStyleSetting represents one of these dimensions. See also [UserStyleSchema] which defines
 * the list of UserStyleSettings provided by the watch face.
 *
 * Styling data gets shared with the companion phone to support editors (typically over bluetooth),
 * as a result the size of serialized UserStyleSettings could become an issue if large.
 *
 * @param id Identifier for the element, must be unique. Styling data gets shared with the companion
 * (typically via bluetooth) so size is a consideration and short ids are encouraged. There is a
 * maximum length see [MAX_LENGTH].
 * @param displayName Localized human readable name for the element, used in the userStyle selection
 * UI.
 * @param description Localized description string displayed under the displayName.
 * @param icon Icon for use in the style selection UI.
 * @param options List of options for this UserStyleSetting. Depending on the type of
 * UserStyleSetting this may be an exhaustive list, or just examples to populate a ListView in case
 * the UserStyleSetting isn't supported by the UI (e.g. a new WatchFace with an old Companion).
 * @param defaultOptionIndex The default option index, used if nothing has been selected within the
 * [options] list.
 * @param affectedWatchFaceLayers Used by the style configuration UI. Describes which rendering
 * layers this style affects.
 */
public sealed class UserStyleSetting(
    public val id: Id,
    public val displayName: CharSequence,
    public val description: CharSequence,
    public val icon: Icon?,
    public val options: List<Option>,
    public val defaultOptionIndex: Int,
    public val affectedWatchFaceLayers: Collection<WatchFaceLayer>
) {
    /**
     * Machine readable identifier for [UserStyleSetting]s. The length of this identifier may not
     * exceed [MAX_LENGTH].
     */
    public class Id(public val value: String) {
        public companion object {
            /** Maximum length of the [value] field. */
            public const val MAX_LENGTH: Int = 40
        }

        init {
            require(value.length <= MAX_LENGTH) {
                "UserStyleSetting.value.length (${value.length}) must be less than MAX_LENGTH " +
                    "($MAX_LENGTH)"
            }
        }

        override fun toString(): String = value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Id

            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }
    }

    public companion object {
        internal fun createFromWireFormat(
            wireFormat: UserStyleSettingWireFormat
        ): UserStyleSetting = when (wireFormat) {
            is BooleanUserStyleSettingWireFormat -> BooleanUserStyleSetting(wireFormat)

            is ComplicationsUserStyleSettingWireFormat ->
                ComplicationSlotsUserStyleSetting(wireFormat)

            is CustomValueUserStyleSettingWireFormat -> CustomValueUserStyleSetting(wireFormat)

            is DoubleRangeUserStyleSettingWireFormat -> DoubleRangeUserStyleSetting(wireFormat)

            is ListUserStyleSettingWireFormat -> ListUserStyleSetting(wireFormat)

            is LongRangeUserStyleSettingWireFormat -> LongRangeUserStyleSetting(wireFormat)

            else -> throw IllegalArgumentException(
                "Unknown StyleCategoryWireFormat " + wireFormat::javaClass.name
            )
        }
    }

    init {
        require(defaultOptionIndex >= 0 && defaultOptionIndex < options.size) {
            "defaultOptionIndex must be in the range [0 .. options.size)"
        }
    }

    internal fun getSettingOptionForId(id: ByteArray?) =
        if (id == null) {
            options[defaultOptionIndex]
        } else {
            getOptionForId(id)
        }

    private constructor(wireFormat: UserStyleSettingWireFormat) : this(
        Id(wireFormat.mId),
        wireFormat.mDisplayName,
        wireFormat.mDescription,
        wireFormat.mIcon,
        wireFormat.mOptions.map { Option.createFromWireFormat(it) },
        wireFormat.mDefaultOptionIndex,
        wireFormat.mAffectsLayers.map { WatchFaceLayer.values()[it] }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public abstract fun toWireFormat(): UserStyleSettingWireFormat

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun getWireFormatOptionsList(): List<OptionWireFormat> =
        options.map { it.toWireFormat() }

    /** Returns the default for when the user hasn't selected an option. */
    public val defaultOption: Option
        get() = options[defaultOptionIndex]

    override fun toString(): String = "{${id.value} : " +
        options.joinToString(transform = { it.toString() }) + "}"

    /**
     * Represents a choice within a style setting which can either be an option from the list or a
     * an arbitrary value depending on the nature of the style setting.
     *
     * @property id Machine readable [Id] for the style setting. Identifier for the option (or the
     *     option itself for [CustomValueUserStyleSetting.CustomValueOption]), must be unique
     *     within the UserStyleSetting. Short ids are encouraged.
     */
    public abstract class Option(public val id: Id) {
        /**
         * Machine readable identifier for [Option]s. The length of this identifier may not exceed
         * [MAX_LENGTH].
         */
        public class Id(public val value: ByteArray) {
            /**
             * Constructs an [Id] with a [String] encoded to a [ByteArray] by
             * [String.encodeToByteArray].
             */
            public constructor(value: String) : this(value.encodeToByteArray())

            public companion object {
                /** Maximum length of the [value] field. */
                public const val MAX_LENGTH: Int = 1024
            }

            init {
                require(value.size <= MAX_LENGTH) {
                    "Option.Id.value.size (${value.size}) must be less than MAX_LENGTH " +
                        "($MAX_LENGTH)"
                }
            }

            override fun toString(): String =
                try {
                    value.decodeToString()
                } catch (e: Exception) {
                    value.toString()
                }
        }

        public companion object {
            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            public fun createFromWireFormat(
                wireFormat: OptionWireFormat
            ): Option =
                when (wireFormat) {
                    is BooleanOptionWireFormat ->
                        BooleanUserStyleSetting.BooleanOption(wireFormat)

                    is ComplicationsOptionWireFormat ->
                        ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(wireFormat)

                    is CustomValueOptionWireFormat ->
                        CustomValueUserStyleSetting.CustomValueOption(wireFormat)

                    is DoubleRangeOptionWireFormat ->
                        DoubleRangeUserStyleSetting.DoubleRangeOption(wireFormat)

                    is ListOptionWireFormat ->
                        ListUserStyleSetting.ListOption(wireFormat)

                    is LongRangeOptionWireFormat ->
                        LongRangeUserStyleSetting.LongRangeOption(wireFormat)

                    else -> throw IllegalArgumentException(
                        "Unknown StyleCategoryWireFormat.OptionWireFormat " +
                            wireFormat::javaClass.name
                    )
                }
        }

        /** @hide */
        @Suppress("HiddenAbstractMethod")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public abstract fun toWireFormat(): OptionWireFormat

        override fun toString(): String =
            try {
                id.value.decodeToString()
            } catch (e: Exception) {
                id.value.toString()
            }
    }

    /**
     * Translates an option name into an option. This will need to be overridden for userStyle
     * categories that can't sensibly be fully enumerated (e.g. a full 24-bit color picker).
     *
     * @param optionId The ID of the option
     * @return An [Option] corresponding to the name. This could either be one of the options from
     * [UserStyleSetting]s or a newly constructed Option depending on the nature of the
     * UserStyleSetting. If optionName is unrecognized then the default value for the setting should
     * be returned.
     */
    public open fun getOptionForId(optionId: ByteArray): Option =
        options.find { it.id.value.contentEquals(optionId) } ?: options[defaultOptionIndex]

    /** A BooleanUserStyleSetting represents a setting with a true and a false setting. */
    public class BooleanUserStyleSetting : UserStyleSetting {

        /**
         * Constructs a [BooleanUserStyleSetting].
         *
         * @param id [Id] for the element, must be unique.
         * @param displayName Localized human readable name for the element, used in the userStyle
         * selection UI.
         * @param description Localized description string displayed under the displayName.
         * @param icon [Icon] for use in the userStyle selection UI.
         * @param affectsWatchFaceLayers Used by the style configuration UI. Describes which watch
         * face rendering layers this style affects.
         * @param defaultValue The default value for this BooleanUserStyleSetting.
         */
        public constructor (
            id: Id,
            displayName: CharSequence,
            description: CharSequence,
            icon: Icon?,
            affectsWatchFaceLayers: Collection<WatchFaceLayer>,
            defaultValue: Boolean
        ) : super(
            id,
            displayName,
            description,
            icon,
            listOf(BooleanOption(true), BooleanOption(false)),
            when (defaultValue) {
                true -> 0
                false -> 1
            },
            affectsWatchFaceLayers
        )

        internal constructor(wireFormat: BooleanUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): BooleanUserStyleSettingWireFormat =
            BooleanUserStyleSettingWireFormat(
                id.value,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectedWatchFaceLayers.map { it.ordinal }
            )

        /** Returns the default value. */
        public fun getDefaultValue(): Boolean = (options[defaultOptionIndex] as BooleanOption).value

        /** Represents a true or false option in the [BooleanUserStyleSetting]. */
        public class BooleanOption : Option {
            public val value: Boolean

            public constructor(value: Boolean) : super(
                Id(ByteArray(1).apply { this[0] = if (value) 1 else 0 })
            ) {
                this.value = value
            }

            internal constructor(
                wireFormat: BooleanOptionWireFormat
            ) : super(Id(wireFormat.mId)) {
                value = wireFormat.mId[0] == 1.toByte()
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat(): BooleanOptionWireFormat =
                BooleanOptionWireFormat(id.value)

            override fun toString(): String = if (id.value[0] == 1.toByte()) "true" else "false"
        }
    }

    /**
     * ComplicationSlotsUserStyleSetting is the recommended [UserStyleSetting] for representing
     * complication slot configuration, options such as the number of active complication slots,
     * their location, etc... The [ComplicationSlotsOption] class allows you to apply a list of
     * [ComplicationSlotOverlay]s on top of the base config as specified by the
     * [androidx.wear.watchface.ComplicationSlot] constructor.
     *
     * The ComplicationsManager listens for style changes with this setting and when a
     * [ComplicationSlotsOption] is selected the overrides are automatically applied. Note its
     * suggested that the default [ComplicationSlotOverlay] (the first entry in the list) does
     * not apply any overrides. Only a single [ComplicationSlotsUserStyleSetting] is permitted in
     * the [UserStyleSchema].
     *
     * Not to be confused with complication data source selection.
     */
    public class ComplicationSlotsUserStyleSetting : UserStyleSetting {

        /**
         * Overrides to be applied to the corresponding androidx.wear.watchface.ComplicationSlot]'s
         * initial config (as specified in it's constructor) when the setting is selected.
         *
         * @param complicationSlotId The id of the [androidx.wear.watchface.ComplicationSlot] to
         * configure.
         * @param enabled If non null, whether the complication should be enabled for this
         * configuration. If null then no changes are made.
         * @param complicationSlotBounds If non null, the [ComplicationSlotBounds] for this
         * configuration. If null then no changes are made.
         * @param accessibilityTraversalIndex If non null the accessibility traversal index
         * for this configuration. This is used to determine the order in which accessibility labels
         * for the watch face are read to the user.
         */
        public class ComplicationSlotOverlay constructor(
            public val complicationSlotId: Int,
            @get:JvmName("isEnabled")
            public val enabled: Boolean? = null,
            public val complicationSlotBounds: ComplicationSlotBounds? = null,
            @SuppressWarnings("AutoBoxing")
            @get:SuppressWarnings("AutoBoxing")
            public val accessibilityTraversalIndex: Int? = null
        ) {
            public class Builder(
                /** The id of the [androidx.wear.watchface.ComplicationSlot] to configure. */
                private val complicationSlotId: Int
            ) {
                private var enabled: Boolean? = null
                private var complicationSlotBounds: ComplicationSlotBounds? = null
                private var accessibilityTraversalIndex: Int? = null

                /** Overrides the complication's enabled flag. */
                public fun setEnabled(enabled: Boolean): Builder = apply {
                    this.enabled = enabled
                }

                /** Overrides the complication's per [ComplicationSlotBounds]. */
                public fun setComplicationSlotBounds(
                    complicationSlotBounds: ComplicationSlotBounds
                ): Builder = apply {
                    this.complicationSlotBounds = complicationSlotBounds
                }

                /**
                 * Overrides the [androidx.wear.watchface.ComplicationSlot]'s accessibility
                 * traversal index. This is used to sort
                 * [androidx.wear.watchface.ContentDescriptionLabel]s. If unset we will order the
                 * complications by their initial accessibilityTraversalIndex (usually the same
                 * as their id).
                 */
                public fun setAccessibilityTraversalIndex(accessibilityTraversalIndex: Int):
                    Builder = apply {
                        this.accessibilityTraversalIndex = accessibilityTraversalIndex
                    }

                public fun build(): ComplicationSlotOverlay =
                    ComplicationSlotOverlay(
                        complicationSlotId,
                        enabled,
                        complicationSlotBounds,
                        accessibilityTraversalIndex
                    )
            }

            internal constructor(
                wireFormat: ComplicationOverlayWireFormat
            ) : this(
                wireFormat.mComplicationSlotId,
                when (wireFormat.mEnabled) {
                    ComplicationOverlayWireFormat.ENABLED_UNKNOWN -> null
                    ComplicationOverlayWireFormat.ENABLED_YES -> true
                    ComplicationOverlayWireFormat.ENABLED_NO -> false
                    else -> throw InvalidParameterException(
                        "Unrecognised wireFormat.mEnabled " + wireFormat.mEnabled
                    )
                },
                wireFormat.mPerComplicationTypeBounds?.let {
                    ComplicationSlotBounds(
                        it.mapKeys { ComplicationType.fromWireType(it.key) }
                    )
                },
                wireFormat.accessibilityTraversalIndex
            )

            internal fun toWireFormat() =
                ComplicationOverlayWireFormat(
                    complicationSlotId,
                    enabled,
                    complicationSlotBounds?.perComplicationTypeBounds?.mapKeys {
                        it.key.toWireComplicationType()
                    },
                    accessibilityTraversalIndex
                )
        }

        /**
         * Constructs a [ComplicationSlotsUserStyleSetting].
         *
         * @param id [Id] for the element, must be unique.
         * @param displayName Localized human readable name for the element, used in the userStyle
         * selection UI.
         * @param description Localized description string displayed under the displayName.
         * @param icon [Icon] for use in the userStyle selection UI.
         * @param complicationConfig The configuration for affected complications.
         * @param affectsWatchFaceLayers Used by the style configuration UI. Describes which watch
         * face rendering layers this style affects, must include
         * [WatchFaceLayer.COMPLICATIONS].
         * @param defaultOption The default option, used when data isn't persisted. Optional
         * parameter which defaults to the first element of [complicationConfig].
         */
        @JvmOverloads
        public constructor (
            id: Id,
            displayName: CharSequence,
            description: CharSequence,
            icon: Icon?,
            complicationConfig: List<ComplicationSlotsOption>,
            affectsWatchFaceLayers: Collection<WatchFaceLayer>,
            defaultOption: ComplicationSlotsOption = complicationConfig.first()
        ) : super(
            id,
            displayName,
            description,
            icon,
            complicationConfig,
            complicationConfig.indexOf(defaultOption),
            affectsWatchFaceLayers
        ) {
            require(affectsWatchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS))
        }

        internal constructor(
            wireFormat: ComplicationsUserStyleSettingWireFormat
        ) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): ComplicationsUserStyleSettingWireFormat =
            ComplicationsUserStyleSettingWireFormat(
                id.value,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectedWatchFaceLayers.map { it.ordinal }
            )

        /**
         * Represents an override to the initial [androidx.wear.watchface.ComplicationSlotsManager]
         * configuration.
         */
        public class ComplicationSlotsOption : Option {
            /**
             * Overlays to be applied when this ComplicationSlotsOption is selected. If this is empty
             * then the net result is the initial complication configuration.
             */
            public val complicationSlotOverlays: Collection<ComplicationSlotOverlay>

            /** Localized human readable name for the setting, used in the style selection UI. */
            public val displayName: CharSequence

            /** Icon for use in the style selection UI. */
            public val icon: Icon?

            /**
             * Constructs a [ComplicationSlotsUserStyleSetting].
             *
             * @param id [Id] for the element, must be unique.
             * @param displayName Localized human readable name for the element, used in the
             * userStyle selection UI.
             * @param icon [Icon] for use in the style selection UI.
             * @param complicationSlotOverlays Overlays to be applied when this
             * ComplicationSlotsOption is selected. If this is empty then the net result is the
             * initial complication configuration.
             */
            public constructor(
                id: Id,
                displayName: CharSequence,
                icon: Icon?,
                complicationSlotOverlays: Collection<ComplicationSlotOverlay>
            ) : super(id) {
                this.complicationSlotOverlays = complicationSlotOverlays
                this.displayName = displayName
                this.icon = icon
            }

            internal constructor(
                wireFormat: ComplicationsOptionWireFormat
            ) : super(Id(wireFormat.mId)) {
                complicationSlotOverlays =
                    wireFormat.mComplicationOverlays.map { ComplicationSlotOverlay(it) }
                displayName = wireFormat.mDisplayName
                icon = wireFormat.mIcon
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat():
                ComplicationsOptionWireFormat =
                    ComplicationsOptionWireFormat(
                        id.value,
                        displayName,
                        icon,
                        complicationSlotOverlays.map { it.toWireFormat() }.toTypedArray()
                    )
        }
    }

    /**
     * A DoubleRangeUserStyleSetting represents a setting with a [Double] value in the range
     * `[minimumValue .. maximumValue]`.
     */
    public class DoubleRangeUserStyleSetting : UserStyleSetting {

        internal companion object {
            internal fun createOptionsList(
                minimumValue: Double,
                maximumValue: Double,
                defaultValue: Double
            ): List<DoubleRangeOption> {
                require(minimumValue < maximumValue)
                require(defaultValue >= minimumValue)
                require(defaultValue <= maximumValue)

                return if (defaultValue != minimumValue && defaultValue != maximumValue) {
                    listOf(
                        DoubleRangeOption(minimumValue),
                        DoubleRangeOption(defaultValue),
                        DoubleRangeOption(maximumValue)
                    )
                } else {
                    listOf(DoubleRangeOption(minimumValue), DoubleRangeOption(maximumValue))
                }
            }
        }

        /**
         * Constructs a [DoubleRangeUserStyleSetting].
         *
         * @param id [Id] for the element, must be unique.
         * @param displayName Localized human readable name for the element, used in the user style
         * selection UI.
         * @param description Localized description string displayed under the displayName.
         * @param icon [Icon] for use in the style selection UI.
         * @param minimumValue Minimum value (inclusive).
         * @param maximumValue Maximum value (inclusive).
         * @param affectsWatchFaceLayers Used by the style configuration UI. Describes which watch
         * face rendering layers this style affects.
         * @param defaultValue The default value for this DoubleRangeUserStyleSetting.
         */
        public constructor (
            id: Id,
            displayName: CharSequence,
            description: CharSequence,
            icon: Icon?,
            minimumValue: Double,
            maximumValue: Double,
            affectsWatchFaceLayers: Collection<WatchFaceLayer>,
            defaultValue: Double
        ) : super(
            id,
            displayName,
            description,
            icon,
            createOptionsList(minimumValue, maximumValue, defaultValue),
            // The index of defaultValue can only ever be 0 or 1.
            when (defaultValue) {
                minimumValue -> 0
                else -> 1
            },
            affectsWatchFaceLayers
        )

        internal constructor(wireFormat: DoubleRangeUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): DoubleRangeUserStyleSettingWireFormat =
            DoubleRangeUserStyleSettingWireFormat(
                id.value,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectedWatchFaceLayers.map { it.ordinal }
            )

        /** Represents an option as a [Double] in the range [minimumValue .. maximumValue]. */
        public class DoubleRangeOption : Option {
            /* The value for this option. Must be within the range [minimumValue .. maximumValue].*/
            public val value: Double

            /**
             * Constructs a [DoubleRangeOption].
             *
             * @param value The value of this [DoubleRangeOption]
             */
            public constructor(value: Double) : super(
                Id(ByteArray(8).apply { ByteBuffer.wrap(this).putDouble(value) })
            ) {
                this.value = value
            }

            internal constructor(
                wireFormat: DoubleRangeOptionWireFormat
            ) : super(Id(wireFormat.mId)) {
                value = ByteBuffer.wrap(wireFormat.mId).double
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat(): DoubleRangeOptionWireFormat =
                DoubleRangeOptionWireFormat(id.value)

            override fun toString(): String = value.toString()
        }

        /** Returns the minimum value. */
        public val minimumValue: Double
            get() = (options.first() as DoubleRangeOption).value

        /** Returns the maximum value. */
        public val maximumValue: Double
            get() = (options.last() as DoubleRangeOption).value

        /** Returns the default value. */
        public val defaultValue: Double
            get() = (options[defaultOptionIndex] as DoubleRangeOption).value

        /** We support all values in the range [min ... max] not just min & max. */
        override fun getOptionForId(optionId: ByteArray): Option =
            options.find { it.id.value.contentEquals(optionId) } ?: checkedOptionForId(optionId)

        private fun checkedOptionForId(optionId: ByteArray): DoubleRangeOption {
            return try {
                val value = ByteBuffer.wrap(optionId).double
                if (value < minimumValue || value > maximumValue) {
                    options[defaultOptionIndex] as DoubleRangeOption
                } else {
                    DoubleRangeOption(value)
                }
            } catch (e: Exception) {
                options[defaultOptionIndex] as DoubleRangeOption
            }
        }
    }

    /** A ListStyleCategory represents a setting with options selected from a List. */
    public open class ListUserStyleSetting : UserStyleSetting {

        /**
         * Constructs a [ListUserStyleSetting].
         *
         * @param id [Id] for the element, must be unique.
         * @param displayName Localized human readable name for the element, used in the userStyle
         * selection UI.
         * @param description Localized description string displayed under the displayName.
         * @param icon [Icon] for use in the userStyle selection UI.
         * @param options List of all options for this ListUserStyleSetting.
         * @param affectsWatchFaceLayers Used by the style configuration UI. Describes which watch
         * face rendering layers this style affects.
         * @param defaultOption The default option, used when data isn't persisted.
         */
        @JvmOverloads
        public constructor (
            id: Id,
            displayName: CharSequence,
            description: CharSequence,
            icon: Icon?,
            options: List<ListOption>,
            affectsWatchFaceLayers: Collection<WatchFaceLayer>,
            defaultOption: ListOption = options.first()
        ) : super(
            id,
            displayName,
            description,
            icon,
            options,
            options.indexOf(defaultOption),
            affectsWatchFaceLayers
        )

        internal constructor(wireFormat: ListUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): ListUserStyleSettingWireFormat =
            ListUserStyleSettingWireFormat(
                id.value,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectedWatchFaceLayers.map { it.ordinal }
            )

        /**
         * Represents choice within a [ListUserStyleSetting], these must be enumerated up front.
         */
        public class ListOption : Option {
            /** Localized human readable name for the setting, used in the style selection UI. */
            public val displayName: CharSequence

            /** Icon for use in the style selection UI. */
            public val icon: Icon?

            /**
             * Constructs a [ListOption].
             *
             * @param id The [Id] of this [ListOption], must be unique within the
             * [ListUserStyleSetting].
             * @param displayName Localized human readable name for the setting, used in the style
             * selection UI.
             * @param icon [Icon] for use in the style selection UI.
             */
            public constructor(id: Id, displayName: CharSequence, icon: Icon?) : super(id) {
                this.displayName = displayName
                this.icon = icon
            }

            internal constructor(
                wireFormat: ListOptionWireFormat
            ) : super(Id(wireFormat.mId)) {
                displayName = wireFormat.mDisplayName
                icon = wireFormat.mIcon
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat(): ListOptionWireFormat =
                ListOptionWireFormat(
                    id.value,
                    displayName,
                    icon
                )
        }
    }

    /**
     * A LongRangeUserStyleSetting represents a setting with a [Long] value in the range
     * [minimumValue .. maximumValue].
     */
    public class LongRangeUserStyleSetting : UserStyleSetting {

        internal companion object {
            internal fun createOptionsList(
                minimumValue: Long,
                maximumValue: Long,
                defaultValue: Long
            ): List<LongRangeOption> {
                require(minimumValue < maximumValue)
                require(defaultValue >= minimumValue)
                require(defaultValue <= maximumValue)

                return if (defaultValue != minimumValue && defaultValue != maximumValue) {
                    listOf(
                        LongRangeOption(minimumValue),
                        LongRangeOption(defaultValue),
                        LongRangeOption(maximumValue)
                    )
                } else {
                    listOf(
                        LongRangeOption(minimumValue),
                        LongRangeOption(maximumValue)
                    )
                }
            }
        }

        /**
         * Constructs a [LongRangeUserStyleSetting].
         *
         * @param id [Id] for the element, must be unique.
         * @param displayName Localized human readable name for the element, used in the userStyle
         * selection UI.
         * @param description Localized description string displayed under the displayName.
         * @param icon [Icon] for use in the userStyle selection UI.
         * @param minimumValue Minimum value (inclusive).
         * @param maximumValue Maximum value (inclusive).
         * @param affectsWatchFaceLayers Used by the style configuration UI. Describes which watch
         * face rendering layers this style affects.
         * @param defaultValue The default value for this LongRangeUserStyleSetting.
         */
        public constructor (
            id: Id,
            displayName: CharSequence,
            description: CharSequence,
            icon: Icon?,
            minimumValue: Long,
            maximumValue: Long,
            affectsWatchFaceLayers: Collection<WatchFaceLayer>,
            defaultValue: Long
        ) : super(
            id,
            displayName,
            description,
            icon,
            createOptionsList(minimumValue, maximumValue, defaultValue),
            // The index of defaultValue can only ever be 0 or 1.
            when (defaultValue) {
                minimumValue -> 0
                else -> 1
            },
            affectsWatchFaceLayers
        )

        internal constructor(wireFormat: LongRangeUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): LongRangeUserStyleSettingWireFormat =
            LongRangeUserStyleSettingWireFormat(
                id.value,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectedWatchFaceLayers.map { it.ordinal }
            )

        /**
         * Represents an option a [Long] in the range [minimumValue .. maximumValue].
         */
        public class LongRangeOption : Option {
            /* The value for this option. Must be within the range [minimumValue..maximumValue]. */
            public val value: Long

            /**
             * Constructs a [LongRangeOption].
             *
             * @param value The value of this [LongRangeOption]
             */
            public constructor(value: Long) : super(
                Id(ByteArray(8).apply { ByteBuffer.wrap(this).putLong(value) })
            ) {
                this.value = value
            }

            internal constructor(
                wireFormat: LongRangeOptionWireFormat
            ) : super(Id(wireFormat.mId)) {
                value = ByteBuffer.wrap(wireFormat.mId).long
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat(): LongRangeOptionWireFormat =
                LongRangeOptionWireFormat(id.value)

            override fun toString(): String = value.toString()
        }

        /** The minimum value. */
        public val minimumValue: Long
            get() = (options.first() as LongRangeOption).value

        /** The maximum value. */
        public val maximumValue: Long
            get() = (options.last() as LongRangeOption).value

        /** The default value. */
        public val defaultValue: Long
            get() = (options[defaultOptionIndex] as LongRangeOption).value

        /**
         * We support all values in the range [min ... max] not just min & max.
         */
        override fun getOptionForId(optionId: ByteArray): Option =
            options.find { it.id.value.contentEquals(optionId) } ?: checkedOptionForId(optionId)

        private fun checkedOptionForId(optionId: ByteArray): LongRangeOption {
            return try {
                val value = ByteBuffer.wrap(optionId).long
                if (value < minimumValue || value > maximumValue) {
                    options[defaultOptionIndex] as LongRangeOption
                } else {
                    LongRangeOption(value)
                }
            } catch (e: Exception) {
                options[defaultOptionIndex] as LongRangeOption
            }
        }
    }

    /**
     * An application specific style setting. This style is ignored by the system editor. This is
     * expected to be used in conjunction with an on watch face editor. Only a single
     * [ComplicationSlotsUserStyleSetting] is permitted in the [UserStyleSchema].
     */
    public class CustomValueUserStyleSetting : UserStyleSetting {
        internal companion object {
            internal const val CUSTOM_VALUE_USER_STYLE_SETTING_ID = "CustomValue"
        }

        /**
         * Constructs a [CustomValueUserStyleSetting].
         *
         * @param affectsWatchFaceLayers Used by the style configuration UI. Describes which watch
         * face rendering layers this style affects.
         * @param defaultValue The default value [ByteArray].
         */
        public constructor (
            affectsWatchFaceLayers: Collection<WatchFaceLayer>,
            defaultValue: ByteArray
        ) : super(
            Id(CUSTOM_VALUE_USER_STYLE_SETTING_ID),
            "",
            "",
            null,
            listOf(CustomValueOption(defaultValue)),
            0,
            affectsWatchFaceLayers
        )

        internal constructor(wireFormat: CustomValueUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): CustomValueUserStyleSettingWireFormat =
            CustomValueUserStyleSettingWireFormat(
                id.value,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                affectedWatchFaceLayers.map { it.ordinal }
            )

        /**
         * An application specific custom value. NB the [CustomValueOption.customValue] is the
         * same as the [CustomValueOption.id].
         */
        public class CustomValueOption : Option {
            /* The [ByteArray] value for this option which is the same as the [id]. */
            public val customValue: ByteArray
                get() = id.value

            /**
             * Constructs a [CustomValueOption].
             *
             * @param customValue The [ByteArray] [id] and value of this [CustomValueOption]. This
             * may not exceed [Id.MAX_LENGTH].
             */
            public constructor(customValue: ByteArray) : super(Id(customValue))

            internal constructor(
                wireFormat: CustomValueOptionWireFormat
            ) : super(Id(wireFormat.mId))

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat(): CustomValueOptionWireFormat =
                CustomValueOptionWireFormat(id.value)
        }

        override fun getOptionForId(optionId: ByteArray): Option =
            options.find { it.id.value.contentEquals(optionId) } ?: CustomValueOption(optionId)
    }
}
