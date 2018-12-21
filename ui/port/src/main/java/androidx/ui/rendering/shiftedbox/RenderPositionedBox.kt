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

package androidx.ui.rendering.shiftedbox

import androidx.ui.assert
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.FloatProperty
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path
import androidx.ui.painting.alignment.Alignment
import androidx.ui.painting.alignment.AlignmentGeometry
import androidx.ui.rendering.box.BoxParentData
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext
import kotlin.math.min

/**
 * Positions its child using a [AlignmentGeometry].
 *
 * For example, to align a box at the bottom right, you would pass this box a
 * tight constraint that is bigger than the child's natural size,
 * with an alignment of [Alignment.bottomRight].
 *
 * By default, sizes to be as big as possible in both axes. If either axis is
 * unconstrained, then in that direction it will be sized to fit the child's
 * dimensions. Using widthFactor and heightFactor you can force this latter
 * behavior in all cases.
 */
class RenderPositionedBox(
    child: RenderBox? = null,
    widthFactor: Float? = null,
    heightFactor: Float? = null,
    alignment: AlignmentGeometry = Alignment.center,
    textDirection: TextDirection? = null
) : RenderAligningShiftedBox(
        child = child,
        alignment = alignment,
        textDirection = textDirection
) {

    init {
        assert(widthFactor == null || widthFactor >= 0.0f)
        assert(heightFactor == null || heightFactor >= 0.0f)
    }

    var _widthFactor: Float? = widthFactor
    var _heightFactor: Float? = heightFactor

    /**
     * If non-null, sets its width to the child's width multiplied by this factor.
     *
     * Can be both greater and less than 1.0 but must be positive.
     */
    var widthFactor: Float?
        get() = _widthFactor
        set(value) {
            assert(value == null || value >= 0.0f)
            if (_widthFactor == value)
                return
            _widthFactor = value
            markNeedsLayout()
        }

    /**
     * If non-null, sets its height to the child's height multiplied by this factor.
     *
     * Can be both greater and less than 1.0 but must be positive.
     */
    var heightFactor: Float?
        get() = _heightFactor
        set(value) {
            assert(value == null || value >= 0.0f)
            if (_heightFactor == value)
                return
            _heightFactor = value
            markNeedsLayout()
        }

    override fun performLayout() {
        val shrinkWrapWidth = _widthFactor != null ||
                constraints!!.maxWidth == Float.POSITIVE_INFINITY
        val shrinkWrapHeight = _heightFactor != null ||
                constraints!!.maxHeight == Float.POSITIVE_INFINITY

        if (child != null) {
            child!!.layout(constraints!!.loosen(), parentUsesSize = true)
            size = constraints!!.constrain(
                    Size(
                        if (shrinkWrapWidth) {
                            child!!.size.width * (_widthFactor ?: 1.0f)
                        } else {
                            Float.POSITIVE_INFINITY
                        },
                        if (shrinkWrapHeight) {
                            child!!.size.height * (_heightFactor ?: 1.0f)
                        } else {
                            Float.POSITIVE_INFINITY
                        }
                    ))
            alignChild()
        } else {
            size = constraints!!.constrain(
                    Size(
                            if (shrinkWrapWidth) 0.0f else Float.POSITIVE_INFINITY,
                            if (shrinkWrapHeight) 0.0f else Float.POSITIVE_INFINITY
                    ))
        }
    }

    override fun debugPaintSize(context: PaintingContext, offset: Offset) {
        super.debugPaintSize(context, offset)
        assert {
            var paint: Paint?
            if (child != null && !child!!.size.isEmpty()) {
                paint = Paint().let {
                    it.style = PaintingStyle.stroke
                    it.strokeWidth = 1.0f
                    it.color = Color(0xFFFFFF00.toInt())
                    it
                }

                var path = Path()
                val childParentData = child!!.parentData as BoxParentData
                if (childParentData.offset.dy > 0.0f) {
                    // vertical alignment arrows
                    val headSize: Float = min(childParentData.offset.dy * 0.2f, 10.0f)
                    TODO("Migration/Filip: Wait for Path to be migrated")
//                    path
//                    ..moveTo(offset.dx + size.width / 2.0, offset.dy)
//                    ..relativeLineTo(0.0, childParentData.offset.dy - headSize)
//                    ..relativeLineTo(headSize, 0.0)
//                    ..relativeLineTo(-headSize, headSize)
//                    ..relativeLineTo(-headSize, -headSize)
//                    ..relativeLineTo(headSize, 0.0)
//                    ..moveTo(offset.dx + size.width / 2.0, offset.dy + size.height)
//                    ..relativeLineTo(0.0, -childParentData.offset.dy + headSize)
//                    ..relativeLineTo(headSize, 0.0)
//                    ..relativeLineTo(-headSize, -headSize)
//                    ..relativeLineTo(-headSize, headSize)
//                    ..relativeLineTo(headSize, 0.0);
                    context.canvas.drawPath(path, paint)
                }
                if (childParentData.offset.dx > 0.0f) {
                    // horizontal alignment arrows
                    val headSize: Float = min(childParentData.offset.dx * 0.2f, 10.0f)
                    TODO("Migration/Filip: Wait for Path to be migrated")
//                    path
//                    ..moveTo(offset.dx, offset.dy + size.height / 2.0)
//                    ..relativeLineTo(childParentData.offset.dx - headSize, 0.0)
//                    ..relativeLineTo(0.0, headSize)
//                    ..relativeLineTo(headSize, -headSize)
//                    ..relativeLineTo(-headSize, -headSize)
//                    ..relativeLineTo(0.0, headSize)
//                    ..moveTo(offset.dx + size.width, offset.dy + size.height / 2.0)
//                    ..relativeLineTo(-childParentData.offset.dx + headSize, 0.0)
//                    ..relativeLineTo(0.0, headSize)
//                    ..relativeLineTo(-headSize, -headSize)
//                    ..relativeLineTo(headSize, -headSize)
//                    ..relativeLineTo(0.0, headSize);
//                    context.canvas.drawPath(path, paint);
                }
            } else {
                paint = Paint().let {
                    it.color = Color(0x90909090.toInt())
                    it
                }
                context.canvas.drawRect(offset and size, paint)
            }
            true
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(FloatProperty.create("widthFactor", _widthFactor, ifNull = "expand"))
        properties.add(FloatProperty.create("heightFactor", _heightFactor, ifNull = "expand"))
    }
}
