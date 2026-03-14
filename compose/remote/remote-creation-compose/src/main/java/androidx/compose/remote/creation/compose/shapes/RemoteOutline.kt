/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.shapes

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.LayoutDirection

/**
 * Defines a simple shape, used for bounding graphical regions.
 *
 * Can be used for defining a shape of the component background, a shape of shadows cast by the
 * component, or to clip the contents.
 */
public sealed class RemoteOutline {

    /** Rectangular area. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Rectangle(public val topLeft: RemoteOffset, public val size: RemoteSize) :
        RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            drawRect(paint, topLeft, size)
        }
    }

    /** Rectangular area with rounded corners. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Rounded(
        internal val topStart: RemoteFloat,
        internal val topEnd: RemoteFloat,
        internal val bottomEnd: RemoteFloat,
        internal val bottomStart: RemoteFloat,
    ) : RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            val w = width
            val h = height
            // Remap corner radii based on layout direction
            val topLeft: RemoteFloat
            val topRight: RemoteFloat
            val bottomRight: RemoteFloat
            val bottomLeft: RemoteFloat

            when (remoteCanvas.layoutDirection) {
                LayoutDirection.Ltr -> {
                    topLeft = topStart
                    topRight = topEnd
                    bottomRight = bottomEnd
                    bottomLeft = bottomStart
                }
                LayoutDirection.Rtl -> {
                    topLeft = topEnd
                    topRight = topStart
                    bottomRight = bottomStart
                    bottomLeft = bottomEnd
                }
            }

            val path = RemotePath()
            val circularArcWeight = 0.7071f.rf // Weight for a 90-degree circular arc

            // 1. Move to top edge
            path.moveTo(topLeft.floatId, 0f.rf.floatId)

            // 2. Top Line & Top-Right Corner
            path.lineTo((w - topRight).floatId, 0f.rf.floatId)
            path.conicTo(
                x1 = w.floatId,
                y1 = 0f.rf.floatId,
                x2 = w.floatId,
                y2 = topRight.floatId,
                weight = circularArcWeight.floatId,
            )

            // 3. Right Line & Bottom-Right Corner
            path.lineTo(w.floatId, (h - bottomRight).floatId)
            path.conicTo(
                x1 = w.floatId,
                y1 = h.floatId,
                x2 = (w - bottomRight).floatId,
                y2 = h.floatId,
                weight = circularArcWeight.floatId,
            )

            // 4. Bottom Line & Bottom-Left Corner
            path.lineTo(bottomLeft.floatId, h.floatId)
            path.conicTo(
                x1 = 0f.rf.floatId,
                y1 = h.floatId,
                x2 = 0f.rf.floatId,
                y2 = (h - bottomLeft).floatId,
                weight = circularArcWeight.floatId,
            )

            // 5. Start Line & Top-Left Corner
            path.lineTo(0f.rf.floatId, topLeft.floatId)
            path.conicTo(
                x1 = 0f.rf.floatId,
                y1 = 0f.rf.floatId,
                x2 = topLeft.floatId,
                y2 = 0f.rf.floatId,
                weight = circularArcWeight.floatId,
            )

            // 6. Close the path
            path.close()
            drawPath(path, paint)
        }
    }

    /** An area defined as a path. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Generic(public val path: RemotePath) : RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            drawPath(path, paint)
        }
    }

    /** Draws the outline to the canvas with paint. */
    public abstract fun RemoteDrawScope.drawOutline(paint: RemotePaint)
}
