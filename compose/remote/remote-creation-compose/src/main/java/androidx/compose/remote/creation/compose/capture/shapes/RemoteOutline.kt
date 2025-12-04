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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture.shapes

import android.graphics.Rect
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.conicTo
import androidx.compose.remote.creation.compose.layout.lineTo
import androidx.compose.remote.creation.compose.layout.moveTo
import androidx.compose.remote.creation.compose.layout.remoteComponentHeight
import androidx.compose.remote.creation.compose.layout.remoteComponentWidth
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class RemoteOutline {

    /** Rectangular area. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Rectangle(public val rect: Rect) : RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            canvas.drawRect(rect, paint)
        }
    }

    /** Rectangular area with rounded corners. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Rounded(
        private val topStart: RemoteFloat,
        private val topEnd: RemoteFloat,
        private val bottomEnd: RemoteFloat,
        private val bottomStart: RemoteFloat,
    ) : RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            val w = remoteComponentWidth(canvas.creationState)
            val h = remoteComponentHeight(canvas.creationState)
            // Remap corner radii based on layout direction
            val topLeft: RemoteFloat
            val topRight: RemoteFloat
            val bottomRight: RemoteFloat
            val bottomLeft: RemoteFloat

            when (layoutDirection) {
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
            path.moveTo(topLeft, 0f.rf)

            // 2. Top Line & Top-Right Corner
            path.lineTo(w - topRight, 0f.rf)
            path.conicTo(x1 = w, y1 = 0f.rf, x2 = w, y2 = topRight, weight = circularArcWeight)

            // 3. Right Line & Bottom-Right Corner
            path.lineTo(w, h - bottomRight)
            path.conicTo(x1 = w, y1 = h, x2 = w - bottomRight, y2 = h, weight = circularArcWeight)

            // 4. Bottom Line & Bottom-Left Corner
            path.lineTo(bottomLeft, h)
            path.conicTo(
                x1 = 0f.rf,
                y1 = h,
                x2 = 0f.rf,
                y2 = h - bottomLeft,
                weight = circularArcWeight,
            )

            // 5. Start Line & Top-Left Corner
            path.lineTo(0f.rf, topLeft)
            path.conicTo(
                x1 = 0f.rf,
                y1 = 0f.rf,
                x2 = topLeft,
                y2 = 0f.rf,
                weight = circularArcWeight,
            )

            // 6. Close the path
            path.close()
            canvas.drawRPath(path, paint)
        }
    }

    /** An area defined as a path. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Generic(public val path: RemotePath) : RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            canvas.drawRPath(path, paint)
        }
    }

    /** Draws the outline to the canvas with paint. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun RemoteDrawScope.drawOutline(paint: RemotePaint)
}
