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
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.painting.Path
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

/**
 * A widget that clips its child using a path.
 *
 * Calls a callback on a delegate whenever the widget is to be
 * painted. The callback returns a path and the widget prevents the
 * child from painting outside the path.
 *
 * Clipping to a path is expensive. Certain shapes have more
 * optimized widgets:
 *
 *  * To clip to a rectangle, consider [ClipRect].
 *  * To clip to an oval or circle, consider [ClipOval].
 *  * To clip to a rounded rectangle, consider [ClipRRect].
 */
class ClipPath(
    key: Key? = null,
    /**
     * If [clipper] is null, the clip will be a rectangle that matches the layout
     * size and location of the child. However, rather than use this default,
     * consider using a [ClipRect], which can achieve the same effect more
     * efficiently.
     */
    val clipper: CustomClipper<Path>? = null,
    child: Widget? = null
) : SingleChildRenderObjectWidget(key, child) {

    override fun createRenderObject(context: BuildContext) =
        RenderClipPath(clipper = clipper)

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        (renderObject as RenderClipPath).clipper = clipper
    }

    override fun didUnmountRenderObject(renderObject: RenderObject?) {
        (renderObject as RenderClipPath).clipper = null
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("clipper", clipper, defaultValue = null))
    }
}

/**
 * Clips its child using a path.
 *
 * Takes a delegate whose primary method returns a path that should
 * be used to prevent the child from painting outside the path.
 *
 * Clipping to a path is expensive. Certain shapes have more
 * optimized render objects:
 *
 *  * To clip to a rectangle, consider [RenderClipRect].
 *  * To clip to an oval or circle, consider [RenderClipOval].
 *  * To clip to a rounded rectangle, consider [RenderClipRRect].
 */
class RenderClipPath(
    child: RenderBox? = null,
    /**
     * If [clipper] is null, the clip will be a rectangle that matches the layout
     * size and location of the child. However, rather than use this default,
     * consider using a [RenderClipRect], which can achieve the same effect more
     * efficiently.
     */
    clipper: CustomClipper<Path>? = null
) : RenderCustomClip<Path>(child, clipper) {

    override val defaultClip
        get() = Path().apply {
            addRect(Offset.zero and size)
        }

    override fun hitTest(result: HitTestResult, position: Offset): Boolean {
        if (clipper != null) {
            updateClip()
            assert(clip != null)
            if (!clip!!.contains(position)) {
                return false
            }
        }
        return super.hitTest(result, position = position)
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        if (child != null) {
            updateClip()
            context.pushClipPath(
                needsCompositing, offset, Offset.zero and size,
                clip!!
            ) { context, offset ->
                super.paint(context, offset)
            }
        }
    }

    override fun debugPaintSize(context: PaintingContext, offset: Offset) {
        assert {
            if (child != null) {
                super.debugPaintSize(context, offset)
                context.canvas.drawPath(clip!!.shift(offset), debugPaint!!)
                // TODO("Migration|Andrey: needs TextPainter.paint")
//                debugText?.paint(context.canvas, offset);
            }
            true
        }
    }
}