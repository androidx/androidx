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
import androidx.wear.watchface.BoundingArc
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotBoundsTypeIntDef
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption

/** A snapshot of the state of a watch face [ComplicationSlot]. */
public class ComplicationSlotState {
    /** Screen space bounds of the [ComplicationSlot] in pixels. */
    public val bounds: Rect

    /** The type of the complication's bounds. */
    @ComplicationSlotBoundsTypeIntDef public val boundsType: Int

    /** The [ComplicationType]s supported by this complication. */
    public val supportedTypes: List<ComplicationType>

    /** The [DefaultComplicationDataSourcePolicy] for this complication slot. */
    public val defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy

    /** The default [ComplicationType] for this complication. */
    @Deprecated("Use defaultDataSourcePolicy.systemDataSourceFallbackDefaultType instead")
    public val defaultDataSourceType: ComplicationType
        get() = defaultDataSourcePolicy.systemDataSourceFallbackDefaultType

    /** Whether or not the complication is currently enabled (i.e. it should be drawn. */
    @get:JvmName("isEnabled") public val isEnabled: Boolean

    /**
     * Whether or not the complication was initially enabled before considering any
     * [ComplicationSlotsOption] whose [ComplicationSlotOverlay]s may enable or disable
     * complicationSlots.
     */
    @get:JvmName("isInitiallyEnabled") public val isInitiallyEnabled: Boolean

    /** The [ComplicationType] of the complication's current [ComplicationData]. */
    public val currentType: ComplicationType

    /** Whether or not the complication data source is fixed (i.e the user can't configure it). */
    public val fixedComplicationDataSource: Boolean

    /**
     * Extras to be merged into the Intent sent when invoking the complication data source chooser
     * activity.
     */
    public val complicationConfigExtras: Bundle

    /**
     * The ID of a string resource (or `null` if absent) to identify the complication slot visually
     * in an editor. This is supposed to be short and without the word complication in it.
     */
    @get:Suppress("AutoBoxing") public val nameResourceId: Int?

    /**
     * The ID of a string resource (or `null` if absent) to identify the complication slot in a
     * screen reader. This is supposed to be a complete sentence.
     */
    @get:Suppress("AutoBoxing") public val screenReaderNameResourceId: Int?

    @OptIn(ComplicationExperimental::class) private val boundingArc: BoundingArc?

    /** Describes the geometry of an edge complication if specified, or `null` otherwise. */
    // TODO(b/230364881): Make this a normal primary property when BoundingArc is no longer
    // experimental.
    @ComplicationExperimental public fun getBoundingArc(): BoundingArc? = boundingArc

    /**
     * @param bounds Screen space bounds of the [ComplicationSlot] in pixels.
     * @param boundsType The type of the complication's bounds.
     * @param supportedTypes The [ComplicationType]s supported by this complication.
     * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] for this
     *   complication slot.
     * @param isEnabled Whether or not the complication is currently enabled (i.e. it should be
     *   drawn).
     * @param isInitiallyEnabled Whether or not the complication was initially enabled before
     *   considering any [ComplicationSlotsOption] whose [ComplicationSlotOverlay]s may enable or
     *   disable complicationSlots.
     * @param currentType The [ComplicationType] of the complication's current [ComplicationData].
     * @param fixedComplicationDataSource Whether or not the complication data source is fixed (i.e
     *   the user can't configure it).
     * @param complicationConfigExtras Extras to be merged into the Intent sent when invoking the
     *   complication data source chooser activity.
     * @param nameResourceId The ID of a string resource (or `null` if absent) to visually identify
     *   the complication slot in an editor.
     * @param screenReaderNameResourceId The ID of a string resource (or `null` if absent) to
     *   identify the complication slot in a screen reader.
     */
    public constructor(
        bounds: Rect,
        @ComplicationSlotBoundsTypeIntDef boundsType: Int,
        supportedTypes: List<ComplicationType>,
        defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        isEnabled: Boolean,
        isInitiallyEnabled: Boolean,
        currentType: ComplicationType,
        fixedComplicationDataSource: Boolean,
        complicationConfigExtras: Bundle,
        @Suppress("AutoBoxing") nameResourceId: Int?,
        @Suppress("AutoBoxing") screenReaderNameResourceId: Int?
    ) {
        this.bounds = bounds
        this.boundsType = boundsType
        this.supportedTypes = supportedTypes
        this.defaultDataSourcePolicy = defaultDataSourcePolicy
        this.isEnabled = isEnabled
        this.isInitiallyEnabled = isInitiallyEnabled
        this.currentType = currentType
        this.fixedComplicationDataSource = fixedComplicationDataSource
        this.complicationConfigExtras = complicationConfigExtras
        this.nameResourceId = nameResourceId
        this.screenReaderNameResourceId = screenReaderNameResourceId

        @OptIn(ComplicationExperimental::class)
        this.boundingArc = null
    }

