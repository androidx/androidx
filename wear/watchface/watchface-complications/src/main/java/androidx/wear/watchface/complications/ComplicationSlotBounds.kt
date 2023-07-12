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

/** Removes the KT class from the public API */
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.watchface.complications

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.RectF
import android.util.TypedValue
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.data.ComplicationType
import java.io.DataOutputStream

const val NAMESPACE_APP = "http://schemas.android.com/apk/res-auto"
const val NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"

/**
 * ComplicationSlotBounds are defined by fractional screen space coordinates in unit-square [0..1].
 * These bounds will be subsequently clamped to the unit square and converted to screen space
 * coordinates. NB 0 and 1 are included in the unit square.
 *
 * One bound is expected per [ComplicationType] to allow [androidx.wear.watchface.ComplicationSlot]s
 * to change shape depending on the type.
 *
 * Taps on the watch are tested first against each ComplicationSlot's [perComplicationTypeBounds]
 * for the relevant [ComplicationType]. Its assumed that [perComplicationTypeBounds] don't overlap.
 * If no intersection was found then taps are checked against [perComplicationTypeBounds] expanded
 * by [perComplicationTypeMargins]. Expanded bounds can overlap so the ComplicationSlot with the
 * lowest id that intersects the coordinates, if any, is selected.
 *
 * @param perComplicationTypeBounds Per [ComplicationType] fractional unit-square screen space
 *   complication bounds.
 * @param perComplicationTypeMargins Per [ComplicationType] fractional unit-square screen space
 *   complication margins for tap detection (doesn't affect rendering).
 */
