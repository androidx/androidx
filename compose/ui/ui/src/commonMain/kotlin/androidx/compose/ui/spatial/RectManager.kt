/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.spatial

import androidx.collection.mutableObjectListOf
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

internal class RectManager {

    val rects: RectList = RectList()

    private val callbacks = mutableObjectListOf<() -> Unit>()
    private var isDirty = false
    private var isFragmented = false

    fun invalidate() {
        isDirty = true
    }

    // TODO: we need to make sure these are dispatched after draw if needed
    fun dispatchCallbacks() {
        if (isDirty) {
            isDirty = false
            // The hierarchy is "settled" in terms of nodes being added/removed for this frame
            // This makes it a reasonable time to "defragment" the RectList data structure. This
            // will keep operations on this data structure efficient over time. This is a fairly
            // cheap operation to run, so we just do it every time
            if (isFragmented) {
                isFragmented = false
                rects.defragment()
            }
            callbacks.forEach { it() }
        }
    }

    fun registerOnChangedCallback(callback: () -> Unit): Any? {
        callbacks.add(callback)
        return callback
    }

    fun unregisterOnChangedCallback(token: Any?) {
        @Suppress("UNCHECKED_CAST")
        token as? (() -> Unit) ?: return
        callbacks.remove(token)
    }

    fun onLayoutLayerPositionalPropertiesChanged(layoutNode: LayoutNode) {
        @OptIn(ExperimentalComposeUiApi::class) if (!ComposeUiFlags.isRectTrackingEnabled) return
        val outerToInnerOffset = layoutNode.outerToInnerOffset()
        if (outerToInnerOffset.isSet) {
            // translational properties only. AARB still valid.
            layoutNode.outerToInnerOffset = outerToInnerOffset
            layoutNode.outerToInnerOffsetDirty = false
            layoutNode.forEachChild {
                // NOTE: this calls rectlist.move(...) so does not need to be recursive
                onLayoutPositionChanged(it, it.outerCoordinator.position, false)
            }
        } else {
            // there are rotations/skews/scales going on, so we need to do a more expensive update
            insertOrUpdateTransformedNodeSubhierarchy(layoutNode)
        }
    }

    fun onLayoutPositionChanged(
        layoutNode: LayoutNode,
        position: IntOffset,
        firstPlacement: Boolean
    ) {
        @OptIn(ExperimentalComposeUiApi::class) if (!ComposeUiFlags.isRectTrackingEnabled) return
        // Our goal here is to get the right "root" coordinates for every layout. We can use
        // LayoutCoordinates.localToRoot to calculate this somewhat readily, however this function
        // is getting called with a very high frequency and so it is important that extracting these
        // coordinates remains relatively cheap to limit the overhead of this tracking. The
        // LayoutCoordinates will traverse up the entire "spine" of the hierarchy, so as we do this
        // calculation for many nodes, we would be making many redundant calculations. In order to
        // minimize this, we store the "offsetFromRoot" of each layout node as we calculate it, and
        // attempt to utilize this value when calculating it for a node that is below it.
        // Additionally, we calculate and cache the parent's "outer to inner offset" which may
        val delegate = layoutNode.measurePassDelegate
        val width = delegate.measuredWidth
        val height = delegate.measuredHeight

        val parent = layoutNode.parent
        val offset: IntOffset

        val lastOffset = layoutNode.offsetFromRoot
        val lastSize = layoutNode.lastSize
        val lastWidth = lastSize.width
        val lastHeight = lastSize.height

        var hasNonTranslationTransformations = false

        if (parent != null) {
            val parentOffsetDirty = parent.outerToInnerOffsetDirty
            val parentOffset = parent.offsetFromRoot
            val prevOuterToInnerOffset = parent.outerToInnerOffset

            offset =
                if (parentOffset.isSet) {
                    val parentOuterInnerOffset =
                        if (parentOffsetDirty) {
                            val it = parent.outerToInnerOffset()

                            parent.outerToInnerOffset = it
                            parent.outerToInnerOffsetDirty = false
                            it
                        } else {
                            prevOuterToInnerOffset
                        }
                    hasNonTranslationTransformations = !parentOuterInnerOffset.isSet
                    parentOffset + parentOuterInnerOffset + position
                } else {
                    layoutNode.outerCoordinator.positionInRoot()
                }
        } else {
            // root
            offset = position
        }

        // If unset is returned then that means there is a rotation/skew/scale
        if (hasNonTranslationTransformations || !offset.isSet) {
            insertOrUpdateTransformedNode(layoutNode, position, firstPlacement)
            return
        }

        layoutNode.offsetFromRoot = offset
        layoutNode.lastSize = IntSize(width, height)

        val l = offset.x
        val t = offset.y
        val r = l + width
        val b = t + height

        if (!firstPlacement && offset == lastOffset && lastWidth == width && lastHeight == height) {
            return
        }

        insertOrUpdate(layoutNode, firstPlacement, l, t, r, b)
    }

    private fun insertOrUpdateTransformedNodeSubhierarchy(layoutNode: LayoutNode) {
        layoutNode.forEachChild {
            insertOrUpdateTransformedNode(it, it.outerCoordinator.position, false)
            insertOrUpdateTransformedNodeSubhierarchy(it)
        }
    }

    private val cachedRect = MutableRect(0f, 0f, 0f, 0f)

