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
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.Color
import androidx.ui.painting.Gradient
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.TextPainter
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.painting.TileMode
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.runtimeType

abstract class RenderCustomClip<T>(
    child: RenderBox?,
    /** If non-null, determines which clip to use on the child. */
    clipper: CustomClipper<T>?
) : RenderProxyBox(child) {

    var clipper: CustomClipper<T>? = clipper
    set(newClipper) {
        if (field == newClipper)
            return
        val oldClipper = field
        field = newClipper
        assert(newClipper != null || oldClipper != null)
        if (newClipper == null || oldClipper == null ||
            oldClipper.runtimeType != oldClipper.runtimeType ||
            newClipper.shouldReclip(oldClipper)) {
            markNeedsClip()
        }
        if (attached) {
            oldClipper?.reclip?.removeListener(markNeedsClip)
            newClipper?.reclip?.addListener(markNeedsClip)
        }
    }

    override fun attach(owner: Any) {
        super.attach(owner)
        clipper?.reclip?.addListener(markNeedsClip)
    }

    override fun detach() {
        clipper?.reclip?.removeListener(markNeedsClip)
        super.detach()
    }

    private val markNeedsClip = {
        clip = null
        markNeedsPaint()
        markNeedsSemanticsUpdate()
    }

    protected var clip: T? = null

    protected abstract val defaultClip: T

    override fun performLayout() {
        val oldSize = if (hasSize) size else null
        super.performLayout()
        if (oldSize != size) {
            clip = null
        }
    }

    protected fun updateClip() {
        clip = clip ?: clipper?.getClip(size) ?: defaultClip
    }

    override fun describeApproximatePaintClip(child: RenderObject): Rect? {
        return clipper?.getApproximateClipRect(size) ?: Offset.zero and size
    }

    protected var debugPaint: Paint? = null
    protected var debugText: TextPainter? = null
    override fun debugPaintSize(context: PaintingContext, offset: Offset) {
        assert {
            debugPaint = debugPaint ?: Paint().apply {
                shader = Gradient.linear(
                    Offset(0.0, 0.0),
                    Offset(10.0, 10.0),
                    listOf(
                        Color(0x00000000),
                        Color(0xFFFF00FF.toInt()),
                        Color(0xFFFF00FF.toInt()),
                        Color(0x00000000)
                    ),
                    listOf(0.25, 0.25, 0.75, 0.75),
                    TileMode.repeated
                )
                strokeWidth = 2.0
                style = PaintingStyle.stroke
                debugText = debugText ?: TextPainter(
                    text = TextSpan(
                        text = "✂",
                        style = TextStyle(
                            color = Color(0xFFFF00FF.toInt()),
                            fontSize = 14.0
                        )
                    ),
                    textDirection = TextDirection.RTL // doesn't matter, it's one character
                )
                // TODO("Migration|Andrey: needs TextPainter.layout")
                // debugText!!.layout()
            }
            true
        }
    }
}