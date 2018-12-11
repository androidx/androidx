/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting.borders

/**
 * The shape to use when rendering a [Border] or [BoxDecoration].
 *
 * Consider using [ShapeBorder] subclasses directly (with [ShapeDecoration]),
 * instead of using [BoxShape] and [Border], if the shapes will need to be
 * interpolated or animated. The [Border] class cannot interpolate between
 * different shapes.
 */
enum class BoxShape {
    /**
     * An axis-aligned, 2D RECTANGLE. May have rounded corners (described by a
     * [BorderRadius]). The edges of the RECTANGLE will match the edges of the box
     * into which the [Border] or [BoxDecoration] is painted.
     *
     * See also:
     *
     *  * [RoundedRectangleBorder], the equivalent [ShapeBorder].
     */
    RECTANGLE,

    /**
     * A CIRCLE centered in the middle of the box into which the [Border] or
     * [BoxDecoration] is painted. The diameter of the CIRCLE is the shortest
     * dimension of the box, either the width or the height, such that the CIRCLE
     * touches the edges of the box.
     *
     * See also:
     *
     *  * [CircleBorder], the equivalent [ShapeBorder].
     */
    CIRCLE

    // Don't add more, instead create a new ShapeBorder.
}