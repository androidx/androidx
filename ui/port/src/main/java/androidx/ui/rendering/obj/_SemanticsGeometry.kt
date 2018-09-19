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

package androidx.ui.rendering.obj

import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.matrixutils.inverseTransformRect
import androidx.ui.vectormath64.Matrix4

// / From parent to child coordinate system.
private fun _transformRect(rect: Rect?, parent: RenderObject, child: RenderObject): Rect? {
    if (rect == null) {
        return null
    }
    if (rect.isEmpty()) {
        return Rect.zero
    }
    val transform: Matrix4 =
        Matrix4.identity()
    parent.applyPaintTransform(child, transform)
    return inverseTransformRect(transform, rect)
}

private fun _intersectRects(a: Rect?, b: Rect?): Rect? {
    if (a == null)
        return b
    if (b == null)
        return a
    return a.intersect(b)
}

// / Helper class that keeps track of the geometry of a [SemanticsNode].
// /
// / It is used to annotate a [SemanticsNode] with the current information for
// / [SemanticsNode.rect] and [SemanticsNode.transform].
// /
// / The `parentClippingRect` may be null if no clip is to be applied.
// /
// / The `ancestors` list has to include all [RenderObject] in order that are
// / located between the [SemanticsNode] whose geometry is represented here
// / (first [RenderObject] in the list) and its closest ancestor [RenderObject]
// / that also owns its own [SemanticsNode] (last [RenderObject] in the list).
internal class _SemanticsGeometry(
    parentSemanticsClipRect: Rect?,
    parentPaintClipRect: Rect?,
    ancestors: List<RenderObject>
) {

    // / Value for [SemanticsNode.transform].
    val transform: Matrix4

    // / Value for [SemanticsNode.parentSemanticsClipRect].
    var semanticsClipRect: Rect? = null
        private set

    // / Value for [SemanticsNode.parentPaintClipRect].
    var paintClipRect: Rect?
        private set

    // / Value for [SemanticsNode.rect].
    var rect: Rect
        private set

    // / Whether the [SemanticsNode] annotated with the geometric information
    // / tracked by this object should be marked as hidden because it is not
    // / visible on screen.
    // /
    // / Hidden elements should still be included in the tree to work around
    // / platform limitations (e.g. accessibility scrolling on iOS).
    // /
    // / See also:
    // /
    // /  * [SemanticsFlag.isHidden] for the purpose of marking a node as hidden.
    var markAsHidden: Boolean = false
        private set

    init {
        assert(ancestors.size > 1)

        transform = Matrix4.identity()
        semanticsClipRect = parentSemanticsClipRect
        paintClipRect = parentPaintClipRect
        for (index in ancestors.size - 1 downTo 1) {
            val parent: RenderObject = ancestors[index]
            val child: RenderObject = ancestors[index - 1]
            val parentSemanticsClipRect: Rect? = parent.describeSemanticsClip(child)
            if (parentSemanticsClipRect != null) {
                semanticsClipRect = parentSemanticsClipRect
                paintClipRect =
                        _intersectRects(paintClipRect, parent.describeApproximatePaintClip(child))
            } else {
                semanticsClipRect = _intersectRects(
                    semanticsClipRect,
                    parent.describeApproximatePaintClip(child)
                )
            }
            semanticsClipRect = _transformRect(semanticsClipRect, parent, child)
            paintClipRect = _transformRect(paintClipRect, parent, child)
            parent.applyPaintTransform(child, transform)
        }

        val owner: RenderObject = ancestors.first()
        rect = if (semanticsClipRect == null) {
            owner.semanticBounds!!
        } else {
            semanticsClipRect!!.intersect(owner.semanticBounds!!)
        }
        paintClipRect?.let {
            val paintRect: Rect = it.intersect(rect)
            markAsHidden = paintRect.isEmpty() && !rect.isEmpty()
            if (!markAsHidden)
                rect = paintRect
        }
    }

    // / Whether the [SemanticsNode] annotated with the geometric information tracked
    // / by this object can be dropped from the semantics tree without losing
    // / semantics information.
    val dropFromTree
        get() = rect.isEmpty()
}