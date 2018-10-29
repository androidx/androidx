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
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext

/**
 * Imposes additional constraints on its child.
 *
 * A render constrained box proxies most functions in the render box protocol
 * to its child, except that when laying out its child, it tightens the
 * constraints provided by its parent by enforcing the [additionalConstraints]
 * as well.
 *
 * For example, if you wanted [child] to have a minimum height of 50.0 logical
 * pixels, you could use `const BoxConstraints(minHeight: 50.0)` as the
 * [additionalConstraints].
 */
class RenderConstrainedBox(
    child: RenderBox? = null,
    /**
     * The [additionalConstraints] argument must not be null and must be valid.
     */
    private var _additionalConstraints: BoxConstraints
) : RenderProxyBox(child) {

    init {
        assert(_additionalConstraints.debugAssertIsValid())
    }

    // Additional constraints to apply to [child] during layout
    var additionalConstraints: BoxConstraints
        get() {
            return _additionalConstraints
        }
        set(value) {
            assert(value.debugAssertIsValid())
            if (_additionalConstraints == value)
                return
            _additionalConstraints = value
            markNeedsLayout()
        }

    override fun computeMinIntrinsicWidth(height: Double): Double {
        if (_additionalConstraints.hasBoundedWidth && _additionalConstraints.hasTightWidth)
            return _additionalConstraints.minWidth
        val width = super.computeMinIntrinsicWidth(height)
        if (_additionalConstraints.hasBoundedWidth)
            return _additionalConstraints.constrainWidth(width)
        return width
    }

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        if (_additionalConstraints.hasBoundedWidth && _additionalConstraints.hasTightWidth)
            return _additionalConstraints.minWidth
        val width = super.computeMaxIntrinsicWidth(height)
        if (_additionalConstraints.hasBoundedWidth)
            return _additionalConstraints.constrainWidth(width)
        return width
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        if (_additionalConstraints.hasBoundedHeight && _additionalConstraints.hasTightHeight)
            return _additionalConstraints.minHeight
        val height = super.computeMinIntrinsicHeight(width)
        if (_additionalConstraints.hasBoundedHeight)
            return _additionalConstraints.constrainHeight(height)
        return height
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        if (_additionalConstraints.hasBoundedHeight && _additionalConstraints.hasTightHeight)
            return _additionalConstraints.minHeight
        val height = super.computeMaxIntrinsicHeight(width)
        if (_additionalConstraints.hasBoundedHeight)
            return _additionalConstraints.constrainHeight(height)
        return height
    }

    override fun performLayout() {
        assert(constraints != null)
        if (child != null) {
            child!!.layout(_additionalConstraints.enforce(constraints!!), parentUsesSize = true)
            size = child!!.size
        } else {
            size = _additionalConstraints.enforce(constraints!!).constrain(Size.zero)
        }
    }

    override fun debugPaintSize(context: PaintingContext, offset: Offset) {
        super.debugPaintSize(context, offset)
        assert {
            if (child == null || child!!.size.isEmpty()) {
                val paint = Paint().apply {
                    color = Color(0x90909090.toInt())
                }
                context.canvas.drawRect(offset.and(size), paint)
            }
            true
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("additionalConstraints",
                additionalConstraints))
    }
}