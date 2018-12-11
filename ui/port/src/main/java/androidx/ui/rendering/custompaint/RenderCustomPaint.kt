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

package androidx.ui.rendering.custompaint

import androidx.ui.assert
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.Key
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.painting.Canvas
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.proxybox.RenderProxyBox
import androidx.ui.runtimeType
import androidx.ui.semantics.SemanticsConfiguration
import androidx.ui.semantics.SemanticsNode

/**
 * Provides a canvas on which to draw during the paint phase.
 *
 * When asked to paint, [RenderCustomPaint] first asks its [painter] to paint
 * on the current canvas, then it paints its child, and then, after painting
 * its child, it asks its [foregroundPainter] to paint. The coordinate system of
 * the canvas matches the coordinate system of the [CustomPaint] object. The
 * painters are expected to paint within a rectangle starting at the origin and
 * encompassing a region of the given size. (If the painters paint outside
 * those bounds, there might be insufficient memory allocated to rasterize the
 * painting commands and the resulting behavior is undefined.)
 *
 * Painters are implemented by subclassing or implementing [CustomPainter].
 *
 * Because custom paint calls its painters during paint, you cannot mark the
 * tree as needing a new layout during the callback (the layout for this frame
 * has already happened).
 *
 * Custom painters normally size themselves to their child. If they do not have
 * a child, they attempt to size themselves to the [preferredSize], which
 * defaults to [Size.zero].
 *
 * See also:
 *
 *  * [CustomPainter], the class that custom painter delegates should extend.
 *  * [Canvas], the API provided to custom painter delegates.
 */
