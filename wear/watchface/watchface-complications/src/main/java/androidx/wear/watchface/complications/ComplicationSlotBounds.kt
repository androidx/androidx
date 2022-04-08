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

import android.content.res.XmlResourceParser
import android.graphics.RectF
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.data.ComplicationType
import java.io.DataOutputStream
import org.xmlpull.v1.XmlPullParser

const val NAMESPACE_APP = "http://schemas.android.com/apk/res-auto"
const val NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"

/**
 * ComplicationSlotBounds are defined by fractional screen space coordinates in unit-square [0..1].
 * These bounds will be subsequently clamped to the unit square and converted to screen space
 * coordinates. NB 0 and 1 are included in the unit square.
 *
 * One bound is expected per [ComplicationType] to allow [androidx.wear.watchface.ComplicationSlot]s
 * to change shape depending on the type.
 */
public class ComplicationSlotBounds(
    /** Per [ComplicationType] fractional unit-square screen space complication bounds. */
    public val perComplicationTypeBounds: Map<ComplicationType, RectF>
) {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun write(dos: DataOutputStream) {
        perComplicationTypeBounds.keys.toSortedSet().forEach { type ->
            val bounds = perComplicationTypeBounds[type]!!
            dos.writeInt(type.toWireComplicationType())
            dos.writeFloat(bounds.left)
            dos.writeFloat(bounds.right)
            dos.writeFloat(bounds.top)
            dos.writeFloat(bounds.bottom)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComplicationSlotBounds

        return perComplicationTypeBounds == other.perComplicationTypeBounds
    }

    override fun hashCode(): Int {
        return perComplicationTypeBounds.toSortedMap().hashCode()
    }

    /**
     * Constructs a ComplicationSlotBounds where all complication types have the same screen space
     * unit-square bounds.
     */
    public constructor(bounds: RectF) : this(ComplicationType.values().associateWith { bounds })

    init {
        require(perComplicationTypeBounds.size == ComplicationType.values().size) {
            "ComplicationSlotBounds must contain entries for each ComplicationType"
        }
        for (type in ComplicationType.values()) {
            require(perComplicationTypeBounds.containsKey(type)) {
                "Missing bounds for $type"
            }
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * The [parser] should be inside a node with any number of ComplicationSlotBounds child
         * nodes. No other child nodes are expected.
         */
        fun inflate(parser: XmlResourceParser): ComplicationSlotBounds? {
            var type = 0
            val outerDepth = parser.depth
            val perComplicationTypeBounds by lazy { HashMap<ComplicationType, RectF>() }
            do {
                if (type == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "ComplicationSlotBounds" -> {
                            val rect = RectF(
                                parser.requireAndGet("left") {
                                    "ComplicationSlotBounds must define 'left'"
                                },
                                parser.requireAndGet("top") {
                                    "ComplicationSlotBounds must define 'top'"
                                },
                                parser.requireAndGet("right") {
                                    "ComplicationSlotBounds must define 'right'"
                                },
                                parser.requireAndGet("bottom") {
                                    "ComplicationSlotBounds must define 'bottom'"
                                }
                            )
                            if (null != parser.getAttributeValue(
                                    NAMESPACE_APP,
                                    "complicationType"
                                )
                            ) {
                                val complicationType = ComplicationType.fromWireType(
                                    parser.getAttributeIntValue(
                                        NAMESPACE_APP,
                                        "complicationType",
                                        0
                                    )
                                )
                                require(
                                    !perComplicationTypeBounds.contains(complicationType)
                                ) {
                                    "Duplicate $complicationType"
                                }
                                perComplicationTypeBounds[complicationType] = rect
                            } else {
                                for (complicationType in ComplicationType.values()) {
                                    require(
                                        !perComplicationTypeBounds.contains(
                                            complicationType
                                        )
                                    ) {
                                        "Duplicate $complicationType"
                                    }
                                    perComplicationTypeBounds[complicationType] = rect
                                }
                            }
                        }
                        else -> throw IllegalArgumentException(
                            "Unexpected node ${parser.name} at line ${parser.lineNumber}"
                        )
                    }
                }
                type = parser.next()
            } while (type != XmlPullParser.END_DOCUMENT && parser.depth > outerDepth)

            return if (perComplicationTypeBounds.isEmpty()) {
                null
            } else {
                ComplicationSlotBounds(perComplicationTypeBounds)
            }
        }
    }
}

internal fun XmlResourceParser.requireAndGet(id: String, produceError: () -> String): Float {
    require(null != getAttributeValue(NAMESPACE_APP, id), produceError)
    return getAttributeFloatValue(NAMESPACE_APP, id, 0f)
}

fun XmlResourceParser.hasValue(id: String): Boolean {
    return null != getAttributeValue(NAMESPACE_APP, id)
}
