/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import org.xmlpull.v1.XmlPullParser
import kotlin.jvm.Throws

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class XmlSchemaAndComplicationSlotsDefinition(
    val schema: UserStyleSchema?,
    val complicationSlots: List<ComplicationSlotStaticData>
) {
    companion object {
        @Throws(PackageManager.NameNotFoundException::class)
        public fun inflate(
            resources: Resources,
            parser: XmlResourceParser
        ): XmlSchemaAndComplicationSlotsDefinition {
            // Parse next until start tag is found
            var type: Int
            do {
                type = parser.next()
            } while (type != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG)

            require(parser.name == "XmlWatchFace") {
                "Expected a XmlWatchFace node"
            }

            var schema: UserStyleSchema? = null
            val outerDepth = parser.depth

            type = parser.next()

            // Parse the XmlWatchFace declaration.
            val complicationSlots = ArrayList<ComplicationSlotStaticData>()
            do {
                if (type == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "UserStyleSchema" -> {
                            schema = UserStyleSchema.inflate(resources, parser)
                        }
                        "ComplicationSlot" -> {
                            complicationSlots.add(
                                ComplicationSlotStaticData.inflate(resources, parser)
                            )
                        }
                        else -> throw IllegalArgumentException(
                            "Unexpected node ${parser.name} at line ${parser.lineNumber}"
                        )
                    }
                }
                type = parser.next()
            } while (type != XmlPullParser.END_DOCUMENT && parser.depth > outerDepth)

            parser.close()

            return XmlSchemaAndComplicationSlotsDefinition(schema, complicationSlots)
        }
    }

    public class ComplicationSlotStaticData(
        val slotId: Int,
        val accessibilityTraversalIndex: Int?,
        @ComplicationSlotBoundsType val boundsType: Int,
        val bounds: ComplicationSlotBounds,
        val supportedTypes: List<ComplicationType>,
        val defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        val defaultDataSourceType: ComplicationType,
        val initiallyEnabled: Boolean,
        val fixedComplicationDataSource: Boolean
    ) {
        companion object {
            fun inflate(
                resources: Resources,
                parser: XmlResourceParser
            ): ComplicationSlotStaticData {
                require(parser.name == "ComplicationSlot") {
                    "Expected a UserStyleSchema node"
                }
                val attributes = resources.obtainAttributes(
                    parser,
                    R.styleable.ComplicationSlot
                )

                require(attributes.hasValue(R.styleable.ComplicationSlot_slotId)) {
                    "A ComplicationSlot must have an id attribute"
                }
                val slotId = attributes.getString(R.styleable.ComplicationSlot_slotId)!!.toInt()

                val accessibilityTraversalIndex = if (
                    attributes.hasValue(R.styleable.ComplicationSlot_accessibilityTraversalIndex)
                ) {
                    attributes.getInteger(
                        R.styleable.ComplicationSlot_accessibilityTraversalIndex,
                        0
                    )
                } else {
                    null
                }
                require(attributes.hasValue(R.styleable.ComplicationSlot_boundsType)) {
                    "A ComplicationSlot must have a boundsType attribute"
                }
                val boundsType = when (
                    attributes.getInteger(R.styleable.ComplicationSlot_boundsType, 0)
                ) {
                    0 -> ComplicationSlotBoundsType.ROUND_RECT
                    1 -> ComplicationSlotBoundsType.BACKGROUND
                    2 -> ComplicationSlotBoundsType.EDGE
                    else -> throw IllegalArgumentException("Unknown boundsType")
                }

                require(attributes.hasValue(R.styleable.ComplicationSlot_supportedTypes)) {
                    "A ComplicationSlot must have a supportedTypes attribute"
                }
                val supportedTypes =
                    attributes.getInteger(R.styleable.ComplicationSlot_supportedTypes, 0)
                val supportedTypesList = ArrayList<ComplicationType>()
                if ((supportedTypes and 0x1) != 0) {
                    supportedTypesList.add(ComplicationType.SHORT_TEXT)
                }
                if ((supportedTypes and 0x2) != 0) {
                    supportedTypesList.add(ComplicationType.LONG_TEXT)
                }
                if ((supportedTypes and 0x4) != 0) {
                    supportedTypesList.add(ComplicationType.RANGED_VALUE)
                }
                if ((supportedTypes and 0x8) != 0) {
                    supportedTypesList.add(ComplicationType.MONOCHROMATIC_IMAGE)
                }
                if ((supportedTypes and 0x10) != 0) {
                    supportedTypesList.add(ComplicationType.SMALL_IMAGE)
                }
                if ((supportedTypes and 0x20) != 0) {
                    supportedTypesList.add(ComplicationType.PHOTO_IMAGE)
                }

                val primaryDataSource =
                    attributes.getString(R.styleable.ComplicationSlot_primaryDataSource)?.let {
                        ComponentName.unflattenFromString(it)
                    }
                val secondaryDataSource =
                    attributes.getString(R.styleable.ComplicationSlot_secondaryDataSource)?.let {
                        ComponentName.unflattenFromString(it)
                    }
                require(
                    attributes.hasValue(R.styleable.ComplicationSlot_systemDataSourceFallback)
                ) {
                    "A ComplicationSlot must have a systemDataSourceFallback attribute"
                }
                val systemDataSourceFallback = attributes.getInt(
                    R.styleable.ComplicationSlot_systemDataSourceFallback,
                    0
                )
                val defaultComplicationDataSourcePolicy = when {
                    secondaryDataSource != null -> {
                        require(primaryDataSource != null) {
                            "If a secondaryDataSource is specified, a primaryDataSource must be too"
                        }
                        DefaultComplicationDataSourcePolicy(
                            primaryDataSource,
                            secondaryDataSource,
                            systemDataSourceFallback
                        )
                    }

                    primaryDataSource != null -> DefaultComplicationDataSourcePolicy(
                        primaryDataSource,
                        systemDataSourceFallback
                    )

                    else -> DefaultComplicationDataSourcePolicy(systemDataSourceFallback)
                }

                require(
                    attributes.hasValue(R.styleable.ComplicationSlot_defaultDataSourceType)
                ) {
                    "A ComplicationSlot must have a defaultDataSourceType attribute"
                }
                val defaultDataSourceType = ComplicationType.fromWireType(
                    attributes.getInt(
                        R.styleable.ComplicationSlot_defaultDataSourceType,
                        0
                    )
                )
                val initiallyEnabled = attributes.getBoolean(
                    R.styleable.ComplicationSlot_initiallyEnabled,
                    true
                )
                val fixedComplicationDataSource = attributes.getBoolean(
                    R.styleable.ComplicationSlot_fixedComplicationDataSource,
                    false
                )
                val bounds = ComplicationSlotBounds.inflate(resources, parser)
                require(bounds != null) {
                    "ComplicationSlot must have either one ComplicationSlotBounds child node or " +
                        "one per ComplicationType."
                }

                attributes.recycle()

                return ComplicationSlotStaticData(
                    slotId,
                    accessibilityTraversalIndex,
                    boundsType,
                    bounds,
                    supportedTypesList,
                    defaultComplicationDataSourcePolicy,
                    defaultDataSourceType,
                    initiallyEnabled,
                    fixedComplicationDataSource
                )
            }
        }
    }

    fun buildComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository,
        complicationSlotInflationFactory: ComplicationSlotInflationFactory?
    ): ComplicationSlotsManager {
        if (complicationSlots.isEmpty()) {
            return ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
        }

        require(complicationSlotInflationFactory != null) {
            "You must override WatchFaceService.getComplicationSlotDetailsFactory to provide " +
            "additional details needed to inflate ComplicationSlotsManager"
        }

        return ComplicationSlotsManager(
            complicationSlots.map {
                ComplicationSlot(
                    it.slotId,
                    it.accessibilityTraversalIndex ?: it.slotId,
                    it.boundsType,
                    it.bounds,
                    complicationSlotInflationFactory.getCanvasComplicationFactory(it.slotId),
                    it.supportedTypes,
                    it.defaultDataSourcePolicy,
                    it.defaultDataSourceType,
                    it.initiallyEnabled,
                    Bundle(),
                    it.fixedComplicationDataSource,
                    when (it.boundsType) {
                        ComplicationSlotBoundsType.ROUND_RECT -> RoundRectComplicationTapFilter()
                        ComplicationSlotBoundsType.BACKGROUND -> BackgroundComplicationTapFilter()
                        ComplicationSlotBoundsType.EDGE ->
                            complicationSlotInflationFactory.getEdgeComplicationTapFilter(it.slotId)
                        else -> throw UnsupportedOperationException(
                            "Unknown boundsType ${it.boundsType}"
                        )
                    }
                )
            },
            currentUserStyleRepository
        )
    }
}