    /**
     * @param bounds Screen space bounds of the [ComplicationSlot] in pixels.
     * @param boundsType The type of the complication's bounds.
     * @param supportedTypes The [ComplicationType]s supported by this complication.
     * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] for this
     *   complication slot.
     * @param isEnabled Whether or not the complication is currently enabled (i.e. it should be
     *   drawn).
     * @param isInitiallyEnabled Whether or not the complication was initially enabled before
     *   considering any [ComplicationSlotsOption] whose [ComplicationSlotOverlay]s may enable or
     *   disable complicationSlots.
     * @param currentType The [ComplicationType] of the complication's current [ComplicationData].
     * @param fixedComplicationDataSource Whether or not the complication data source is fixed (i.e
     *   the user can't configure it).
     * @param complicationConfigExtras Extras to be merged into the Intent sent when invoking the
     *   complication data source chooser activity.
     * @param nameResourceId The ID of a string resource (or `null` if absent) to visually identify
     *   the complication slot in an editor.
     * @param screenReaderNameResourceId The ID of a string resource (or `null` if absent) to
     *   identify the complication slot in a screen reader.
     * @param edgeComplicationBoundingArc The [BoundingArc] describing the geometry of an edge
     *   complication if specified, or `null` otherwise.
     */
    @ComplicationExperimental
    public constructor(
        bounds: Rect,
        @ComplicationSlotBoundsTypeIntDef boundsType: Int,
        supportedTypes: List<ComplicationType>,
        defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        isEnabled: Boolean,
        isInitiallyEnabled: Boolean,
        currentType: ComplicationType,
        fixedComplicationDataSource: Boolean,
        complicationConfigExtras: Bundle,
        @Suppress("AutoBoxing") nameResourceId: Int?,
        @Suppress("AutoBoxing") screenReaderNameResourceId: Int?,
        edgeComplicationBoundingArc: BoundingArc?
    ) {
        this.bounds = bounds
        this.boundsType = boundsType
        this.supportedTypes = supportedTypes
        this.defaultDataSourcePolicy = defaultDataSourcePolicy
        this.isEnabled = isEnabled
        this.isInitiallyEnabled = isInitiallyEnabled
        this.currentType = currentType
        this.fixedComplicationDataSource = fixedComplicationDataSource
        this.complicationConfigExtras = complicationConfigExtras
        this.nameResourceId = nameResourceId
        this.screenReaderNameResourceId = screenReaderNameResourceId
        this.boundingArc = edgeComplicationBoundingArc
    }

    /**
     * @param bounds Screen space bounds of the [ComplicationSlot] in pixels.
     * @param boundsType The type of the complication's bounds.
     * @param supportedTypes The [ComplicationType]s supported by this complication.
     * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] for this
     *   complication slot.
     * @param defaultDataSourceType The default [ComplicationType] for this complication.
     * @param isEnabled Whether or not the complication is currently enabled (i.e. it should be
     *   drawn).
     * @param isInitiallyEnabled Whether or not the complication was initially enabled before
     *   considering any [ComplicationSlotsOption] whose [ComplicationSlotOverlay]s may enable or
     *   disable complicationSlots.
     * @param currentType The [ComplicationType] of the complication's current [ComplicationData].
     * @param fixedComplicationDataSource Whether or not the complication data source is fixed (i.e
     *   the user can't configure it).
     * @param complicationConfigExtras Extras to be merged into the Intent sent when invoking the
     *   complication data source chooser activity.
     */
    @Deprecated(
        "defaultDataSourceType is depreciated",
        ReplaceWith(
            "ComplicationSlotState(Rect, Int, List<ComplicationType>, " +
                "DefaultComplicationDataSourcePolicy, Boolean, Boolean, ComplicationType, Boolean" +
                ", Bundle)"
        )
    )
    public constructor(
        bounds: Rect,
        @ComplicationSlotBoundsTypeIntDef boundsType: Int,
        supportedTypes: List<ComplicationType>,
        defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        defaultDataSourceType: ComplicationType,
        isEnabled: Boolean,
        isInitiallyEnabled: Boolean,
        currentType: ComplicationType,
        fixedComplicationDataSource: Boolean,
        complicationConfigExtras: Bundle
    ) {
        this.bounds = bounds
        this.boundsType = boundsType
        this.supportedTypes = supportedTypes
        this.defaultDataSourcePolicy =
            when {
                defaultDataSourcePolicy.secondaryDataSource != null ->
                    DefaultComplicationDataSourcePolicy(
                        defaultDataSourcePolicy.primaryDataSource!!,
                        defaultDataSourcePolicy.primaryDataSourceDefaultType
                            ?: defaultDataSourceType,
                        defaultDataSourcePolicy.secondaryDataSource!!,
                        defaultDataSourcePolicy.secondaryDataSourceDefaultType
                            ?: defaultDataSourceType,
                        defaultDataSourcePolicy.systemDataSourceFallback,
                        defaultDataSourceType
                    )
                defaultDataSourcePolicy.primaryDataSource != null ->
                    DefaultComplicationDataSourcePolicy(
                        defaultDataSourcePolicy.primaryDataSource!!,
                        defaultDataSourcePolicy.primaryDataSourceDefaultType
                            ?: defaultDataSourceType,
                        defaultDataSourcePolicy.systemDataSourceFallback,
                        defaultDataSourceType
                    )
                else ->
                    DefaultComplicationDataSourcePolicy(
                        defaultDataSourcePolicy.systemDataSourceFallback,
                        defaultDataSourceType
                    )
            }
        this.isEnabled = isEnabled
        this.isInitiallyEnabled = isInitiallyEnabled
        this.currentType = currentType
        this.fixedComplicationDataSource = fixedComplicationDataSource
        this.complicationConfigExtras = complicationConfigExtras
        this.nameResourceId = null
        this.screenReaderNameResourceId = null
        @OptIn(ComplicationExperimental::class)
        this.boundingArc = null
    }

