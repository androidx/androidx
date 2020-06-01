/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.core

import android.graphics.RectF
import androidx.ui.core.focus.ModifiedFocusNode
import androidx.ui.core.keyinput.ModifiedKeyInputNode
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Paint
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.toPxPosition

/**
 * Measurable and Placeable type that has a position.
 */
internal abstract class LayoutNodeWrapper(
    internal val layoutNode: LayoutNode
) : Placeable(), Measurable, LayoutCoordinates {
    internal open val wrapped: LayoutNodeWrapper? = null
    internal var wrappedBy: LayoutNodeWrapper? = null

    /**
     * The scope used to measure the wrapped. InnerPlaceables are using the MeasureScope
     * of the LayoutNode. ModifiedLayoutNode2s are using their own instances MeasureScopes.
     * For fewer allocations, everything else is reusing the measure scope of their wrapped.
     */
    abstract val measureScope: MeasureScope

    // TODO(popam): avoid allocation here
    final override val measuredSize: IntPxSize
        get() = IntPxSize(measureResult.width, measureResult.height)
    // Size exposed to LayoutCoordinates.
    final override val size: IntPxSize get() = measuredSize

    final override var measurementConstraints = Constraints()

    open val invalidateLayerOnBoundsChange = true

    private var _measureResult: MeasureScope.MeasureResult? = null
    var measureResult: MeasureScope.MeasureResult
        get() = _measureResult ?: error(UnmeasuredError)
        internal set(value) {
            if (invalidateLayerOnBoundsChange &&
                (value.width != _measureResult?.width || value.height != _measureResult?.height)
            ) {
                findLayer()?.invalidate()
            }
            _measureResult = value
        }

    var position: IntPxPosition = IntPxPosition.Origin
        internal set(value) {
            if (invalidateLayerOnBoundsChange && value != field) {
                findLayer()?.invalidate()
            }
            field = value
        }

    override val parentCoordinates: LayoutCoordinates?
        get() {
            check(isAttached) { ExpectAttachedLayoutCoordinates }
            return layoutNode.layoutNodeWrapper.wrappedBy
        }

    // True when the wrapper is running its own placing block to obtain the position of the
    // wrapped, but is not interested in the position of the wrapped of the wrapped.
    var isShallowPlacing = false

    // TODO(mount): This is not thread safe.
    private var rectCache: RectF? = null

    /**
     * Whether a pointer that is relative to the device screen is in the bounds of this
     * LayoutNodeWrapper.
     */
    fun isGlobalPointerInBounds(globalPointerPosition: PxPosition): Boolean {
        // TODO(shepshapard): Right now globalToLocal has to traverse the tree all the way back up
        //  so calling this is expensive.  Would be nice to cache data such that this is cheap.
        val localPointerPosition = globalToLocal(globalPointerPosition)
        return localPointerPosition.x >= 0 &&
                localPointerPosition.x < measuredSize.width.value &&
                localPointerPosition.y >= 0 &&
                localPointerPosition.y < measuredSize.height.value
    }

    /**
     * Measures the modified child.
     */
    abstract fun performMeasure(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Placeable

    /**
     * Measures the modified child.
     */
    final override fun measure(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Placeable {
        measurementConstraints = constraints
        return performMeasure(constraints, layoutDirection)
    }

    /**
     * Places the modified child.
     */
    abstract override fun place(position: IntPxPosition)

    /**
     * Draws the content of the LayoutNode
     */
    abstract fun draw(canvas: Canvas)

    /**
     * Executes a hit test on any appropriate type associated with this [LayoutNodeWrapper].
     *
     * Override appropriately to either add a [PointerInputFilter] to [hitPointerInputFilters] or
     * to pass the execution on.
     *
     * @param pointerPositionRelativeToScreen The tested pointer position, which is relative to
     * the device screen.
     * @param hitPointerInputFilters The collection that the hit [PointerInputFilter]s will be
     * added to if hit.
     *
     * @return True if any [PointerInputFilter]s were hit and thus added to
     * [hitPointerInputFilters].
     */
    abstract fun hitTest(
        pointerPositionRelativeToScreen: PxPosition,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ): Boolean

    override fun childToLocal(child: LayoutCoordinates, childLocal: PxPosition): PxPosition {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        check(child.isAttached) { "Child $child is not attached!" }
        var wrapper = child as LayoutNodeWrapper
        var position = childLocal
        while (wrapper !== this) {
            position = wrapper.toParentPosition(position)

            val parent = wrapper.wrappedBy
            check(parent != null) {
                "childToLocal: child parameter is not a child of the LayoutCoordinates"
            }
            wrapper = parent
        }
        return position
    }

    override fun globalToLocal(global: PxPosition): PxPosition {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        val wrapper = wrappedBy ?: return fromParentPosition(
            global - layoutNode.requireOwner().calculatePosition().toPxPosition()
        )
        return fromParentPosition(wrapper.globalToLocal(global))
    }

    override fun localToGlobal(local: PxPosition): PxPosition {
        return localToRoot(local) + layoutNode.requireOwner().calculatePosition()
    }

    override fun localToRoot(local: PxPosition): PxPosition {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        var wrapper: LayoutNodeWrapper? = this
        var position = local
        while (wrapper != null) {
            position = wrapper.toParentPosition(position)
            wrapper = wrapper.wrappedBy
        }
        return position
    }

    protected inline fun withPositionTranslation(canvas: Canvas, block: (Canvas) -> Unit) {
        val x = position.x.value.toFloat()
        val y = position.y.value.toFloat()
        canvas.translate(x, y)
        block(canvas)
        canvas.translate(-x, -y)
    }

    /**
     * Converts [position] in the local coordinate system to a [PxPosition] in the
     * [parentCoordinates] coordinate system.
     */
    open fun toParentPosition(position: PxPosition): PxPosition = position + this.position

    /**
     * Converts [position] in the [parentCoordinates] coordinate system to a [PxPosition] in the
     * local coordinate system.
     */
    open fun fromParentPosition(position: PxPosition): PxPosition = position - this.position

    protected fun drawBorder(canvas: Canvas, paint: Paint) {
        val rect = Rect(
            left = 0.5f,
            top = 0.5f,
            right = measuredSize.width.value.toFloat() - 0.5f,
            bottom = measuredSize.height.value.toFloat() - 0.5f
        )
        canvas.drawRect(rect, paint)
    }

    /**
     * Attaches the [LayoutNodeWrapper] and its wrapped [LayoutNodeWrapper] to an active
     * LayoutNode.
     *
     * This will be called when the [LayoutNode] associated with this [LayoutNodeWrapper] is
     * attached to the [Owner].
     *
     * It is also called whenever the modifier chain is replaced and the [LayoutNodeWrapper]s are
     * recreated.
     */
    abstract fun attach()

    /**
     * Detaches the [LayoutNodeWrapper] and its wrapped [LayoutNodeWrapper] from an active
     * LayoutNode.
     *
     * This will be called when the [LayoutNode] associated with this [LayoutNodeWrapper] is
     * detached from the [Owner].
     *
     * It is also called whenever the modifier chain is replaced and the [LayoutNodeWrapper]s are
     * recreated.
     */
    abstract fun detach()

    /**
     * Modifies bounds to be in the parent LayoutNodeWrapper's coordinates, including clipping,
     * scaling, etc.
     */
    protected open fun rectInParent(bounds: RectF) {
        val x = position.x.value
        bounds.left += x
        bounds.right += x

        val y = position.y.value
        bounds.top += y
        bounds.bottom += y
    }

    override fun childBoundingBox(child: LayoutCoordinates): PxBounds {
        check(isAttached) { ExpectAttachedLayoutCoordinates }
        check(child.isAttached) { "Child $child is not attached!" }
        val bounds = rectCache ?: RectF().also { rectCache = it }
        bounds.set(
            0f,
            0f,
            child.size.width.value.toFloat(),
            child.size.height.value.toFloat()
        )
        var wrapper = child as LayoutNodeWrapper
        while (wrapper !== this) {
            wrapper.rectInParent(bounds)

            val parent = wrapper.wrappedBy
            check(parent != null) {
                "childToLocal: child parameter is not a child of the LayoutCoordinates"
            }
            wrapper = parent
        }
        return PxBounds(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom
        )
    }

    /**
     * Returns the layer that this wrapper will draw into.
     */
    open fun findLayer(): OwnedLayer? {
        return if (layoutNode.innerLayerWrapper != null) {
            wrappedBy?.findLayer()
        } else {
            layoutNode.findLayer()
        }
    }

    /**
     * Returns the first [ModifiedFocusNode] in the wrapper list that wraps this
     * [LayoutNodeWrapper].
     */
    abstract fun findPreviousFocusWrapper(): ModifiedFocusNode?

    /**
     * Returns the next [ModifiedFocusNode] in the wrapper list that is wrapped by this
     * [LayoutNodeWrapper].
     */
    abstract fun findNextFocusWrapper(): ModifiedFocusNode?

    /**
     * Returns the last [ModifiedFocusNode] found following this [LayoutNodeWrapper]. It searches
     * the wrapper list associated with this [LayoutNodeWrapper]
     */
    abstract fun findLastFocusWrapper(): ModifiedFocusNode?

    /**
     * Find the first ancestor that is a [ModifiedFocusNode].
     */
    internal fun findParentFocusNode(): ModifiedFocusNode? {
        // TODO(b/152066829): We shouldn't need to search through the parentLayoutNode, as the
        // wrappedBy property should automatically point to the last layoutWrapper of the parent.
        // Find out why this doesn't work.
        var focusParent = wrappedBy?.findPreviousFocusWrapper()
        if (focusParent != null) {
            return focusParent
        }

        var parentLayoutNode = layoutNode.parent
        while (parentLayoutNode != null) {
            focusParent = parentLayoutNode.layoutNodeWrapper.findLastFocusWrapper()
            if (focusParent != null) {
                return focusParent
            }
            parentLayoutNode = parentLayoutNode.parent
        }
        return null
    }

    /**
     *  Find the first ancestor that is a [ModifiedKeyInputNode].
     */
    internal fun findParentKeyInputNode(): ModifiedKeyInputNode? {
        // TODO(b/152066829): We shouldn't need to search through the parentLayoutNode, as the
        // wrappedBy property should automatically point to the last layoutWrapper of the parent.
        // Find out why this doesn't work.
        var keyInputParent = wrappedBy?.findPreviousKeyInputWrapper()
        if (keyInputParent != null) {
            return keyInputParent
        }

        var parentLayoutNode = layoutNode.parent
        while (parentLayoutNode != null) {
            keyInputParent = parentLayoutNode.layoutNodeWrapper.findLastKeyInputWrapper()
            if (keyInputParent != null) {
                return keyInputParent
            }
            parentLayoutNode = parentLayoutNode.parent
        }
        return null
    }

    /**
     * Returns the first [ModifiedKeyInputNode] in the wrapper list that wraps this
     * [LayoutNodeWrapper].
     */
    abstract fun findPreviousKeyInputWrapper(): ModifiedKeyInputNode?

    /**
     * Returns the next [ModifiedKeyInputNode] in the wrapper list that is wrapped by this
     * [LayoutNodeWrapper].
     */
    abstract fun findNextKeyInputWrapper(): ModifiedKeyInputNode?

    /**
     * Returns the last [ModifiedFocusNode] found following this [LayoutNodeWrapper]. It searches
     * the wrapper list associated with this [LayoutNodeWrapper]
     */
    abstract fun findLastKeyInputWrapper(): ModifiedKeyInputNode?

    internal companion object {
        const val ExpectAttachedLayoutCoordinates = "LayoutCoordinate operations are only valid " +
                "when isAttached is true"
        const val UnmeasuredError = "Asking for measurement result of unmeasured layout modifier"
    }
}
