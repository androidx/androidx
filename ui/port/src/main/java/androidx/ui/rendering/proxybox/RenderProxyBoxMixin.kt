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

import androidx.ui.engine.geometry.Offset
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.obj.ParentData
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.vectormath64.Matrix4

/**
 * Implementation of [RenderProxyBox].
 *
 * This class can be used as a mixin for situations where the proxying behavior
 * of [RenderProxyBox] is desired but inheriting from [RenderProxyBox] is
 * impractical (e.g. because you want to mix in other classes as well).
 */
// TODO(ianh): Remove this class once https://github.com/dart-lang/sdk/issues/15101 is fixed

// TODO(Migration/Andrey): abstract instead of a mixin, also changed RenderBox to
// TODO(Migration/Andrey): extend RenderObjectWithChildMixin
abstract class RenderProxyBoxMixin : RenderBox() {
//    // This class is intended to be used as a mixin, and should not be
//    // extended directly.
//    factory RenderProxyBoxMixin._() => null;

    override fun setupParentData(child: RenderObject) {
        // We don't actually use the offset argument in BoxParentData, so let's
        // avoid allocating it at all.
        if (child.parentData !is ParentData) {
            child.parentData = ParentData()
        }
    }

    override fun computeMinIntrinsicWidth(height: Double): Double {
        return child?.getMinIntrinsicWidth(height) ?: 0.0
    }

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        return child?.getMaxIntrinsicWidth(height) ?: 0.0
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        return child?.getMinIntrinsicHeight(width) ?: 0.0
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        return child?.getMaxIntrinsicHeight(width) ?: 0.0
    }

    // TODO(Migration/Andrey): Needs TextBaseline
//    override fun computeDistanceToActualBaseline(baseline: TextBaseline) {
//        if (child != null)
//            return child.getDistanceToActualBaseline(baseline);
//        return super.computeDistanceToActualBaseline(baseline);
//    }

    override fun performLayout() {
        if (child != null) {
            child!!.layout(constraints!!, parentUsesSize = true)
            size = child!!.size
        } else {
            performResize()
        }
    }

    // TODO(Migration/Andrey): Needs HitTestResult
//    override bool hitTestChildren(HitTestResult result, { Offset position }) {
//        return child?.hitTest(result, position: position) ?? false;
//    }

    override fun applyPaintTransform(child: RenderObject, transform: Matrix4) {}

    override fun paint(context: PaintingContext, offset: Offset) {
        child?.let { context.paintChild(it, offset) }
    }
}