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

import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.NAMESPACE_APP
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.getIntRefAttribute
import androidx.wear.watchface.complications.getStringRefAttribute
import androidx.wear.watchface.complications.hasValue
import androidx.wear.watchface.complications.moveToStart
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleFlavors
import androidx.wear.watchface.style.UserStyleSchema
import kotlin.jvm.Throws
import org.xmlpull.v1.XmlPullParser

@OptIn(ComplicationExperimental::class)
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
            parser.moveToStart("XmlWatchFace")

            val complicationScaleX =
                parser.getAttributeFloatValue(NAMESPACE_APP, "complicationScaleX", 1.0f)
            val complicationScaleY =
                parser.getAttributeFloatValue(NAMESPACE_APP, "complicationScaleY", 1.0f)

            require(complicationScaleX > 0) { "complicationScaleX should be positive" }
            require(complicationScaleY > 0) { "complicationScaleY should be positive" }

            var schema: UserStyleSchema? = null
            var flavors: UserStyleFlavors? = null
            val outerDepth = parser.depth

            var type = parser.next()

            // Parse the XmlWatchFace declaration.
            val complicationSlots = ArrayList<ComplicationSlotStaticData>()
            do {
                if (type == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "UserStyleSchema" -> {
                            schema =
                                UserStyleSchema.inflate(
                                    resources,
                                    parser,
                                    complicationScaleX,
                                    complicationScaleY
                                )
                        }
                        "ComplicationSlot" -> {
                            complicationSlots.add(
                                ComplicationSlotStaticData.inflate(
                                    resources,
                                    parser,
                                    complicationScaleX,
                                    complicationScaleY
                                )
                            )
                        }
                        "UserStyleFlavors" -> {
                            require(schema != null) {
                                "A UserStyleFlavors node requires a previous UserStyleSchema node"
                            }
                            flavors = UserStyleFlavors.inflate(resources, parser, schema)
                        }
                        else ->
                            throw IllegalArgumentException(
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
        @ComplicationSlotBoundsTypeIntDef val boundsType: Int,
        val bounds: ComplicationSlotBounds,
        val supportedTypes: List<ComplicationType>,
        val defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        val initiallyEnabled: Boolean,
        val fixedComplicationDataSource: Boolean,
        val nameResourceId: Int?,
        val screenReaderNameResourceId: Int?,
        val boundingArc: BoundingArc?
    ) {
        companion object {
            @Suppress("NewApi")
            private val typesMap by
                lazy(LazyThreadSafetyMode.NONE) {
                    mapOf(
                        "SHORT_TEXT" to ComplicationType.SHORT_TEXT,
                        "LONG_TEXT" to ComplicationType.LONG_TEXT,
                        "RANGED_VALUE" to ComplicationType.RANGED_VALUE,
                        "MONOCHROMATIC_IMAGE" to ComplicationType.MONOCHROMATIC_IMAGE,
                        "SMALL_IMAGE" to ComplicationType.SMALL_IMAGE,
                        "PHOTO_IMAGE" to ComplicationType.PHOTO_IMAGE,
                        "GOAL_PROGRESS" to ComplicationType.GOAL_PROGRESS,
                        "WEIGHTED_ELEMENTS" to ComplicationType.WEIGHTED_ELEMENTS
                    )
                }

            fun inflate(
                resources: Resources,
                parser: XmlResourceParser,
                complicationScaleX: Float,
                complicationScaleY: Float
            ): ComplicationSlotStaticData {
                require(parser.name == "ComplicationSlot") { "Expected a UserStyleSchema node" }
                val slotId = getIntRefAttribute(resources, parser, "slotId")
                require(slotId != null) { "A ComplicationSlot must have a slotId attribute" }
                val accessibilityTraversalIndex =
                    if (parser.hasValue("accessibilityTraversalIndex")) {
                        parser.getAttributeIntValue(NAMESPACE_APP, "accessibilityTraversalIndex", 0)
                    } else {
                        null
                    }
                require(parser.hasValue("boundsType")) {
                    "A ComplicationSlot must have a boundsType attribute"
                }
                val boundsType =
                    when (parser.getAttributeIntValue(NAMESPACE_APP, "boundsType", 0)) {
                        0 -> ComplicationSlotBoundsType.ROUND_RECT
                        1 -> ComplicationSlotBoundsType.BACKGROUND
                        2 -> ComplicationSlotBoundsType.EDGE
                        else -> throw IllegalArgumentException("Unknown boundsType")
                    }

                require(parser.hasValue("supportedTypes")) {
                    "A ComplicationSlot must have a supportedTypes attribute"
                }
                val supportedTypes =
                    getStringRefAttribute(resources, parser, "supportedTypes")?.split('|')
                        ?: throw IllegalArgumentException(
                            "Unable to extract the supported type(s) for ComplicationSlot $slotId"
                        )
                val supportedTypesList =
                    supportedTypes.map {
                        typesMap[it]
                            ?: throw IllegalArgumentException(
                                "Unrecognised type $it for ComplicationSlot $slotId"
                            )
                    }

                val defaultComplicationDataSourcePolicy =
                    DefaultComplicationDataSourcePolicy.inflate(
                        resources,
                        parser,
                        "ComplicationSlot"
                    )

                val initiallyEnabled =
                    parser.getAttributeBooleanValue(NAMESPACE_APP, "initiallyEnabled", true)
                val fixedComplicationDataSource =
                    parser.getAttributeBooleanValue(
                        NAMESPACE_APP,
                        "fixedComplicationDataSource",
                        false
                    )
                val nameResourceId =
                    if (parser.hasValue("name")) {
                        parser.getAttributeResourceValue(NAMESPACE_APP, "name", 0)
                    } else {
                        null
                    }
                val screenReaderNameResourceId =
                    if (parser.hasValue("screenReaderName")) {
                        parser.getAttributeResourceValue(NAMESPACE_APP, "screenReaderName", 0)
                    } else {
                        null
                    }
                val boundingArc =
                    if (parser.hasValue("startArcAngle")) {
                        BoundingArc(
                            parser.getAttributeFloatValue(NAMESPACE_APP, "startArcAngle", 0f),
                            parser.getAttributeFloatValue(NAMESPACE_APP, "totalArcAngle", 0f),
                            parser.getAttributeFloatValue(NAMESPACE_APP, "arcThickness", 0f)
                        )
                    } else {
                        null
                    }
                val bounds =
                    ComplicationSlotBounds.inflate(
                        resources,
                        parser,
                        complicationScaleX,
                        complicationScaleY
                    )
                require(bounds != null) {
                    "ComplicationSlot must have either one ComplicationSlotBounds child node or " +
                        "one per ComplicationType."
                }
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
                    screenReaderNameResourceId,
                    boundingArc
                )
            }
        }
    }

    fun buildComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository,
        complicationSlotInflationFactory: ComplicationSlotInflationFactory
    ): ComplicationSlotsManager {
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
                        else ->
                            throw UnsupportedOperationException(
                                "Unknown boundsType ${it.boundsType}"
                            )
                    },
                    it.nameResourceId,
                    it.screenReaderNameResourceId,
                    it.boundingArc
                )
            },
            currentUserStyleRepository
        )
    }
}
