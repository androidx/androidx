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

import android.graphics.RectF
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import androidx.annotation.RestrictTo
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.watchface.style.data.ComplicationsUserStyleCategoryWireFormat
import androidx.wear.watchface.style.data.ComplicationsUserStyleCategoryWireFormat.ComplicationConfigWireFormat
import androidx.wear.watchface.style.data.ComplicationsUserStyleCategoryWireFormat.ComplicationsOptionWireFormat
import java.security.InvalidParameterException

/**
 * ComplicationsUserStyleCategory is the recommended [UserStyleCategory] for representing
 * complication configuration options such as the number of active complications, their location,
 * etc...
 *
 * The ComplicationsManager listens for style changes with this category and applies them
 * automatically.
 *
 * Not to be confused with complication provider selection.
 */
class ComplicationsUserStyleCategory : UserStyleCategory {

    /**
     * Overrides to be applied to the corresponding complication's initial config (as specified in
     * [androidx.wear.watchface.Complication]) when the category is selected.
     */
    class ComplicationOverride internal constructor(
        /** The id of the complication to configure. */
        val complicationId: Int,

        /**
         * If non null, whether the complication should be enabled for this configuration. If null
         * then no changes are made.
         */
        @get:JvmName("isEnabled")
        val enabled: Boolean?,

        /**
         * If non null, the new unit square screen space complication bounds for this configuration.
         * If null then no changes are made.
         */
        val bounds: RectF?,

        /**
         * If non null, the new types of complication supported by this complication for this
         * configuration. If null then no changes are made.
         */
        val supportedTypes: IntArray?,

        /**
         * If non null, the new default complication provider for this configuration. If null then
         * no changes are made.
         */
        val defaultProviderPolicy: DefaultComplicationProviderPolicy?,

        /**
         * If non null, the new default complication provider data type. See
         * [ComplicationData .ComplicationType].  If null then no changes are made.
         */
        @get:SuppressWarnings("AutoBoxing")
        val defaultProviderType: Int?
    ) {
        class Builder(
            /** The id of the complication to configure. */
            private val complicationId: Int
        ) {
            private var enabled: Boolean? = null
            private var bounds: RectF? = null
            private var supportedTypes: IntArray? = null
            private var defaultComplicationProviderPolicy: DefaultComplicationProviderPolicy? = null
            private var defaultComplicationProviderType: Int? = null

            /** Overrides the complication's enabled flag. */
            fun setEnabled(enabled: Boolean) = apply {
                this.enabled = enabled
            }

            /** Overrides the complication's unit-square screen space bounds. */
            fun setBounds(bounds: RectF) = apply {
                this.bounds = bounds
            }

            /** Overrides the complication's supported complication types. */
            fun setSupportedTypes(supportedTypes: IntArray) = apply {
                this.supportedTypes = supportedTypes
            }

            /** Overrides the complication's [DefaultComplicationProviderPolicy]. */
            fun setDefaultProviderPolicy(
                defaultComplicationProviderPolicy: DefaultComplicationProviderPolicy?
            ) = apply {
                this.defaultComplicationProviderPolicy = defaultComplicationProviderPolicy
            }

            /**
             * Overrides the default complication provider data type. See
             * [ComplicationData.ComplicationType]
             */
            fun setDefaultProviderType(
                @ComplicationData.ComplicationType defaultComplicationProviderType: Int
            ) = apply {
                this.defaultComplicationProviderType = defaultComplicationProviderType
            }

            fun build() = ComplicationOverride(
                complicationId,
                enabled,
                bounds,
                supportedTypes,
                defaultComplicationProviderPolicy,
                defaultComplicationProviderType
            )
        }

        internal constructor(
            wireFormat: ComplicationConfigWireFormat
        ) : this(
            wireFormat.mComplicationId,
            when (wireFormat.mEnabled) {
                ComplicationConfigWireFormat.ENABLED_UNKNOWN -> null
                ComplicationConfigWireFormat.ENABLED_YES -> true
                ComplicationConfigWireFormat.ENABLED_NO -> false
                else -> throw InvalidParameterException(
                    "Unrecognised wireFormat.mEnabled " + wireFormat.mEnabled
                )
            },
            wireFormat.mBounds,
            wireFormat.mSupportedTypes,
            wireFormat.mDefaultProviders?.let {
                DefaultComplicationProviderPolicy(it, wireFormat.mSystemProviderFallback)
            },
            if (wireFormat.mDefaultProviderType !=
                ComplicationConfigWireFormat.NO_DEFAULT_PROVIDER_TYPE
            ) {
                wireFormat.mDefaultProviderType
            } else {
                null
            }
        )

        internal fun toWireFormat() =
            ComplicationConfigWireFormat(
                complicationId,
                enabled,
                bounds,
                supportedTypes,
                defaultProviderPolicy?.providersAsList(),
                defaultProviderPolicy?.systemProviderFallback,
                defaultProviderType
            )
    }

    constructor (
        /** Identifier for the element, must be unique. */
        id: String,

        /** Localized human readable name for the element, used in the userStyle selection UI. */
        displayName: String,

        /** Localized description string displayed under the displayName. */
        description: String,

        /** Icon for use in the userStyle selection UI. */
        icon: Icon?,

        /**
         * The configuration for affected complications. The first entry is the default value
         */
        complicationConfig: List<ComplicationsOption>,

        /**
         * Used by the style configuration UI. Describes which rendering layers this style affects,
         * must include [Layer.COMPLICATIONS].
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

    internal constructor(wireFormat: ComplicationsUserStyleCategoryWireFormat) : super(wireFormat)

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toWireFormat() =
        ComplicationsUserStyleCategoryWireFormat(
            id,
            displayName,
            description,
            icon,
            getWireFormatOptionsList(),
            defaultOptionIndex,
            affectsLayers.map { it.ordinal }
        )

    /** Represents an override to the initial complication configuration. */
    open class ComplicationsOption : Option {
        /**
         * Overrides to be applied when this ComplicationsOption is selected. If this is empty
         * then the net result is the initial complication configuration.
         */
        val complications: Collection<ComplicationOverride>

        /** Localized human readable name for the setting, used in the style selection UI. */
        val displayName: String

        /** Icon for use in the style selection UI. */
        val icon: Icon?

        constructor(
            id: String,
            displayName: String,
            icon: Icon?,
            complications: Collection<ComplicationOverride>
        ) : super(id) {
            this.complications = complications
            this.displayName = displayName
            this.icon = icon
        }

        internal constructor(wireFormat: ComplicationsOptionWireFormat) : super(wireFormat.mId) {
            complications = wireFormat.mComplications.map { ComplicationOverride(it) }
            displayName = wireFormat.mDisplayName
            icon = wireFormat.mIcon
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun toWireFormat() =
            ComplicationsOptionWireFormat(
                id,
                displayName,
                icon,
                complications.map { it.toWireFormat() }.toTypedArray()
            )
    }
}