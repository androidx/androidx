/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.protolayout.material3

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.material3.tokens.ShapeTokens

/** Class holding corner shapes as defined by the Wear Material3 shape specification. */
public object Shape {
    /** Returns the [Corner] from the shape tokens for the given token name. */
    internal fun fromToken(@ShapeToken shapeToken: Int): Corner {
        return when (shapeToken) {
            CORNER_EXTRA_LARGE -> ShapeTokens.CORNER_EXTRA_LARGE
            CORNER_EXTRA_SMALL -> ShapeTokens.CORNER_EXTRA_SMALL
            CORNER_FULL -> ShapeTokens.CORNER_FULL
            CORNER_LARGE -> ShapeTokens.CORNER_LARGE
            CORNER_MEDIUM -> ShapeTokens.CORNER_MEDIUM
            CORNER_NONE -> ShapeTokens.CORNER_NONE
            CORNER_SMALL -> ShapeTokens.CORNER_SMALL
            else -> throw IllegalArgumentException("Shape $shapeToken does not exit.")
        }
    }

    /** An extra large rounded corner shape. */
    public const val CORNER_EXTRA_LARGE: Int = 0

    /** An extra small rounded corner shape. */
    public const val CORNER_EXTRA_SMALL: Int = 1

    /** A fully rounded corner shape. */
    public const val CORNER_FULL: Int = 2

    /** A large rounded corner shape. */
    public const val CORNER_LARGE: Int = 3

    /** A medium rounded corner shape. */
    public const val CORNER_MEDIUM: Int = 4

    /** A non-rounded corner shape */
    public const val CORNER_NONE: Int = 5

    /** A small rounded corner shape. */
    public const val CORNER_SMALL: Int = 6

    internal const val TOKEN_COUNT = 7

    /** The referencing token names for a range of corner shapes in Material3. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        CORNER_EXTRA_LARGE,
        CORNER_EXTRA_SMALL,
        CORNER_FULL,
        CORNER_LARGE,
        CORNER_MEDIUM,
        CORNER_NONE,
        CORNER_SMALL
    )
    public annotation class ShapeToken
}