public class ComplicationSlotBounds(
    public val perComplicationTypeBounds: Map<ComplicationType, RectF>,
    public val perComplicationTypeMargins: Map<ComplicationType, RectF>
) {
    @Deprecated(
        "Use a constructor that specifies perComplicationTypeMargins",
        ReplaceWith(
            "ComplicationSlotBounds(Map<ComplicationType, RectF>, Map<ComplicationType, RectF>)"
        )
    )
    constructor(
        perComplicationTypeBounds: Map<ComplicationType, RectF>
    ) : this(perComplicationTypeBounds, perComplicationTypeBounds.mapValues { RectF() })

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun write(dos: DataOutputStream) {
        perComplicationTypeBounds.keys.toSortedSet().forEach { type ->
            dos.writeInt(type.toWireComplicationType())
            perComplicationTypeBounds[type]!!.write(dos)
            perComplicationTypeMargins[type]!!.write(dos)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComplicationSlotBounds

        if (perComplicationTypeBounds != other.perComplicationTypeBounds) return false
        return perComplicationTypeMargins == other.perComplicationTypeMargins
    }

    override fun hashCode(): Int {
        var result = perComplicationTypeBounds.toSortedMap().hashCode()
        result = 31 * result + perComplicationTypeMargins.toSortedMap().hashCode()
        return result
    }

    override fun toString(): String {
        return "ComplicationSlotBounds(perComplicationTypeBounds=$perComplicationTypeBounds, " +
            "perComplicationTypeMargins=$perComplicationTypeMargins)"
    }

    /**
     * Constructs a ComplicationSlotBounds where all complication types have the same screen space
     * unit-square [bounds] and [margins].
     */
    @JvmOverloads
    public constructor(
        bounds: RectF,
        margins: RectF = RectF()
    ) : this(
        ComplicationType.values().associateWith { bounds },
        ComplicationType.values().associateWith { margins }
    )

    init {
        require(perComplicationTypeBounds.size == ComplicationType.values().size) {
            "perComplicationTypeBounds must contain entries for each ComplicationType"
        }
        require(perComplicationTypeMargins.size == ComplicationType.values().size) {
            "perComplicationTypeMargins must contain entries for each ComplicationType"
        }
        for (type in ComplicationType.values()) {
            require(perComplicationTypeBounds.containsKey(type)) { "Missing bounds for $type" }
            require(perComplicationTypeMargins.containsKey(type)) { "Missing margins for $type" }
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        internal const val NODE_NAME = "ComplicationSlotBounds"

        /**
         * Constructs a [ComplicationSlotBounds] from a potentially incomplete Map<ComplicationType,
         * RectF>, backfilling with empty [RectF]s. This method is necessary because there can be a
         * skew between the version of the library between the watch face and the system which would
         * otherwise be problematic if new complication types have been introduced.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createFromPartialMap(
            partialPerComplicationTypeBounds: Map<ComplicationType, RectF>,
            partialPerComplicationTypeMargins: Map<ComplicationType, RectF>
        ): ComplicationSlotBounds {
            val boundsMap = HashMap(partialPerComplicationTypeBounds)
            val marginsMap = HashMap(partialPerComplicationTypeMargins)

            for (type in ComplicationType.values()) {
                boundsMap.putIfAbsent(type, RectF())
                marginsMap.putIfAbsent(type, RectF())
            }

            return ComplicationSlotBounds(boundsMap, marginsMap)
        }

        /**
         * The [parser] should be inside a node with any number of ComplicationSlotBounds child
         * nodes. No other child nodes are expected.
         */
        fun inflate(
            resources: Resources,
            parser: XmlResourceParser,
            complicationScaleX: Float,
            complicationScaleY: Float
        ): ComplicationSlotBounds? {
            val perComplicationTypeBounds by lazy { HashMap<ComplicationType, RectF>() }
            val perComplicationTypeMargins by lazy { HashMap<ComplicationType, RectF>() }
            parser.iterate {
                when (parser.name) {
                    NODE_NAME -> {
                        val rect =
                            if (parser.hasValue("left"))
                                RectF(
                                    parser.requireAndGet("left", resources, complicationScaleX),
                                    parser.requireAndGet("top", resources, complicationScaleY),
                                    parser.requireAndGet("right", resources, complicationScaleX),
                                    parser.requireAndGet("bottom", resources, complicationScaleY)
                                )
                            else if (parser.hasValue("center_x")) {
                                val halfWidth =
                                    parser.requireAndGet("size_x", resources, complicationScaleX) /
                                        2.0f
                                val halfHeight =
                                    parser.requireAndGet("size_y", resources, complicationScaleY) /
                                        2.0f
                                val centerX =
                                    parser.requireAndGet("center_x", resources, complicationScaleX)
                                val centerY =
                                    parser.requireAndGet("center_y", resources, complicationScaleY)
                                RectF(
                                    centerX - halfWidth,
                                    centerY - halfHeight,
                                    centerX + halfWidth,
                                    centerY + halfHeight
                                )
                            } else {
                                throw IllegalArgumentException(
                                    "$NODE_NAME must " +
                                        "either define top, bottom, left, right" +
                                        "or center_x, center_y, size_x, size_y should be specified"
                                )
                            }
                        val margin =
                            RectF(
                                parser.get("marginLeft", resources, complicationScaleX) ?: 0f,
                                parser.get("marginTop", resources, complicationScaleY) ?: 0f,
                                parser.get("marginRight", resources, complicationScaleX) ?: 0f,
                                parser.get("marginBottom", resources, complicationScaleY) ?: 0f
                            )
                        if (null != parser.getAttributeValue(NAMESPACE_APP, "complicationType")) {
                            val complicationType =
                                ComplicationType.fromWireType(
                                    parser.getAttributeIntValue(
                                        NAMESPACE_APP,
                                        "complicationType",
                                        0
                                    )
                                )
                            require(!perComplicationTypeBounds.contains(complicationType)) {
                                "Duplicate $complicationType"
                            }
                            perComplicationTypeBounds[complicationType] = rect
                            perComplicationTypeMargins[complicationType] = margin
                        } else {
                            for (complicationType in ComplicationType.values()) {
                                require(!perComplicationTypeBounds.contains(complicationType)) {
                                    "Duplicate $complicationType"
                                }
                                perComplicationTypeBounds[complicationType] = rect
                                perComplicationTypeMargins[complicationType] = margin
                            }
                        }
                    }
                    else -> throw IllegalNodeException(parser)
                }
            }

            return if (perComplicationTypeBounds.isEmpty()) {
                null
            } else {
                createFromPartialMap(perComplicationTypeBounds, perComplicationTypeMargins)
            }
        }
    }
}

internal fun XmlResourceParser.requireAndGet(
    id: String,
    resources: Resources,
    scale: Float
): Float {
    val value = get(id, resources, scale)
    require(value != null) { "${ComplicationSlotBounds.NODE_NAME} must define '$id'" }
    return value
}

internal fun XmlResourceParser.get(id: String, resources: Resources, scale: Float): Float? {
    val stringValue = getAttributeValue(NAMESPACE_APP, id) ?: return null
    val resId = getAttributeResourceValue(NAMESPACE_APP, id, 0)
    if (resId != 0) {
        return resources.getDimension(resId) / resources.displayMetrics.widthPixels
    }

    // There is "dp" -> "dip" conversion while resources compilation.
    val dpStr = "dip"

    if (stringValue.endsWith(dpStr)) {
        val dps = stringValue.substring(0, stringValue.length - dpStr.length).toFloat()
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dps,
            resources.displayMetrics
        ) / resources.displayMetrics.widthPixels
    } else {
        require(scale > 0) { "scale should be positive" }
        return stringValue.toFloat() / scale
    }
}

fun XmlResourceParser.hasValue(id: String): Boolean {
    return null != getAttributeValue(NAMESPACE_APP, id)
}

internal fun RectF.write(dos: DataOutputStream) {
    dos.writeFloat(left)
    dos.writeFloat(right)
    dos.writeFloat(top)
    dos.writeFloat(bottom)
}
