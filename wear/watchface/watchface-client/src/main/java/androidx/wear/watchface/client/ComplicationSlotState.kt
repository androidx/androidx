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
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay

/**
 * State of the [ComplicationSlot].
 *
 * @param bounds Screen space bounds of the [ComplicationSlot] in pixels.
 * @param boundsType The type of the complication's bounds.
 * @param supportedTypes The [ComplicationType]s supported by this complication.
 * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] for this complication.
 * @param defaultDataSourceType The default [ComplicationType] for this complication.
 * @param isEnabled Whether or not the complication is currently enabled (i.e. it should be drawn).
 * @param isInitiallyEnabled Whether or not the complication was initially enabled before
 * considering any [ComplicationSlotsOption] whose [ComplicationSlotOverlay]s may enable or disable
 * complicationSlots.
 * @param currentType The [ComplicationType] of the complication's current [ComplicationData].
 * @param fixedComplicationDataSource Whether or not the complication data source is fixed (i.e the
 * user can't configure it).
 * @param complicationConfigExtras Extras to be merged into the Intent sent when invoking the
 * complication data source chooser activity.
 */
public class ComplicationSlotState(
    public val bounds: Rect,
    @ComplicationSlotBoundsType public val boundsType: Int,
    public val supportedTypes: List<ComplicationType>,
    public val defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
    public val defaultDataSourceType: ComplicationType,
    @get:JvmName("isEnabled")
    public val isEnabled: Boolean,
    @get:JvmName("isInitiallyEnabled")
    public val isInitiallyEnabled: Boolean,
    public val currentType: ComplicationType,
    @get:JvmName("isFixedComplicationDataSource")
    public val fixedComplicationDataSource: Boolean,
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
        DefaultComplicationDataSourcePolicy(
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
