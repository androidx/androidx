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

package androidx.wear.watchface.client

import android.graphics.Rect
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationOverlay
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationsOption

/** State of the [Complication]. */
public class ComplicationState(
    /** Screen space bounds of the [Complication] in pixels. */
    public val bounds: Rect,

    /** The type of the complication's bounds. */
    @ComplicationBoundsType public val boundsType: Int,

    /** The [ComplicationType]s supported by this complication. */
    public val supportedTypes: List<ComplicationType>,

    /** The [DefaultComplicationProviderPolicy] for this complication. */
    public val defaultProviderPolicy: DefaultComplicationProviderPolicy,

    /** The default [ComplicationType] for this complication. */
    public val defaultProviderType: ComplicationType,

    /** Whether or not the complication is currently enabled (i.e. it should be drawn). */
    @get:JvmName("isEnabled")
    public val isEnabled: Boolean,

    /**
     * Whether or not the complication was initially enabled before considering any
     * [ComplicationsOption] whose [ComplicationOverlay]s may enable or disable complications.
     */
    @get:JvmName("isInitiallyEnabled")
    public val isInitiallyEnabled: Boolean,

    /** The [ComplicationType] of the complication's current [ComplicationData]. */
    public val currentType: ComplicationType,

    /** Whether or not the complication provider is fixed (i.e the user can't configure it). */
    @get:JvmName("isFixedComplicationProvider")
    public val fixedComplicationProvider: Boolean,

    /** Extras to be merged into the Intent sent when invoking the provider chooser activity. */
    public val complicationConfigExtras: Bundle
) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        complicationStateWireFormat: ComplicationStateWireFormat
    ) : this(
        complicationStateWireFormat.bounds,
        complicationStateWireFormat.boundsType,
        complicationStateWireFormat.supportedTypes.map { ComplicationType.fromWireType(it) },
        DefaultComplicationProviderPolicy(
            complicationStateWireFormat.defaultProvidersToTry ?: emptyList(),
            complicationStateWireFormat.fallbackSystemProvider
        ),
        ComplicationType.fromWireType(complicationStateWireFormat.defaultProviderType),
        complicationStateWireFormat.isEnabled,
        complicationStateWireFormat.isInitiallyEnabled,
        ComplicationType.fromWireType(complicationStateWireFormat.currentType),
        complicationStateWireFormat.isFixedComplicationProvider,
        complicationStateWireFormat.complicationConfigExtras
    )
}