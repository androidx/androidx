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
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.ComplicationStateWireFormat

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

    /** Whether or not the complication is drawn. */
    @get:JvmName("isEnabled")
    public val isEnabled: Boolean
) {
    internal constructor(
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
        complicationStateWireFormat.isEnabled
    )
}