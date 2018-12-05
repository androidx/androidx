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

package androidx.ui.rendering.proxybox

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.Path
import androidx.ui.painting.borders.ShapeBorder

/** A [CustomClipper] that clips to the outer path of a [ShapeBorder]. */
class ShapeBorderClipper(
    /** The shape border whose outer path this clipper clips to. */
    val shape: ShapeBorder,
    /**
     * The [textDirection] argument must be provided non-null if [shape]
     * has a text direction dependency (for example if it is expressed in terms
     * of "start" and "end" instead of "left" and "right"). It may be null if
     * the border will not need the text direction to paint itself.
     * [ShapeBorder]s can depend on the text direction (e.g having a "dent"
     * towards the start of the shape).
     */
    val textDirection: TextDirection? = null
) : CustomClipper<Path>() {

    /** Returns the outer path of [shape] as the clip. */
    override fun getClip(size: Size): Path {
        return shape.getOuterPath(Offset.zero and size, textDirection = textDirection)
    }

    override fun shouldReclip(oldClipper: CustomClipper<Path>): Boolean {
        if (oldClipper !is ShapeBorderClipper)
            return true
        return oldClipper.shape != shape
    }
}