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

package androidx.wear.watchface.complications.data

import android.os.Build
import android.support.wearable.complications.ComplicationData as WireComplicationData
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * The possible complication data types.
 *
 * See also wear/watchface/watchface-complications-data/src/main/res/values/attrs.xml for the XML
 * definition. And supportedTypes in wear/watchface/watchface/src/main/res/values/attrs.xml. And
 * wear/watchface/watchface/src/main/res/values/attrs.xml which defines a subset.
 */
public enum class ComplicationType(private val wireType: Int) {
    NO_DATA(WireComplicationData.TYPE_NO_DATA),
    EMPTY(WireComplicationData.TYPE_EMPTY),
    NOT_CONFIGURED(WireComplicationData.TYPE_NOT_CONFIGURED),
    SHORT_TEXT(WireComplicationData.TYPE_SHORT_TEXT),
    LONG_TEXT(WireComplicationData.TYPE_LONG_TEXT),
    RANGED_VALUE(WireComplicationData.TYPE_RANGED_VALUE),
    MONOCHROMATIC_IMAGE(WireComplicationData.TYPE_ICON),
    SMALL_IMAGE(WireComplicationData.TYPE_SMALL_IMAGE),
    PHOTO_IMAGE(WireComplicationData.TYPE_LARGE_IMAGE),
    NO_PERMISSION(WireComplicationData.TYPE_NO_PERMISSION),
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    GOAL_PROGRESS(WireComplicationData.TYPE_GOAL_PROGRESS),
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    WEIGHTED_ELEMENTS(WireComplicationData.TYPE_WEIGHTED_ELEMENTS);

    /**
     * Converts this value to the integer value used for serialization.
     *
     * This is only needed internally to convert to the underlying communication protocol.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY) public fun toWireComplicationType(): Int = wireType

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Converts the integer value used for serialization into a [ComplicationType].
         *
         * This is only needed internally to convert to the underlying communication protocol.
         */
        @OptIn(ComplicationExperimental::class)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @Suppress("NewApi")
        public fun fromWireType(wireType: Int): ComplicationType =
            when (wireType) {
                NO_DATA.wireType -> NO_DATA
                EMPTY.wireType -> EMPTY
                NOT_CONFIGURED.wireType -> NOT_CONFIGURED
                SHORT_TEXT.wireType -> SHORT_TEXT
                LONG_TEXT.wireType -> LONG_TEXT
                RANGED_VALUE.wireType -> RANGED_VALUE
                MONOCHROMATIC_IMAGE.wireType -> MONOCHROMATIC_IMAGE
                SMALL_IMAGE.wireType -> SMALL_IMAGE
                PHOTO_IMAGE.wireType -> PHOTO_IMAGE
                NO_PERMISSION.wireType -> NO_PERMISSION
                GOAL_PROGRESS.wireType -> GOAL_PROGRESS
                WEIGHTED_ELEMENTS.wireType -> WEIGHTED_ELEMENTS
                else -> EMPTY
            }

        /**
         * Converts an array of [ComplicationType] to an array of integers with the corresponding
         * wire types.
         *
         * This is only needed internally to convert to the underlying communication protocol.
         *
         * Needed to access this conveniently in Java.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun toWireTypes(types: Collection<ComplicationType>): IntArray = types.toWireTypes()

        /**
         * Converts an array of integer values used for serialization into the corresponding array
         * of [ComplicationType].
         *
         * This is only needed internally to convert to the underlying communication protocol.
         *
         * Needed to access this conveniently in Java.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun fromWireTypes(types: IntArray): Array<ComplicationType> =
            types.toApiComplicationTypes()

        /**
         * Converts an array of integer values used for serialization into the corresponding list of
         * [ComplicationType].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun fromWireTypeList(types: IntArray): List<ComplicationType> =
            types.map { fromWireType(it) }
    }
}

/**
 * Converts an array of [ComplicationType] to an array of integers with the corresponding wire
 * types.
 *
 * This is only needed internally to convert to the underlying communication protocol.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Collection<ComplicationType>.toWireTypes(): IntArray =
    this.map { it.toWireComplicationType() }.toIntArray()

/**
 * Converts an array of integer values uses for serialization into the corresponding array of
 * [ComplicationType] to .
 *
 * This is only needed internally to convert to the underlying communication protocol.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun IntArray.toApiComplicationTypes(): Array<ComplicationType> =
    this.map { ComplicationType.fromWireType(it) }.toTypedArray()
