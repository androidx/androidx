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
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationOverlay
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationsOption
import androidx.wear.watchface.style.data.BooleanUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.ComplicationsUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.DoubleRangeUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.ListUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.LongRangeUserStyleSettingWireFormat
import androidx.wear.watchface.style.data.UserStyleSettingWireFormat
import java.security.InvalidParameterException

/**
 * Watch faces often have user configurable styles, the definition of what is a style is left up to
 * the watch face but it typically incorporates a variety of settings such as: color, visual theme
 * for watch hands, font, tick shape, complications, audio elements, etc...
 *
 * A UserStyleSetting represents one of these dimensions. See also [UserStyleSchema] which defines
 * the list of UserStyleSettings provided by the watch face.
 */
public sealed class UserStyleSetting(
    /** Identifier for the element, must be unique. */
    public val id: String,

    /** Localized human readable name for the element, used in the userStyle selection UI. */
    public val displayName: CharSequence,

    /** Localized description string displayed under the displayName. */
    public val description: CharSequence,

    /** Icon for use in the style selection UI. */
    public val icon: Icon?,

    /**
     * List of options for this UserStyleSetting. Depending on the type of UserStyleSetting this
     * may be an exhaustive list, or just examples to populate a ListView in case the
     * UserStyleSetting isn't supported by the UI (e.g. a new WatchFace with an old Companion).
     */
    public val options: List<Option>,

    /**
     * The default option index, used if nothing has been selected within the [options] list.
     */
    public val defaultOptionIndex: Int,

    /**
     * Used by the style configuration UI. Describes which rendering layers this style affects.
     */
    public val affectsLayers: Collection<Layer>
) {
    internal companion object {
        internal fun createFromWireFormat(
            wireFormat: UserStyleSettingWireFormat
        ): UserStyleSetting = when (wireFormat) {
            is BooleanUserStyleSettingWireFormat -> BooleanUserStyleSetting(wireFormat)

            is ComplicationsUserStyleSettingWireFormat ->
                ComplicationsUserStyleSetting(wireFormat)

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

    internal fun getSettingOptionForId(id: String?) =
        if (id == null) {
            options[defaultOptionIndex]
        } else {
            getOptionForId(id)
        }

    private constructor(wireFormat: UserStyleSettingWireFormat) : this(
        wireFormat.mId,
        wireFormat.mDisplayName,
        wireFormat.mDescription,
        wireFormat.mIcon,
        wireFormat.mOptions.map { Option.createFromWireFormat(it) },
        wireFormat.mDefaultOptionIndex,
        wireFormat.mAffectsLayers.map { Layer.values()[it] }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public abstract fun toWireFormat(): UserStyleSettingWireFormat

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun getWireFormatOptionsList(): List<UserStyleSettingWireFormat.OptionWireFormat> =
        options.map { it.toWireFormat() }

    /** Returns the default for when the user hasn't selected an option. */
    public fun getDefaultOption(): Option = options[defaultOptionIndex]

    /**
     * Represents a choice within a style setting.
     *
     * @property id Machine readable identifier for the style setting.
     */
    public abstract class Option(
        /** Identifier for the option, must be unique within the UserStyleSetting. */
        public val id: String
    ) {
        public companion object {

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            public fun createFromWireFormat(
                wireFormat: UserStyleSettingWireFormat.OptionWireFormat
            ): Option =
                when (wireFormat) {
                    is BooleanUserStyleSettingWireFormat.BooleanOptionWireFormat ->
                        BooleanUserStyleSetting.BooleanOption(wireFormat)

                    is ComplicationsUserStyleSettingWireFormat.ComplicationsOptionWireFormat ->
                        ComplicationsUserStyleSetting.ComplicationsOption(wireFormat)

                    is DoubleRangeUserStyleSettingWireFormat.DoubleRangeOptionWireFormat ->
                        DoubleRangeUserStyleSetting.DoubleRangeOption(wireFormat)

                    is ListUserStyleSettingWireFormat.ListOptionWireFormat ->
                        ListUserStyleSetting.ListOption(wireFormat)

                    is LongRangeUserStyleSettingWireFormat.LongRangeOptionWireFormat ->
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
        public abstract fun toWireFormat(): UserStyleSettingWireFormat.OptionWireFormat
    }

    /**
     * Translates an option name into an option. This will need to be overridden for userStyle
     * categories that can't sensibly be fully enumerated (e.g. a full 24-bit color picker).
     *
     * @param optionId The ID of the option
     * @return An [Option] corresponding to the name. This could either be one of the
     *     options from userStyleSettings or a newly constructed Option depending on the nature
     *     of the UserStyleSetting. If optionName is unrecognized then the default value for the
     *     setting should be returned.
     */
    public open fun getOptionForId(optionId: String): Option =
        options.find { it.id == optionId } ?: options[defaultOptionIndex]

    /** A BooleanUserStyleSetting represents a setting with a true and a false setting. */
    public class BooleanUserStyleSetting : UserStyleSetting {

        public constructor (
            /** Identifier for the element, must be unique. */
            id: String,

            /**
             * Localized human readable name for the element, used in the userStyle selection UI.
             */
            displayName: CharSequence,

            /** Localized description string displayed under the displayName. */
            description: CharSequence,

            /** Icon for use in the userStyle selection UI. */
            icon: Icon?,

            /** The default value for this BooleanUserStyleSetting. */
            defaultValue: Boolean,

            /**
             * Used by the style configuration UI. Describes which rendering layers this style
             * affects.
             */
            affectsLayers: Collection<Layer>
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
            affectsLayers
        )

        internal constructor(wireFormat: BooleanUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): BooleanUserStyleSettingWireFormat =
            BooleanUserStyleSettingWireFormat(
                id,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectsLayers.map { it.ordinal }
            )

        /** Returns the default value. */
        public fun getDefaultValue(): Boolean = (options[defaultOptionIndex] as BooleanOption).value

        /** Represents a true or false option in the [BooleanUserStyleSetting]. */
        public class BooleanOption : Option {
            public val value: Boolean

            public constructor(value: Boolean) : super(value.toString()) {
                this.value = value
            }

            internal constructor(
                wireFormat: BooleanUserStyleSettingWireFormat.BooleanOptionWireFormat
            ) : super(wireFormat.mId) {
                value = wireFormat.mValue
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat(): BooleanUserStyleSettingWireFormat.BooleanOptionWireFormat =
                BooleanUserStyleSettingWireFormat.BooleanOptionWireFormat(id, value)
        }
    }

    /**
     * ComplicationsUserStyleSetting is the recommended [UserStyleSetting] for representing
     * complication configuration options such as the number of active complications, their
     * location, etc... The [ComplicationsOption] class allows you to apply a list of
     * [ComplicationOverlay]s on top of the base config as specified by the
     * [androidx.wear.watchface.Complication] constructor.
     *
     * The ComplicationsManager listens for style changes with this setting and when a
     * [ComplicationsOption] is selected the overrides are automatically applied. Note its suggested
     * that the default [ComplicationOverlay] (the first entry in the list) does not apply any
     * overrides.
     *
     * Not to be confused with complication provider selection.
     */
    public class ComplicationsUserStyleSetting : UserStyleSetting {

        /**
         * Overrides to be applied to the corresponding complication's initial config (as specified
         * in [androidx.wear.watchface.Complication]) when the setting is selected.
         */
        public class ComplicationOverlay constructor(
            /** The id of the complication to configure. */
            public val complicationId: Int,

            /**
             * If non null, whether the complication should be enabled for this configuration. If
             * null then no changes are made.
             */
            @get:JvmName("isEnabled")
            public val enabled: Boolean? = null,

            /**
             * If non null, the new [ComplicationBounds] for this configuration. If null then no
             * changes are made.
             */
            public val complicationBounds: ComplicationBounds? = null,

            /**
             * If non null, the new types of complication supported by this complication for this
             * configuration. If null then no changes are made.
             */
            public val supportedTypes: List<ComplicationType>? = null,

            /**
             * If non null, the new default complication provider for this configuration. If null
             * then no changes are made.
             */
            public val defaultProviderPolicy: DefaultComplicationProviderPolicy? = null,

            /**
             * If non null, the new default complication provider data type. If null then no changes
             * are made.
             */
            public val defaultProviderType: ComplicationType? = null
        ) {
            public class Builder(
                /** The id of the complication to configure. */
                private val complicationId: Int
            ) {
                private var enabled: Boolean? = null
                private var complicationBounds: ComplicationBounds? = null
                private var supportedTypes: List<ComplicationType>? = null
                private var defaultComplicationProviderPolicy: DefaultComplicationProviderPolicy? =
                    null
                private var defaultComplicationProviderType: ComplicationType? = null

                /** Overrides the complication's enabled flag. */
                public fun setEnabled(enabled: Boolean): Builder = apply {
                    this.enabled = enabled
                }

                /** Overrides the complication's per [ComplicationBounds]. */
                public fun setComplicationBounds(complicationBounds: ComplicationBounds): Builder =
                    apply {
                        this.complicationBounds = complicationBounds
                    }

                /** Overrides the complication's supported complication types. */
                public fun setSupportedTypes(supportedTypes: List<ComplicationType>): Builder =
                    apply {
                        this.supportedTypes = supportedTypes
                    }

                /** Overrides the complication's [DefaultComplicationProviderPolicy]. */
                public fun setDefaultProviderPolicy(
                    defaultComplicationProviderPolicy: DefaultComplicationProviderPolicy?
                ): Builder = apply {
                    this.defaultComplicationProviderPolicy = defaultComplicationProviderPolicy
                }

                /**
                 * Overrides the default complication provider data type.
                 */
                public fun setDefaultProviderType(
                    defaultComplicationProviderType: ComplicationType
                ): Builder = apply {
                    this.defaultComplicationProviderType = defaultComplicationProviderType
                }

                public fun build(): ComplicationOverlay =
                    ComplicationOverlay(
                        complicationId,
                        enabled,
                        complicationBounds,
                        supportedTypes,
                        defaultComplicationProviderPolicy,
                        defaultComplicationProviderType
                    )
            }

            internal constructor(
                wireFormat: ComplicationsUserStyleSettingWireFormat.ComplicationOverlayWireFormat
            ) : this(
                wireFormat.mComplicationId,
                when (wireFormat.mEnabled) {
                    ComplicationsUserStyleSettingWireFormat.ComplicationOverlayWireFormat
                        .ENABLED_UNKNOWN -> null
                    ComplicationsUserStyleSettingWireFormat.ComplicationOverlayWireFormat
                        .ENABLED_YES -> true
                    ComplicationsUserStyleSettingWireFormat.ComplicationOverlayWireFormat
                        .ENABLED_NO -> false
                    else -> throw InvalidParameterException(
                        "Unrecognised wireFormat.mEnabled " + wireFormat.mEnabled
                    )
                },
                wireFormat.mPerComplicationTypeBounds?.let { ComplicationBounds(it) },
                wireFormat.mSupportedTypes?.let { ComplicationType.fromWireTypeList(it) },
                wireFormat.mDefaultProviders?.let {
                    DefaultComplicationProviderPolicy(it, wireFormat.mSystemProviderFallback)
                },
                if (wireFormat.mDefaultProviderType !=
                    ComplicationsUserStyleSettingWireFormat.ComplicationOverlayWireFormat
                        .NO_DEFAULT_PROVIDER_TYPE
                ) {
                    ComplicationType.fromWireType(wireFormat.mDefaultProviderType)
                } else {
                    null
                }
            )

            internal fun toWireFormat() =
                ComplicationsUserStyleSettingWireFormat.ComplicationOverlayWireFormat(
                    complicationId,
                    enabled,
                    complicationBounds?.perComplicationTypeBounds,
                    supportedTypes?.let { ComplicationType.toWireTypes(it) },
                    defaultProviderPolicy?.providersAsList(),
                    defaultProviderPolicy?.systemProviderFallback,
                    defaultProviderType?.asWireComplicationType()
                )
        }

        public constructor (
            /** Identifier for the element, must be unique. */
            id: String,

            /**
             * Localized human readable name for the element, used in the userStyle selection UI.
             */
            displayName: CharSequence,

            /** Localized description string displayed under the displayName. */
            description: CharSequence,

            /** Icon for use in the userStyle selection UI. */
            icon: Icon?,

            /**
             * The configuration for affected complications. The first entry is the default value
             */
            complicationConfig: List<ComplicationsOption>,

            /**
             * Used by the style configuration UI. Describes which rendering layers this style
             * affects, must include [Layer.COMPLICATIONS].
             */
            affectsLayers: Collection<Layer>
        ) : super(
            id,
            displayName,
            description,
            icon,
            complicationConfig,
            0,
            affectsLayers
        ) {
            require(affectsLayers.contains(Layer.COMPLICATIONS))
        }

        internal constructor(
            wireFormat: ComplicationsUserStyleSettingWireFormat
        ) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): ComplicationsUserStyleSettingWireFormat =
            ComplicationsUserStyleSettingWireFormat(
                id,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectsLayers.map { it.ordinal }
            )

        /** Represents an override to the initial complication configuration. */
        public class ComplicationsOption : Option {
            /**
             * Overlays to be applied when this ComplicationsOption is selected. If this is empty
             * then the net result is the initial complication configuration.
             */
            public val complicationOverlays: Collection<ComplicationOverlay>

            /** Localized human readable name for the setting, used in the style selection UI. */
            public val displayName: CharSequence

            /** Icon for use in the style selection UI. */
            public val icon: Icon?

            public constructor(
                id: String,
                displayName: CharSequence,
                icon: Icon?,
                complicationOverlays: Collection<ComplicationOverlay>
            ) : super(id) {
                this.complicationOverlays = complicationOverlays
                this.displayName = displayName
                this.icon = icon
            }

            internal constructor(
                wireFormat: ComplicationsUserStyleSettingWireFormat.ComplicationsOptionWireFormat
            ) : super(wireFormat.mId) {
                complicationOverlays =
                    wireFormat.mComplicationOverlays.map { ComplicationOverlay(it) }
                displayName = wireFormat.mDisplayName
                icon = wireFormat.mIcon
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat():
                ComplicationsUserStyleSettingWireFormat.ComplicationsOptionWireFormat =
                    ComplicationsUserStyleSettingWireFormat.ComplicationsOptionWireFormat(
                        id,
                        displayName,
                        icon,
                        complicationOverlays.map { it.toWireFormat() }.toTypedArray()
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

        public constructor (
            /** Identifier for the element, must be unique. */
            id: String,

            /**
             * Localized human readable name for the element, used in the userStyle selection UI.
             */
            displayName: CharSequence,

            /** Localized description string displayed under the displayName. */
            description: CharSequence,

            /** Icon for use in the userStyle selection UI. */
            icon: Icon?,

            /** Minimum value (inclusive). */
            minimumValue: Double,

            /** Maximum value (inclusive). */
            maximumValue: Double,

            /** The default value for this DoubleRangeUserStyleSetting. */
            defaultValue: Double,

            /**
             * Used by the style configuration UI. Describes which rendering layers this style
             * affects.
             */
            affectsLayers: Collection<Layer>
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
            affectsLayers
        )

        internal constructor(wireFormat: DoubleRangeUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): DoubleRangeUserStyleSettingWireFormat =
            DoubleRangeUserStyleSettingWireFormat(
                id,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectsLayers.map { it.ordinal }
            )

        /** Represents an option as a [Double] in the range [minimumValue .. maximumValue]. */
        public class DoubleRangeOption : Option {
            /* The value for this option. Must be within the range [minimumValue .. maximumValue].*/
            public val value: Double

            public constructor(value: Double) : super(value.toString()) {
                this.value = value
            }

            internal companion object {
                internal const val KEY_DOUBLE_VALUE = "KEY_DOUBLE_VALUE"
            }

            internal constructor(
                wireFormat: DoubleRangeUserStyleSettingWireFormat.DoubleRangeOptionWireFormat
            ) : super(wireFormat.mId) {
                value = wireFormat.mValue
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat():
                DoubleRangeUserStyleSettingWireFormat.DoubleRangeOptionWireFormat =
                    DoubleRangeUserStyleSettingWireFormat.DoubleRangeOptionWireFormat(id, value)
        }

        /** Returns the minimum value. */
        public fun getMinimumValue(): Double = (options.first() as DoubleRangeOption).value

        /** Returns the maximum value. */
        public fun getMaximumValue(): Double = (options.last() as DoubleRangeOption).value

        /** Returns the default value. */
        public fun getDefaultValue(): Double =
            (options[defaultOptionIndex] as DoubleRangeOption).value

        /** We support all values in the range [min ... max] not just min & max. */
        override fun getOptionForId(optionId: String): Option =
            options.find { it.id == optionId } ?: checkedOptionForId(optionId)

        private fun checkedOptionForId(optionId: String): DoubleRangeOption {
            return try {
                val value = optionId.toDouble()
                if (value < getMinimumValue() || value > getMaximumValue()) {
                    options[defaultOptionIndex] as DoubleRangeOption
                } else {
                    DoubleRangeOption(value)
                }
            } catch (e: NumberFormatException) {
                options[defaultOptionIndex] as DoubleRangeOption
            }
        }
    }

    /** A ListStyleCategory represents a setting with options selected from a List. */
    public open class ListUserStyleSetting : UserStyleSetting {

        @JvmOverloads
        public constructor (
            /** Identifier for the element, must be unique. */
            id: String,

            /** Localized human readable name for the element, used in the userStyle selection UI.*/
            displayName: CharSequence,

            /** Localized description string displayed under the displayName. */
            description: CharSequence,

            /** Icon for use in the userStyle selection UI. */
            icon: Icon?,

            /** List of all options for this ListUserStyleSetting. */
            options: List<ListOption>,

            /**
             * Used by the style configuration UI. Describes which rendering layers this style
             * affects.
             */
            affectsLayers: Collection<Layer>,

            /** The default option, used when data isn't persisted. */
            defaultOption: ListOption = options.first()
        ) : super(
            id,
            displayName,
            description,
            icon,
            options,
            options.indexOf(defaultOption),
            affectsLayers
        )

        internal constructor(wireFormat: ListUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): ListUserStyleSettingWireFormat =
            ListUserStyleSettingWireFormat(
                id,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectsLayers.map { it.ordinal }
            )

        /**
         * Represents choice within a [ListUserStyleSetting], these must be enumerated up front.
         */
        public class ListOption : Option {
            /** Localized human readable name for the setting, used in the style selection UI. */
            public val displayName: CharSequence

            /** Icon for use in the style selection UI. */
            public val icon: Icon?

            public constructor(id: String, displayName: CharSequence, icon: Icon?) : super(id) {
                this.displayName = displayName
                this.icon = icon
            }

            internal constructor(
                wireFormat: ListUserStyleSettingWireFormat.ListOptionWireFormat
            ) : super(wireFormat.mId) {
                displayName = wireFormat.mDisplayName
                icon = wireFormat.mIcon
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat(): ListUserStyleSettingWireFormat.ListOptionWireFormat =
                ListUserStyleSettingWireFormat.ListOptionWireFormat(id, displayName, icon)
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

        public constructor (
            /** Identifier for the element, must be unique. */
            id: String,

            /** Localized human readable name for the element, used in the userStyle selection UI.*/
            displayName: CharSequence,

            /** Localized description string displayed under the displayName. */
            description: CharSequence,

            /** Icon for use in the userStyle selection UI. */
            icon: Icon?,

            /** Minimum value (inclusive). */
            minimumValue: Long,

            /** Maximum value (inclusive). */
            maximumValue: Long,

            /** The default value for this LongRangeUserStyleSetting. */
            defaultValue: Long,

            /**
             * Used by the style configuration UI. Describes which rendering layers this style
             * affects.
             */
            affectsLayers: Collection<Layer>
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
            affectsLayers
        )

        internal constructor(wireFormat: LongRangeUserStyleSettingWireFormat) : super(wireFormat)

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat(): LongRangeUserStyleSettingWireFormat =
            LongRangeUserStyleSettingWireFormat(
                id,
                displayName,
                description,
                icon,
                getWireFormatOptionsList(),
                defaultOptionIndex,
                affectsLayers.map { it.ordinal }
            )

        /**
         * Represents an option a [Long] in the range [minimumValue .. maximumValue].
         */
        public class LongRangeOption : Option {
            /* The value for this option. Must be within the range [minimumValue..maximumValue]. */
            public val value: Long

            public constructor(value: Long) : super(value.toString()) {
                this.value = value
            }

            internal companion object {
                internal const val KEY_LONG_VALUE = "KEY_LONG_VALUE"
            }

            internal constructor(
                wireFormat: LongRangeUserStyleSettingWireFormat.LongRangeOptionWireFormat
            ) : super(wireFormat.mId) {
                value = wireFormat.mValue
            }

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
            override fun toWireFormat():
                LongRangeUserStyleSettingWireFormat.LongRangeOptionWireFormat =
                    LongRangeUserStyleSettingWireFormat.LongRangeOptionWireFormat(id, value)
        }

        /**
         * Returns the minimum value.
         */
        public fun getMinimumValue(): Long = (options.first() as LongRangeOption).value

        /**
         * Returns the maximum value.
         */
        public fun getMaximumValue(): Long = (options.last() as LongRangeOption).value

        /**
         * Returns the default value.
         */
        public fun getDefaultValue(): Long = (options[defaultOptionIndex] as LongRangeOption).value

        /**
         * We support all values in the range [min ... max] not just min & max.
         */
        override fun getOptionForId(optionId: String): Option =
            options.find { it.id == optionId } ?: checkedOptionForId(optionId)

        private fun checkedOptionForId(optionId: String): LongRangeOption {
            return try {
                val value = optionId.toLong()
                if (value < getMinimumValue() || value > getMaximumValue()) {
                    options[defaultOptionIndex] as LongRangeOption
                } else {
                    LongRangeOption(value)
                }
            } catch (e: NumberFormatException) {
                options[defaultOptionIndex] as LongRangeOption
            }
        }
    }
}
