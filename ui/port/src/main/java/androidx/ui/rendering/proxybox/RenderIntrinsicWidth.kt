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

import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox

/**
 * Sizes its child to the child's intrinsic width.
 *
 * Sizes its child's width to the child's maximum intrinsic width. If
 * [stepWidth] is non-null, the child's width will be snapped to a multiple of
 * the [stepWidth]. Similarly, if [stepHeight] is non-null, the child's height
 * will be snapped to a multiple of the [stepHeight].
 *
 * This class is useful, for example, when unlimited width is available and
 * you would like a child that would otherwise attempt to expand infinitely to
 * instead size itself to a more reasonable width.
 *
 * This class is relatively expensive, because it adds a speculative layout
 * pass before the final layout phase. Avoid using it where possible. In the
 * worst case, this render object can result in a layout that is O(N²) in the
 * depth of the tree.
 */
class RenderIntrinsicWidth(
    private var _stepWidth: Double? = null,
    private var _stepHeight: Double? = null,
    child: RenderBox? = null
) : RenderProxyBox(child) {
/** Creates a render object that sizes itself to its child's intrinsic width. */

    /** If non-null, force the child's width to be a multiple of this value. */
    var stepWidth: Double?
        get() { return _stepWidth }
    set(value) {
        if (value == _stepWidth)
            return
        _stepWidth = value
        markNeedsLayout()
    }

    /** If non-null, force the child's height to be a multiple of this value. */
    var stepHeight: Double?
        get() { return _stepHeight }
        set(value) {
            if (value == _stepHeight)
                return
            _stepHeight = value
            markNeedsLayout()
        }

    companion object {
        internal fun _applyStep(input: Double, step: Double?): Double {
            assert(input.isFinite())
            if (step == null)
                return input
            return Math.ceil(input / step) * step
        }
    }

    override fun computeMinIntrinsicWidth(height: Double) = computeMaxIntrinsicWidth(height)

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        if (child == null)
            return 0.0
        val width = child!!.getMaxIntrinsicWidth(height)
        return _applyStep(width, _stepWidth)
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        var resultWidth = width
        if (child == null)
            return 0.0
        if (!resultWidth.isFinite())
            resultWidth = computeMaxIntrinsicWidth(Double.POSITIVE_INFINITY)
        assert(resultWidth.isFinite())
        val height = child!!.getMinIntrinsicHeight(resultWidth)
        return _applyStep(height, _stepHeight)
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        var resultWidth = width
        if (child == null)
            return 0.0
        if (!resultWidth.isFinite())
            resultWidth = computeMaxIntrinsicWidth(Double.POSITIVE_INFINITY)
        assert(resultWidth.isFinite())
        val height = child!!.getMaxIntrinsicHeight(resultWidth)
        return _applyStep(height, _stepHeight)
    }

    override fun performLayout() {
        if (child != null) {
            var childConstraints: BoxConstraints = constraints!!
            if (!childConstraints.hasTightWidth) {
                val width = child!!.getMaxIntrinsicWidth(childConstraints.maxHeight)
                assert(width.isFinite())
                childConstraints = childConstraints.tighten(width = _applyStep(width, _stepWidth))
            }
            if (_stepHeight != null) {
                val height = child!!.getMaxIntrinsicHeight(childConstraints.maxWidth)
                assert(height.isFinite())
                childConstraints = childConstraints.tighten(height =
                _applyStep(height, _stepHeight))
            }
            child!!.layout(childConstraints, parentUsesSize = true)
            size = child!!.size
        } else {
            performResize()
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DoubleProperty.create("stepWidth", stepWidth))
        properties.add(DoubleProperty.create("stepHeight", stepHeight))
    }
}