class RenderCustomPaint(
    /**
     * The background custom paint delegate.
     *
     * This painter, if non-null, is called to paint behind the children.
     */
    painter: CustomPainter? = null,
    /**
     * The foreground custom paint delegate.
     *
     * This painter, if non-null, is called to paint in front of the children.
     */
    foregroundPainter: CustomPainter? = null,
    /**
     * The size that this [RenderCustomPaint] should aim for, given the layout
     * constraints, if there is no child.
     *
     * Defaults to [Size.zero].
     *
     * If there's a child, this is ignored, and the size of the child is used
     * instead.
     */
    preferredSize: Size = Size.zero,
    /**
     * Whether to hint that this layer's painting should be cached.
     *
     * The compositor contains a raster cache that holds bitmaps of layers in
     * order to avoid the cost of repeatedly rendering those layers on each
     * frame. If this flag is not set, then the compositor will apply its own
     * heuristics to decide whether the this layer is complex enough to benefit
     * from caching.
     */
    var isComplex: Boolean = false,
    /**
     * Whether the raster cache should be told that this painting is likely
     * to change in the next frame.
     */
    var willChange: Boolean = false,
    child: RenderBox? = null
) : RenderProxyBox(child) {

    /**
     * Set a new background custom paint delegate.
     *
     * If the new delegate is the same as the previous one, this does nothing.
     *
     * If the new delegate is the same class as the previous one, then the new
     * delegate has its [CustomPainter.shouldRepaint] called; if the result is
     * true, then the delegate will be called.
     *
     * If the new delegate is a different class than the previous one, then the
     * delegate will be called.
     *
     * If the new value is null, then there is no background custom painter.
     */
    var painter: CustomPainter? = painter
        set(value) {
            if (field == value)
                return
            val oldPainter = field
            field = value
            didUpdatePainter(value, oldPainter)
        }

    /**
     * Set a new foreground custom paint delegate.
     *
     * If the new delegate is the same as the previous one, this does nothing.
     *
     * If the new delegate is the same class as the previous one, then the new
     * delegate has its [CustomPainter.shouldRepaint] called; if the result is
     * true, then the delegate will be called.
     *
     * If the new delegate is a different class than the previous one, then the
     * delegate will be called.
     *
     * If the new value is null, then there is no foreground custom painter.
     */
    var foregroundPainter: CustomPainter? = foregroundPainter
        set(value) {
            if (field == value)
                return
            val oldPainter = field
            field = value
            didUpdatePainter(value, oldPainter)
        }

    private val markNeedsPaintCallback = { markNeedsPaint() }

    private fun didUpdatePainter(newPainter: CustomPainter?, oldPainter: CustomPainter?) {
        // Check if we need to repaint.
        if (newPainter == null) {
            assert(oldPainter != null) // We should be called only for changes.
            markNeedsPaint()
        } else if (oldPainter == null ||
            newPainter.runtimeType != oldPainter.runtimeType ||
            newPainter.shouldRepaint(oldPainter)
        ) {
            markNeedsPaint()
        }
        if (attached) {
            oldPainter?.removeListener(markNeedsPaintCallback)
            newPainter?.addListener(markNeedsPaintCallback)
        }

        // Check if we need to rebuild semantics.
        if (newPainter == null) {
            assert(oldPainter != null) // We should be called only for changes.
            if (attached)
                markNeedsSemanticsUpdate()
        } else if (oldPainter == null ||
            newPainter.runtimeType != oldPainter.runtimeType ||
            newPainter.shouldRebuildSemantics(oldPainter)
        ) {
            markNeedsSemanticsUpdate()
        }
    }

    var preferredSize: Size = preferredSize
        set(value) {
            if (field == value)
                return
            field = value
            markNeedsLayout()
        }

    override fun attach(owner: Any) {
        super.attach(owner)
        painter?.addListener(markNeedsPaintCallback)
        foregroundPainter?.addListener(markNeedsPaintCallback)
    }

    override fun detach() {
        painter?.removeListener(markNeedsPaintCallback)
        foregroundPainter?.removeListener(markNeedsPaintCallback)
        super.detach()
    }

    override fun hitTestChildren(result: HitTestResult, position: Offset): Boolean {
        if (foregroundPainter != null && (foregroundPainter!!.hitTest(position) == true))
            return true
        return super.hitTestChildren(result, position = position)
    }

    override fun hitTestSelf(position: Offset): Boolean {
        return painter != null && (painter!!.hitTest(position) ?: true)
    }

    override fun performResize() {
        size = constraints!!.constrain(preferredSize)
        markNeedsSemanticsUpdate()
    }

    private fun paintWithPainter(canvas: Canvas, offset: Offset, painter: CustomPainter) {
        var debugPreviousCanvasSaveCount = 0
        canvas.save()
        assert { debugPreviousCanvasSaveCount = canvas.getSaveCount(); true }
        if (offset != Offset.zero)
            canvas.translate(offset.dx, offset.dy)
        painter.paint(canvas, size)
        assert {
            // This isn't perfect. For example, we can't catch the case of
            // someone first restoring, then setting a transform or whatnot,
            // then saving.
            // If this becomes a real problem, we could add logic to the
            // Canvas class to lock the canvas at a particular save count
            // such that restore() fails if it would take the lock count
            // below that number.
            val debugNewCanvasSaveCount = canvas.getSaveCount()
            if (debugNewCanvasSaveCount > debugPreviousCanvasSaveCount) {
                throw FlutterError(
                    "The $painter custom painter called canvas.save() or canvas.saveLayer() at " +
                            "least ${debugNewCanvasSaveCount - debugPreviousCanvasSaveCount} " +
                            "more time${if (debugNewCanvasSaveCount -
                                debugPreviousCanvasSaveCount == 1
                            ) "" else "s"} than it called " +
                            "canvas.restore().\nThis leaves the canvas in an inconsistent state " +
                            "and will probably result in a broken display.\nYou must pair each " +
                            "call to save()/saveLayer() with a later matching call to restore()."
                )
            }
            if (debugNewCanvasSaveCount < debugPreviousCanvasSaveCount) {
                throw FlutterError(
                    "The $painter custom painter called canvas.restore() " +
                            "${debugPreviousCanvasSaveCount - debugNewCanvasSaveCount} more " +
                            "time${if (debugPreviousCanvasSaveCount -
                                debugNewCanvasSaveCount == 1
                            ) "" else "s"} than it called " +
                            "canvas.save() or canvas.saveLayer().\nThis leaves the canvas in an " +
                            "inconsistent state and will result in a broken display.\nYou should " +
                            "only call restore() if you first called save() or saveLayer()."
                )
            }
            debugNewCanvasSaveCount == debugPreviousCanvasSaveCount
        }
        canvas.restore()
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        if (painter != null) {
            paintWithPainter(context.canvas, offset, painter!!)
            setRasterCacheHints(context)
        }
        super.paint(context, offset)
        if (foregroundPainter != null) {
            paintWithPainter(context.canvas, offset, foregroundPainter!!)
            setRasterCacheHints(context)
        }
    }

    private fun setRasterCacheHints(context: PaintingContext) {
        if (isComplex)
            context.setIsComplexHint()
        if (willChange)
            context.setWillChangeHint()
    }

    /** Builds semantics for the picture drawn by [painter]. */
    private var backgroundSemanticsBuilder: SemanticsBuilderCallback? = null

    /** Builds semantics for the picture drawn by [foregroundPainter]. */
    private var foregroundSemanticsBuilder: SemanticsBuilderCallback? = null

    override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)
        backgroundSemanticsBuilder = painter?.semanticsBuilder
        foregroundSemanticsBuilder = foregroundPainter?.semanticsBuilder
        config.isSemanticBoundary = backgroundSemanticsBuilder != null ||
                foregroundSemanticsBuilder != null
    }

    /** Describe the semantics of the picture painted by the [painter]. */
    private var backgroundSemanticsNodes: List<SemanticsNode>? = null

    /** Describe the semantics of the picture painted by the [foregroundPainter]. */
    private var foregroundSemanticsNodes: List<SemanticsNode>? = null

    override fun assembleSemanticsNode(
        node: SemanticsNode,
        config: SemanticsConfiguration,
        children: Collection<SemanticsNode>
    ) {
        assert {
            if (child == null && children.isNotEmpty()) {
                throw FlutterError(
                    "$runtimeType does not have a child widget but received a " +
                            "non-empty list of child SemanticsNode:\n + " +
                            children.joinToString("\n")
                )
            }
            true
        }

        val backgroundSemantics = backgroundSemanticsBuilder?.invoke(size) ?: listOf()
        backgroundSemanticsNodes = updateSemanticsChildren(
            backgroundSemanticsNodes,
            backgroundSemantics
        )

        val foregroundSemantics = foregroundSemanticsBuilder?.invoke(size) ?: listOf()
        foregroundSemanticsNodes = updateSemanticsChildren(
            foregroundSemanticsNodes,
            foregroundSemantics
        )

        val hasBackgroundSemantics = backgroundSemanticsNodes?.isNotEmpty() ?: false
        val hasForegroundSemantics = foregroundSemanticsNodes?.isNotEmpty() ?: false
        val finalChildren = mutableListOf<SemanticsNode>()
        if (hasBackgroundSemantics) {
            finalChildren.addAll(backgroundSemanticsNodes!!)
        }
        finalChildren.addAll(children)
        if (hasForegroundSemantics) {
            finalChildren.addAll(foregroundSemanticsNodes!!)
        }
        super.assembleSemanticsNode(node, config, finalChildren)
    }

    override fun clearSemantics() {
        super.clearSemantics()
        backgroundSemanticsNodes = null
        foregroundSemanticsNodes = null
    }

    /**
     * Updates the nodes of `oldSemantics` using data in `newChildSemantics`, and
     * returns a new list containing child nodes sorted according to the order
     * specified by `newChildSemantics`.
     *
     * [SemanticsNode]s that match [CustomPainterSemantics] by [Key]s preserve
     * their [SemanticsNode.key] field. If a node with the same key appears in
     * a different position in the list, it is moved to the new position, but the
     * same object is reused.
     *
     * [SemanticsNode]s whose `key` is null may be updated from
     * [CustomPainterSemantics] whose `key` is also null. However, the algorithm
     * does not guarantee it. If your semantics require that specific nodes are
     * updated from specific [CustomPainterSemantics], it is recommended to match
     * them by specifying non-null keys.
     *
     * The algorithm tries to be as close to [RenderObjectElement.updateChildren]
     * as possible, deviating only where the concepts diverge between widgets and
     * semantics. For example, a [SemanticsNode] can be updated from a
     * [CustomPainterSemantics] based on `Key` alone; their types are not
     * considered because there is only one type of [SemanticsNode]. There is no
     * concept of a "forgotten" node in semantics, deactivated nodes, or global
     * keys.
     */
    private fun updateSemanticsChildren(
        oldSemantics: List<SemanticsNode>?,
        newChildSemantics: List<CustomPainterSemantics>?
    ): List<SemanticsNode> {
        val oldSemantics = oldSemantics ?: listOf()
        val newChildSemantics = newChildSemantics ?: listOf()

        assert {
            val keys = mutableMapOf<Key, Int>()
            val errors = StringBuffer()
            newChildSemantics.forEachIndexed { i, child ->
                val child = newChildSemantics[i]
                if (child.key != null) {
                    if (keys.containsKey(child.key)) {
                        errors.appendln("- duplicate key ${child.key} found at position $i")
                    }
                    keys[child.key] = i
                }
            }

            if (errors.isNotEmpty()) {
                throw FlutterError(
                    "Failed to update the list of CustomPainterSemantics:\n$errors"
                )
            }

            true
        }

        var newChildrenTop = 0
        var oldChildrenTop = 0
        var newChildrenBottom = newChildSemantics.size - 1
        var oldChildrenBottom = oldSemantics.size - 1

        val newChildren = arrayOfNulls<SemanticsNode>(newChildSemantics.size)

        // Update the top of the list.
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            val oldChild = oldSemantics[oldChildrenTop]
            val newSemantics = newChildSemantics[newChildrenTop]
            if (!canUpdateSemanticsChild(oldChild, newSemantics))
                break
            val newChild = updateSemanticsChild(oldChild, newSemantics)
            newChildren[newChildrenTop] = newChild
            newChildrenTop += 1
            oldChildrenTop += 1
        }

        // Scan the bottom of the list.
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            val oldChild = oldSemantics[oldChildrenBottom]
            val newChild = newChildSemantics[newChildrenBottom]
            if (!canUpdateSemanticsChild(oldChild, newChild))
                break
            oldChildrenBottom -= 1
            newChildrenBottom -= 1
        }

        // Scan the old children in the middle of the list.
        val haveOldChildren = oldChildrenTop <= oldChildrenBottom
        var oldKeyedChildren: MutableMap<Key, SemanticsNode>? = null
        if (haveOldChildren) {
            oldKeyedChildren = mutableMapOf()
            while (oldChildrenTop <= oldChildrenBottom) {
                val oldChild = oldSemantics[oldChildrenTop]
                if (oldChild.key != null)
                    oldKeyedChildren[oldChild.key] = oldChild
                oldChildrenTop += 1
            }
        }

        // Update the middle of the list.
        while (newChildrenTop <= newChildrenBottom) {
            var oldChild: SemanticsNode? = null
            val newSemantics = newChildSemantics[newChildrenTop]
            if (haveOldChildren) {
                val key = newSemantics.key
                if (key != null) {
                    oldChild = oldKeyedChildren!!.get(key)
                    if (oldChild != null) {
                        if (canUpdateSemanticsChild(oldChild, newSemantics)) {
                            // we found a match!
                            // remove it from oldKeyedChildren so we don't unsync it later
                            oldKeyedChildren.remove(key)
                        } else {
                            // Not a match, let's pretend we didn't see it for now.
                            oldChild = null
                        }
                    }
                }
            }
            assert(oldChild == null || canUpdateSemanticsChild(oldChild, newSemantics))
            val newChild = updateSemanticsChild(oldChild, newSemantics)
            assert(oldChild == newChild || oldChild == null)
            newChildren[newChildrenTop] = newChild
            newChildrenTop += 1
        }

        // We've scanned the whole list.
        assert(oldChildrenTop == oldChildrenBottom + 1)
        assert(newChildrenTop == newChildrenBottom + 1)
        assert(newChildSemantics.size - newChildrenTop == oldSemantics.size - oldChildrenTop)
        newChildrenBottom = newChildSemantics.size - 1
        oldChildrenBottom = oldSemantics.size - 1

        // Update the bottom of the list.
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            val oldChild = oldSemantics[oldChildrenTop]
            val newSemantics = newChildSemantics[newChildrenTop]
            assert(canUpdateSemanticsChild(oldChild, newSemantics))
            val newChild = updateSemanticsChild(oldChild, newSemantics)
            assert(oldChild == newChild)
            newChildren[newChildrenTop] = newChild
            newChildrenTop += 1
            oldChildrenTop += 1
        }

        assert {
            newChildren.forEach {
                assert(it != null)
            }
            true
        }

        return newChildren.map { it!! }.toList()
    }

    /**
     * Whether `oldChild` can be updated with properties from `newSemantics`.
     *
     * If `oldChild` can be updated, it is updated using [_updateSemanticsChild].
     * Otherwise, the node is replaced by a new instance of [SemanticsNode].
     */
    private fun canUpdateSemanticsChild(
        oldChild: SemanticsNode,
        newSemantics: CustomPainterSemantics
    ): Boolean {
        return oldChild.key == newSemantics.key
    }

    /**
     * Updates `oldChild` using the properties of `newSemantics`.
     *
     * This method requires that `_canUpdateSemanticsChild(oldChild, newSemantics)`
     * is true prior to calling it.
     */
    private fun updateSemanticsChild(
        oldChild: SemanticsNode?,
        newSemantics: CustomPainterSemantics
    ): SemanticsNode {
        assert(oldChild == null || canUpdateSemanticsChild(oldChild, newSemantics))

        val newChild = oldChild ?: SemanticsNode(
            key = newSemantics.key
        )

        val properties = newSemantics.properties
        val config = SemanticsConfiguration()

        if (properties.checked != null) {
            config.isChecked = properties.checked
        }
        if (properties.selected != null) {
            config.isSelected = properties.selected
        }
        if (properties.button != null) {
            config.isButton = properties.button
        }
        if (properties.textField != null) {
            config.isTextField = properties.textField
        }
        if (properties.focused != null) {
            config.isFocused = properties.focused
        }
        if (properties.enabled != null) {
            config.isEnabled = properties.enabled
        }
        if (properties.inMutuallyExclusiveGroup != null) {
            config.isInMutuallyExclusiveGroup = properties.inMutuallyExclusiveGroup
        }
        if (properties.obscured != null) {
            config.isObscured = properties.obscured
        }
        if (properties.hidden != null) {
            config.isHidden = properties.hidden
        }
        if (properties.header != null) {
            config.isHeader = properties.header
        }
        if (properties.scopesRoute != null) {
            config.scopesRoute = properties.scopesRoute
        }
        if (properties.namesRoute != null) {
            config.namesRoute = properties.namesRoute
        }
        if (properties.label != null) {
            config.label = properties.label
        }
        if (properties.value != null) {
            config.value = properties.value
        }
        if (properties.increasedValue != null) {
            config.increasedValue = properties.increasedValue
        }
        if (properties.decreasedValue != null) {
            config.decreasedValue = properties.decreasedValue
        }
        if (properties.hint != null) {
            config.hint = properties.hint
        }
        if (properties.textDirection != null) {
            config.textDirection = properties.textDirection
        }
        if (properties.onTap != null) {
            config.onTap = properties.onTap
        }
        if (properties.onLongPress != null) {
            config.onLongPress = properties.onLongPress
        }
        if (properties.onScrollLeft != null) {
            config.onScrollLeft = properties.onScrollLeft
        }
        if (properties.onScrollRight != null) {
            config.onScrollRight = properties.onScrollRight
        }
        if (properties.onScrollUp != null) {
            config.onScrollUp = properties.onScrollUp
        }
        if (properties.onScrollDown != null) {
            config.onScrollDown = properties.onScrollDown
        }
        if (properties.onIncrease != null) {
            config.onIncrease = properties.onIncrease
        }
        if (properties.onDecrease != null) {
            config.onDecrease = properties.onDecrease
        }
        if (properties.onCopy != null) {
            config.onCopy = properties.onCopy
        }
        if (properties.onCut != null) {
            config.onCut = properties.onCut
        }
        if (properties.onPaste != null) {
            config.onPaste = properties.onPaste
        }
        if (properties.onMoveCursorForwardByCharacter != null) {
            config.onMoveCursorForwardByCharacter = properties.onMoveCursorForwardByCharacter
        }
        if (properties.onMoveCursorBackwardByCharacter != null) {
            config.onMoveCursorBackwardByCharacter = properties.onMoveCursorBackwardByCharacter
        }
        if (properties.onSetSelection != null) {
            config.onSetSelection = properties.onSetSelection
        }
        if (properties.onDidGainAccessibilityFocus != null) {
            config.onDidGainAccessibilityFocus = properties.onDidGainAccessibilityFocus
        }
        if (properties.onDidLoseAccessibilityFocus != null) {
            config.onDidLoseAccessibilityFocus = properties.onDidLoseAccessibilityFocus
        }

        newChild.updateWith(
            config = config,
            // As of now CustomPainter does not support multiple tree levels.
            childrenInInversePaintOrder = emptyList()
        )

        with(newChild) {
            rect = newSemantics.rect
            transform = newSemantics.transform
            tags = newSemantics.tags
        }

        return newChild
    }
}
