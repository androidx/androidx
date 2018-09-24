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

import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.painting.alignment.Alignment
import androidx.ui.painting.alignment.AlignmentGeometry
import androidx.ui.rendering.box.BoxParentData
import androidx.ui.rendering.box.RenderBox

/**
 * Abstract class for one-child-layout render boxes that use a
 * [AlignmentGeometry] to align their children.
 *
 * Ctor comment:
 * Initializes member variables for subclasses.
 *
 * The [alignment] argument must not be null.
 */
abstract class RenderAligningShiftedBox(
    alignment: AlignmentGeometry = Alignment.center,
    textDirection: TextDirection? = null,
    child: RenderBox? = null
) : RenderShiftedBox(child) {

    private var _alignment: AlignmentGeometry = alignment
    private var _textDirection: TextDirection? = textDirection

    /**
     * How to align the child.
     *
     * The x and y values of the alignment control the horizontal and vertical
     * alignment, respectively. An x value of -1.0 means that the left edge of
     * the child is aligned with the left edge of the parent whereas an x value
     * of 1.0 means that the right edge of the child is aligned with the right
     * edge of the parent. Other values interpolate (and extrapolate) linearly.
     * For example, a value of 0.0 means that the center of the child is aligned
     * with the center of the parent.
     *
     * If this is set to a [AlignmentDirectional] object, then
     * [textDirection] must not be null.
     */
    var alignment: AlignmentGeometry
        get() = _alignment
        /**
         * Sets the alignment to a new value, and triggers a layout update.
         *
         * The new alignment must not be null.
         */
        set(value) {
            assert(value != null)
            if (_alignment == value)
                return
            _alignment = value
            _markNeedResolution()
        }

    /**
     * The text direction with which to resolve [alignment].
     *
     * This may be changed to null, but only after [alignment] has been changed
     * to a value that does not depend on the direction.
     */
    var textDirection: TextDirection?
        get() = _textDirection
        set(value) {
            if (_textDirection == value)
                return
            _textDirection = value
            _markNeedResolution()
        }

    /** A constructor to be used only when the extending class also has a mixin. */
    // TODO(gspencer): Remove this constructor once https://github.com/dart-lang/sdk/issues/15101 is fixed.
//    @protected
//    RenderAligningShiftedBox.mixin(AlignmentGeometry alignment,TextDirection textDirection, RenderBox child)
//    : this(alignment: alignment, textDirection: textDirection, child: child);

    var _resolvedAlignment: Alignment? = null

    private fun _resolve() {
        if (_resolvedAlignment != null)
            return
        _resolvedAlignment = alignment.resolve(textDirection)
    }

    private fun _markNeedResolution() {
        _resolvedAlignment = null
        markNeedsLayout()
    }

    /**
     * Apply the current [alignment] to the [child].
     *
     * Subclasses should call this method if they have a child, to have
     * this class perform the actual alignment. If there is no child,
     * do not call this method.
     *
     * This method must be called after the child has been laid out and
     * this object's own size has been set.
     */
    fun alignChild() {
        _resolve()
        assert(child != null)
        val child = this.child!!
        assert(!child.debugNeedsLayout)
        assert(child.hasSize)
        assert(hasSize)
        assert(_resolvedAlignment != null)
        val childParentData = child.parentData as BoxParentData
        childParentData.offset = _resolvedAlignment!!.alongOffset(size - child.size)
        onChildPositionChanged(child, childParentData.offset)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("alignment", alignment))
        properties.add(EnumProperty("textDirection", textDirection, defaultValue = null))
    }
}