    private fun insertOrUpdateTransformedNode(
        layoutNode: LayoutNode,
        position: IntOffset,
        firstPlacement: Boolean,
    ) {
        val coord = layoutNode.outerCoordinator
        val delegate = layoutNode.measurePassDelegate
        val width = delegate.measuredWidth
        val height = delegate.measuredHeight
        val rect = cachedRect

        rect.set(
            left = position.x.toFloat(),
            top = position.y.toFloat(),
            right = (position.x + width).toFloat(),
            bottom = (position.y + height).toFloat(),
        )

        coord.boundingRectInRoot(rect)

        val l = rect.left.toInt()
        val t = rect.top.toInt()
        val r = rect.right.toInt()
        val b = rect.bottom.toInt()
        val id = layoutNode.semanticsId
        // NOTE: we call update here instead of move since the subhierarchy will not be moved by a
        // simple delta since we are dealing with rotation/skew/scale/etc.
        if (firstPlacement || !rects.update(id, l, t, r, b)) {
            val parentId = layoutNode.parent?.semanticsId ?: -1
            rects.insert(
                id,
                l,
                t,
                r,
                b,
                parentId = parentId,
            )
        }
        invalidate()
    }

    private fun insertOrUpdate(
        layoutNode: LayoutNode,
        firstPlacement: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val id = layoutNode.semanticsId
        if (firstPlacement || !rects.move(id, l, t, r, b)) {
            val parentId = layoutNode.parent?.semanticsId ?: -1
            rects.insert(
                id,
                l,
                t,
                r,
                b,
                parentId = parentId,
            )
        }
        invalidate()
    }

    private fun NodeCoordinator.positionInRoot(): IntOffset {
        // TODO: can we use offsetFromRoot here to speed up calculation?
        var position = Offset.Zero
        var coordinator: NodeCoordinator? = this
        while (coordinator != null) {
            val layer = coordinator.layer
            position += coordinator.position
            coordinator = coordinator.wrappedBy
            if (layer != null) {
                val matrix = layer.underlyingMatrix
                val analysis = matrix.analyzeComponents()
                if (analysis == 0b11) continue
                val hasNonTranslationComponents = analysis and 0b10 == 0
                if (hasNonTranslationComponents) {
                    return IntOffset.Max
                }
                position = matrix.map(position)
            }
        }
        return position.round()
    }

    private fun NodeCoordinator.boundingRectInRoot(rect: MutableRect) {
        // TODO: can we use offsetFromRoot here to speed up calculation?
        var coordinator: NodeCoordinator? = this
        while (coordinator != null) {
            val layer = coordinator.layer
            rect.translate(coordinator.position.toOffset())
            coordinator = coordinator.wrappedBy
            if (layer != null) {
                val matrix = layer.underlyingMatrix
                if (!matrix.isIdentity()) {
                    matrix.map(rect)
                }
            }
        }
    }

    private fun LayoutNode.outerToInnerOffset(): IntOffset {
        val terminator = outerCoordinator
        var position = Offset.Zero
        var coordinator: NodeCoordinator? = innerCoordinator
        while (coordinator != null) {
            if (coordinator === terminator) break
            val layer = coordinator.layer
            position += coordinator.position
            coordinator = coordinator.wrappedBy
            if (layer != null) {
                val matrix = layer.underlyingMatrix
                val analysis = matrix.analyzeComponents()
                if (analysis.isIdentity) continue
                if (analysis.hasNonTranslationComponents) {
                    return IntOffset.Max
                }
                position = matrix.map(position)
            }
        }
        return position.round()
    }

    fun remove(layoutNode: LayoutNode) {
        rects.remove(layoutNode.semanticsId)
        invalidate()
        isFragmented = true
    }
}

/**
 * Returns true if the offset is not IntOffset.Max. In this class we are using `IntOffset.Max` to be
 * a sentinel value for "unspecified" so that we can avoid boxing.
 */
private val IntOffset.isSet: Boolean
    get() = this != IntOffset.Max

/**
 * We have logic that looks at whether or not a Matrix is an identity matrix, in which case we avoid
 * doing expensive matrix calculations. Additionally, even if the matrix is non-identity, we can
 * avoid a lot of extra work if the matrix is only doing translations, and no rotations/skews/scale.
 *
 * Since checking for these conditions involves a lot of overlapping work, we have this bespoke
 * function which will return an Int that encodes the answer to both questions. If the 2nd bit of
 * the result is set, this means that there are no rotations/skews/scales. If the first bit of the
 * result is set, it means that there are no translations.
 *
 * This also means that the result of `0b11` indicates that it is the identity matrix.
 */
private fun Matrix.analyzeComponents(): Int {
    // See top-level comment
    val v = values
    if (v.size < 16) return 0
    val isIdentity3x3 =
        v[0] == 1f &&
            v[1] == 0f &&
            v[2] == 0f &&
            v[4] == 0f &&
            v[5] == 1f &&
            v[6] == 0f &&
            v[8] == 0f &&
            v[9] == 0f &&
            v[10] == 1f

    // translation components
    val hasNoTranslationComponents = v[12] == 0f && v[13] == 0f && v[14] == 0f && v[15] == 1f

    return isIdentity3x3.toInt() shl 1 or hasNoTranslationComponents.toInt()
}

@Suppress("NOTHING_TO_INLINE")
private inline val Int.isIdentity: Boolean
    get() = this == 0b11

@Suppress("NOTHING_TO_INLINE")
private inline val Int.hasNonTranslationComponents: Boolean
    get() = this and 0b10 == 0

@Suppress("NOTHING_TO_INLINE") private inline fun Boolean.toInt(): Int = if (this) 1 else 0
