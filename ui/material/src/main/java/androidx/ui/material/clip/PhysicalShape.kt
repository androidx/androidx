/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.clip

import androidx.ui.assert
import androidx.ui.core.Dimension
import androidx.ui.core.adapter.DensityConsumer
import androidx.ui.core.adapter.Draw
import androidx.ui.core.compareTo
import androidx.ui.core.dp
import androidx.ui.core.toPx
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path
import androidx.ui.painting.debugDisableShadows
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.composer


/**
 * A widget representing a physical layer that clips its children to a path.
 *
 * Physical layers cast shadows based on an [elevation] which is nominally in
 * logical pixels, coming vertically out of the rendering surface.
 *
 * [PhysicalModel] does the same but only supports shapes that can be expressed
 * as rectangles with rounded corners.
 *
 * See also:
 *
 *  * [ShapeBorderClipper], which converts a [ShapeBorder] to a [CustomerClipper], as
 *    needed by this widget.
 */
class PhysicalShape(
    /**
     * Determines which clip to use.
     *
     * If the path in question is expressed as a [ShapeBorder] subclass,
     * consider using the [ShapeBorderClipper] delegate class to adapt the
     * shape for use with this widget.
     */
    var clipper: CustomClipper<Path>,
    /** The z-coordinate at which to place this physical object. */
    var elevation: Dimension = 0.dp,
    /** The background color. */
    var color: Color,
    /** When elevation is non zero the color to use for the shadow color. */
    var shadowColor: Color = Color(0xFF000000.toInt()),
    @Children var children: () -> Unit
) : Component() {

//    // TODO("Migration|Andrey: Needs semantics in R4a")
//    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
//        super.debugFillProperties(properties)
//        properties.add(DiagnosticsProperty.create("clipper", clipper))
//        properties.add(FloatProperty.create("elevation", elevation))
//        properties.add(DiagnosticsProperty.create("color", color))
//        properties.add(DiagnosticsProperty.create("shadowColor", shadowColor))
//    }

    private val clipHolder = ClipHolder(this::recompose) { size ->
        val path = Path()
        path.addRect(Offset.zero and size)
        path
    }

    // TODO("Andrey: We need a mechanism to only allow taps within the clip path")
//    override fun hitTest(result: HitTestResult, position: Offset): Boolean {
//        if (clipper != null) {
//            updateClip()
//            assert(clip != null)
//            if (!clip!!.contains(position))
//                return false
//        }
//        return super.hitTest(result, position)
//    }

    override fun compose() {
        <DensityConsumer> density ->
            <Draw> canvas, parentSize ->
                val clip = clipHolder.getClip(clipper, parentSize, density)
                var paintShadows = true
                assert {
                    if (debugDisableShadows) {
                        if (elevation > 0.dp) {
                            val paint = Paint()
                            paint.color = shadowColor
                            paint.style = PaintingStyle.stroke
                            paint.strokeWidth = elevation.toPx(density) * 2.0f
                            canvas.drawPath(
                                clip,
                                paint
                            )
                        }
                        paintShadows = false
                    }
                    true
                }

                if (elevation != 0.dp && paintShadows) {
                    TODO("Migration|Andrey: Needs canvas.drawShadow. b/123215187")
    //                    canvas.drawShadow(
    //                        offsetPath,
    //                        shadowColor,
    //                        elevation,
    //                        color.alpha != 0xFF
    //                    )
                }
                val paint = Paint()
                paint.color = color
                paint.style = PaintingStyle.fill
                paint.strokeWidth = elevation.toPx(density) * 2.0f
                canvas.drawPath(clip, paint)
                val rect = Rect(0f, 0f, parentSize.width, parentSize.height)
                canvas.saveLayer(rect, Paint())
                canvas.clipPath(clip)
            </Draw>
        </DensityConsumer>
        <children/>
        <Draw> canvas, _ ->
            canvas.restore()
        </Draw>
        // TODO("Andrey: Call clipHolder.dispose() in onDispose effect")
    }

}
