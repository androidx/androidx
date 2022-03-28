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
import android.content.res.TypedArray
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
@OptIn(WatchFaceFlavorsExperimental::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class XmlSchemaAndComplicationSlotsDefinition(
    val schema: UserStyleSchema?,
    val complicationSlots: List<ComplicationSlotStaticData>,
    val flavors: UserStyleFlavors?
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
            var flavors: UserStyleFlavors? = null
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
                        "UserStyleFlavors" -> {
                            require(schema != null) {
                                "A UserStyleFlavors node requires a previous UserStyleSchema node"
                            }
                            flavors = UserStyleFlavors.inflate(resources, parser, schema)
                        }
                        else -> throw IllegalArgumentException(
                            "Unexpected node ${parser.name} at line ${parser.lineNumber}"
                        )
                    }
                }
                type = parser.next()
            } while (type != XmlPullParser.END_DOCUMENT && parser.depth > outerDepth)

            parser.close()

            return XmlSchemaAndComplicationSlotsDefinition(schema, complicationSlots, flavors)
        }
    }

    public class ComplicationSlotStaticData(
        val slotId: Int,
        val accessibilityTraversalIndex: Int?,
        @ComplicationSlotBoundsType val boundsType: Int,
        val bounds: ComplicationSlotBounds,
        val supportedTypes: List<ComplicationType>,
        val defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        val initiallyEnabled: Boolean,
        val fixedComplicationDataSource: Boolean,
        val nameResourceId: Int?,
        val screenReaderNameResourceId: Int?
    ) {
        companion object {
            fun inflateDefaultComplicationDataSourcePolicy(
                attributes: TypedArray,
                primaryDataSourceAttr: Int,
                primaryDataSourceDefaultTypeAttr: Int,
                secondaryDataSourceAttr: Int,
                secondaryDataSourceDefaultTypeAttr: Int,
                systemDataSourceFallbackAttr: Int,
                systemDataSourceFallbackDefaultTypeAttr: Int
            ): DefaultComplicationDataSourcePolicy {
                val primaryDataSource =
                    attributes.getString(primaryDataSourceAttr)?.let {
                        ComponentName.unflattenFromString(it)
                    }
                val primaryDataSourceDefaultType =
                    if (attributes.hasValue(primaryDataSourceDefaultTypeAttr)) {
                        ComplicationType.fromWireType(
                            attributes.getInt(
                                R.styleable.ComplicationSlot_primaryDataSourceDefaultType,
                                0
                            )
                        )
                    } else {
                        null
                    }
                val secondaryDataSource =
                    attributes.getString(secondaryDataSourceAttr)?.let {
                        ComponentName.unflattenFromString(it)
                    }

                val secondaryDataSourceDefaultType =
                    if (attributes.hasValue(secondaryDataSourceDefaultTypeAttr)) {
                        ComplicationType.fromWireType(
                            attributes.getInt(
                                R.styleable.ComplicationSlot_secondaryDataSourceDefaultType,
                                0
                            )
                        )
                    } else {
                        null
                    }

                require(attributes.hasValue(systemDataSourceFallbackAttr)) {
                    "A ComplicationSlot must have a systemDataSourceFallback attribute"
                }
                val systemDataSourceFallback = attributes.getInt(systemDataSourceFallbackAttr, 0)
                require(attributes.hasValue(systemDataSourceFallbackDefaultTypeAttr)) {
                    "A ComplicationSlot must have a systemDataSourceFallbackDefaultType" +
                        " attribute"
                }
                val systemDataSourceFallbackDefaultType = ComplicationType.fromWireType(
                    attributes.getInt(systemDataSourceFallbackDefaultTypeAttr, 0))
                return when {
                    secondaryDataSource != null -> {
                        require(primaryDataSource != null) {
                            "If a secondaryDataSource is specified, a primaryDataSource must be too"
                        }
                        require(primaryDataSourceDefaultType != null) {
                            "If a primaryDataSource is specified, a " +
                                "primaryDataSourceDefaultType must be too"
                        }
                        require(secondaryDataSourceDefaultType != null) {
                            "If a secondaryDataSource is specified, a " +
                                "secondaryDataSourceDefaultType must be too"
                        }
                        DefaultComplicationDataSourcePolicy(
                            primaryDataSource,
                            primaryDataSourceDefaultType,
                            secondaryDataSource,
                            secondaryDataSourceDefaultType,
                            systemDataSourceFallback,
                            systemDataSourceFallbackDefaultType
                        )
                    }

                    primaryDataSource != null -> {
                        require(primaryDataSourceDefaultType != null) {
                            "If a primaryDataSource is specified, a " +
                                "primaryDataSourceDefaultType must be too"
                        }
                        DefaultComplicationDataSourcePolicy(
                            primaryDataSource,
                            primaryDataSourceDefaultType,
                            systemDataSourceFallback,
                            systemDataSourceFallbackDefaultType
                        )
                    }

                    else -> {
                        DefaultComplicationDataSourcePolicy(
                            systemDataSourceFallback,
                            systemDataSourceFallbackDefaultType
                        )
                    }
                }
            }

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

                val defaultComplicationDataSourcePolicy =
                    inflateDefaultComplicationDataSourcePolicy(
                        attributes,
                        R.styleable.ComplicationSlot_primaryDataSource,
                        R.styleable.ComplicationSlot_primaryDataSourceDefaultType,
                        R.styleable.ComplicationSlot_secondaryDataSource,
                        R.styleable.ComplicationSlot_secondaryDataSourceDefaultType,
                        R.styleable.ComplicationSlot_systemDataSourceFallback,
                        R.styleable.ComplicationSlot_systemDataSourceFallbackDefaultType
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

                val nameResourceId =
                    if (attributes.hasValue(R.styleable.ComplicationSlot_name)) {
                        attributes.getResourceId(R.styleable.ComplicationSlot_name, 0)
                    } else {
                        null
                    }
                val screenReaderNameResourceId =
                    if (attributes.hasValue(R.styleable.ComplicationSlot_screenReaderName)) {
                        attributes.getResourceId(R.styleable.ComplicationSlot_screenReaderName, 0)
                    } else {
                        null
                    }

                attributes.recycle()

                return ComplicationSlotStaticData(
                    slotId,
                    accessibilityTraversalIndex,
                    boundsType,
                    bounds,
                    supportedTypesList,
                    defaultComplicationDataSourcePolicy,
                    initiallyEnabled,
                    fixedComplicationDataSource,
                    nameResourceId,
                    screenReaderNameResourceId
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
                    it.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType,
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
                    },
                    it.nameResourceId,
                    it.screenReaderNameResourceId
                )
            },
            currentUserStyleRepository
        )
    }
}