    @OptIn(ComplicationExperimental::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        complicationStateWireFormat: ComplicationStateWireFormat
    ) : this(
        complicationStateWireFormat.bounds,
        complicationStateWireFormat.boundsType,
        complicationStateWireFormat.supportedTypes.map { ComplicationType.fromWireType(it) },
        DefaultComplicationDataSourcePolicy(
            complicationStateWireFormat.defaultDataSourcesToTry ?: emptyList(),
            complicationStateWireFormat.fallbackSystemProvider,
            ComplicationType.fromWireType(complicationStateWireFormat.primaryDataSourceDefaultType),
            ComplicationType.fromWireType(
                complicationStateWireFormat.secondaryDataSourceDefaultType
            ),
            ComplicationType.fromWireType(complicationStateWireFormat.defaultDataSourceType)
        ),
        complicationStateWireFormat.isEnabled,
        complicationStateWireFormat.isInitiallyEnabled,
        ComplicationType.fromWireType(complicationStateWireFormat.currentType),
        complicationStateWireFormat.isFixedComplicationProvider,
        complicationStateWireFormat.complicationConfigExtras,
        complicationStateWireFormat.nameResourceId,
        complicationStateWireFormat.screenReaderNameResourceId,
        @OptIn(ComplicationExperimental::class)
        complicationStateWireFormat.boundingArc?.let {
            BoundingArc(it.arcStartAngle, it.totalArcAngle, it.arcThickness)
        }
    )

    @Suppress("Deprecation")
    override fun toString(): String {
        @OptIn(ComplicationExperimental::class)
        return "ComplicationSlotState(bounds=$bounds, " +
            "boundsType=$boundsType, supportedTypes=$supportedTypes, " +
            "defaultDataSourcePolicy=$defaultDataSourcePolicy, " +
            "defaultDataSourceType=$defaultDataSourceType, isEnabled=$isEnabled, " +
            "isInitiallyEnabled=$isInitiallyEnabled, currentType=$currentType, " +
            "fixedComplicationDataSource=$fixedComplicationDataSource, " +
            "complicationConfigExtras=$complicationConfigExtras, " +
            "nameResourceId=$nameResourceId, " +
            "screenReaderNameResourceId=$screenReaderNameResourceId), " +
            "edgeComplicationBoundingArc=$boundingArc)\n"
    }

    @Suppress("Deprecation")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComplicationSlotState

        if (bounds != other.bounds) return false
        if (boundsType != other.boundsType) return false
        if (supportedTypes != other.supportedTypes) return false
        if (defaultDataSourcePolicy != other.defaultDataSourcePolicy) return false
        if (defaultDataSourceType != other.defaultDataSourceType) return false
        if (isEnabled != other.isEnabled) return false
        if (isInitiallyEnabled != other.isInitiallyEnabled) return false
        if (currentType != other.currentType) return false
        if (fixedComplicationDataSource != other.fixedComplicationDataSource) return false
        if (complicationConfigExtras != other.complicationConfigExtras) return false
        if (nameResourceId != other.nameResourceId) return false
        if (screenReaderNameResourceId != other.screenReaderNameResourceId) return false
        @OptIn(ComplicationExperimental::class) if (boundingArc != other.boundingArc) return false

        return true
    }

    @Suppress("Deprecation")
    override fun hashCode(): Int {
        var result = bounds.hashCode()
        result = 31 * result + boundsType
        result = 31 * result + supportedTypes.hashCode()
        result = 31 * result + defaultDataSourcePolicy.hashCode()
        result = 31 * result + defaultDataSourceType.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isInitiallyEnabled.hashCode()
        result = 31 * result + currentType.hashCode()
        result = 31 * result + fixedComplicationDataSource.hashCode()
        result = 31 * result + complicationConfigExtras.hashCode()
        result = 31 * result + (nameResourceId ?: 0)
        result = 31 * result + (screenReaderNameResourceId ?: 0)
        @OptIn(ComplicationExperimental::class)
        result = 31 * result + boundingArc.hashCode()
        return result
    }
}
