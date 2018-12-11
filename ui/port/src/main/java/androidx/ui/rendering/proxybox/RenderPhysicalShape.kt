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

import androidx.ui.assert
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path
import androidx.ui.painting.debugDisableShadows
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext

internal val defaultPaint = Paint()
internal val transparentPaint = Paint().apply {
    color = Color(0x00000000)
}

/**
 * Creates a physical shape layer that clips its child to a [Path].
 *
 * A physical shape layer casts a shadow based on its [elevation].
 *
 * See also:
 *
 * * [RenderPhysicalModel], which is optimized for rounded rectangles and
 *   circles.
 */
class RenderPhysicalShape(
    child: RenderBox? = null,
    clipper: CustomClipper<Path>,
    elevation: Double,
    color: Color,
    shadowColor: Color = Color(0xFF000000.toInt())
) : RenderPhysicalModelBase<Path>(child, elevation, color, shadowColor, clipper) {

    override val defaultClip get() = Path().apply { addRect(Offset.zero and size) }

    override fun hitTest(result: HitTestResult, position: Offset): Boolean {
        if (clipper != null) {
            updateClip()
            assert(clip != null)
            if (!clip!!.contains(position))
                return false
        }
        return super.hitTest(result, position)
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        if (child != null) {
            updateClip()
            val offsetBounds = offset and size
            val offsetPath = clip!!.shift(offset)
            var paintShadows = true
            assert {
                if (debugDisableShadows) {
                    if (elevation > 0.0) {
                        context.canvas.drawPath(
                            offsetPath,
                            Paint().apply {
                                color = shadowColor
                                style = PaintingStyle.stroke
                                strokeWidth = elevation * 2.0
                            }
                        )
                    }
                    paintShadows = false
                }
                true
            }
            if (needsCompositing) {
                TODO("Migration|Andrey: Needs PhysicalModelLayer")
//                val physicalModel = PhysicalModelLayer(
//                        clipPath= offsetPath,
//                elevation= if (paintShadows) elevation else 0.0,
//                color= color,
//                shadowColor= shadowColor,
//                )
//                context.pushLayer(physicalModel, super.paint, offset, childPaintBounds= offsetBounds);
            } else {
                val canvas = context.canvas
                if (elevation != 0.0 && paintShadows) {
                    // The drawShadow call doesn't add the region of the shadow to the
                    // picture's bounds, so we draw a hardcoded amount of extra space to
                    // account for the maximum potential area of the shadow.
                    // TODO(jsimmons): remove this when Skia does it for us.
                    canvas.drawRect(
                        offsetBounds.inflate(20.0),
                        transparentPaint
                    )
                    TODO("Migration|Andrey: Needs canvas.drawShadow")
//                    canvas.drawShadow(
//                        offsetPath,
//                        shadowColor,
//                        elevation,
//                        color.alpha != 0xFF
//                    )
                }
                canvas.drawPath(offsetPath, Paint().apply {
                    color = this@RenderPhysicalShape.color
                    style = PaintingStyle.fill
                })
                canvas.saveLayer(offsetBounds, defaultPaint)
                canvas.clipPath(offsetPath)
                super.paint(context, offset)
                canvas.restore()
                assert(context.canvas == canvas) {
                    "canvas changed even though " +
                            "needsCompositing was false"
                }
            }
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("clipper", clipper))
    }
}

/**
 * A physical model layer casts a shadow based on its [elevation].
 *
 * The concrete implementations [RenderPhysicalModel] and [RenderPhysicalShape]
 * determine the actual shape of the physical model.
 */
abstract class RenderPhysicalModelBase<T>(
    child: RenderBox?,
    elevation: Double,
    color: Color,
    shadowColor: Color,
    clipper: CustomClipper<T>?
) : RenderCustomClip<T>(child, clipper) {

    /**
     * The z-coordinate at which to place this material.
     *
     * If [debugDisableShadows] is set, this value is ignored and no shadow is
     * drawn (an outline is rendered instead).
     */
    var elevation: Double = elevation
        set(value) {
            if (field == value)
                return
            val didNeedCompositing = alwaysNeedsCompositing
            field = value
            if (didNeedCompositing != alwaysNeedsCompositing)
                markNeedsCompositingBitsUpdate()
            markNeedsPaint()
        }

    /** The shadow color. */
    var shadowColor: Color = shadowColor
        set(value) {
            if (field == value)
                return
            field = value
            markNeedsPaint()
        }

    /** The background color. */
    var color: Color = color
        set(value) {
            if (field == value)
                return
            field = value
            markNeedsPaint()
        }

    // TODO(Migration/Andrey: Not sure we need this)
//    // On Fuchsia, the system compositor is responsible for drawing shadows
//    // for physical model layers with non-zero elevation.
//    @override
//    bool get alwaysNeedsCompositing => _elevation != 0.0 && defaultTargetPlatform == TargetPlatform.fuchsia;

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DoubleProperty.create("elevation", elevation))
        properties.add(DiagnosticsProperty.create("color", color))
        properties.add(DiagnosticsProperty.create("shadowColor", color))
    }
}