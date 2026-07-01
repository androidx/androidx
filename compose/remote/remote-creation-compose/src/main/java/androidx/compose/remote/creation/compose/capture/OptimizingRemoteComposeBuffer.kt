/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.state.RemoteFloat

/**
 * A [RemoteComposeBuffer] that optimizes the recorded operations before writing them to the binary
 * output.
 *
 * Instead of writing operations eagerly, this buffer builds an in-memory tree of [TreeOp]s during
 * the composition phase. When the document is serialized (via
 * [OptimizingRemoteComposeWriter.encodeToByteArray]), it executes an optimization pipeline
 * consisting of an elision pass, a flattening pass, and a transform optimization pass, before
 * flushing the optimized tree to the actual binary buffer.
 *
 * ### How Operations are Handled
 * - **Transforms**: Matrix operations (Translate, Scale, Rotate, Skew) are represented as
 *   [TreeOp.Transform] nodes containing [PendingOp]s. During the optimization phase, consecutive
 *   transforms are buffered into a list and optimized together. They are fused (e.g., combining two
 *   scales) and commuted (e.g., pushing translations left past scales) to reduce the total number
 *   of matrix operations written to the binary buffer.
 * - **Regular Draw Calls & State**: Drawing operations (e.g., `drawRect`, `drawPath`) and state
 *   changes (e.g., `addPaint`, `setTheme`) are wrapped in [TreeOp.Draw] nodes. These are appended
 *   directly to the current [TreeOp.Save] scope's children list. They are not buffered or optimized
 *   individually. If a [TreeOp.Save] scope contains no drawing operations, the entire scope (along
 *   with its transforms/clips) is discarded. If it does contain drawing operations but is at the
 *   end of its parent scope, the save/restore boundaries are inlined (elided) while preserving all
 *   the drawing operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class OptimizingRemoteComposeBuffer(apiLevel: Int) : RemoteComposeBuffer(apiLevel) {

    private val rootScope = TreeOp.Save()
    private var currentScope = rootScope

    protected open fun createDelegate(apiLevel: Int): RemoteComposeBuffer {
        return RemoteComposeBuffer(apiLevel)
    }

    protected val delegate: RemoteComposeBuffer by lazy {
        createDelegate(apiLevel).apply { setBuffer(this@OptimizingRemoteComposeBuffer.getBuffer()) }
    }

    override fun setBuffer(buffer: WireBuffer) {
        super.setBuffer(buffer)
        delegate.setBuffer(buffer)
    }

    private fun recordDraw(action: (RemoteComposeBuffer) -> Unit) {
        currentScope.children.add(TreeOp.Draw(action))
        markDrawCall()
    }

    private fun RemoteFloat.cacheKeysMatch(other: RemoteFloat): Boolean {
        return this.cacheKey == other.cacheKey
    }

    override fun reset(apiLevel: Int) {
        super.reset(apiLevel)
        rootScope.children.clear()
        rootScope.hasDrawCalls = false
        currentScope = rootScope
    }

    /**
     * Runs the optimization pipeline on the recorded operations tree and flushes the optimized
     * operations to the underlying binary buffer.
     *
     * This method should be called once when the recording is complete, typically triggered by the
     * writer's serialization method.
     */
    public fun optimizeAndFlush(creationState: RemoteComposeCreationState) {
        if (rootScope.children.isEmpty()) return
        elisionPass(rootScope, false)
        flatten(rootScope)
        optimizeTransforms(rootScope)

        try {
            for (i in 0 until rootScope.children.size) {
                rootScope.children[i].write(delegate, creationState)
            }
        } finally {
            rootScope.children.clear()
            rootScope.hasDrawCalls = false
            currentScope = rootScope
        }
    }

    /**
     * Traverses the tree from right-to-left (reverse post-order) to identify redundant
     * `save`/`restore` blocks.
     *
     * A block is marked as [TreeOp.ElisionMode.INLINE] if it contains draw calls but there are no
     * subsequent draw calls after its restore point (making the state restoration pointless).
     * Blocks with no draw calls at all are marked as [TreeOp.ElisionMode.DISCARD].
     *
     * @param node The root of the subtree to process.
     * @param seenDrawCall Whether a draw call has been seen to the right of this subtree.
     * @return True if a draw call was seen during the traversal of this subtree or to its right.
     */
    private fun elisionPass(node: TreeOp.Save, seenDrawCall: Boolean): Boolean {
        var currentSeenDrawCall = seenDrawCall
        for (i in node.children.size - 1 downTo 0) {
            val child = node.children[i]
            when (child) {
                is TreeOp.Draw -> {
                    currentSeenDrawCall = true
                }
                is TreeOp.Save -> {
                    if (!child.hasDrawCalls) {
                        child.elisionMode = TreeOp.ElisionMode.DISCARD
                    } else if (!currentSeenDrawCall) {
                        child.elisionMode = TreeOp.ElisionMode.INLINE
                        currentSeenDrawCall = elisionPass(child, currentSeenDrawCall)
                    } else {
                        child.elisionMode = TreeOp.ElisionMode.PRESERVE
                        elisionPass(child, false)
                        currentSeenDrawCall = true
                    }
                }
                else -> {}
            }
        }
        return currentSeenDrawCall
    }

    /**
     * Recursively applies the elision decisions made in [elisionPass].
     *
     * Removes [TreeOp.ElisionMode.DISCARD] nodes and inlines the children of
     * [TreeOp.ElisionMode.INLINE] nodes directly into their parent's children list.
     */
    private fun flatten(node: TreeOp.Save) {
        val newChildren =
            buildList(node.children.size) {
                for (i in 0 until node.children.size) {
                    val child = node.children[i]
                    when (child) {
                        is TreeOp.Save -> {
                            flatten(child)
                            when (child.elisionMode) {
                                TreeOp.ElisionMode.DISCARD -> {}
                                TreeOp.ElisionMode.INLINE -> {
                                    addAll(child.children)
                                }
                                TreeOp.ElisionMode.PRESERVE -> {
                                    add(child)
                                }
                            }
                        }
                        else -> {
                            add(child)
                        }
                    }
                }
            }
        node.children.clear()
        node.children.addAll(newChildren)
    }

    /**
     * Recursively optimizes transform operations within each scope of the tree.
     *
     * Within each [TreeOp.Save] node, consecutive [TreeOp.Transform] nodes (separated only by other
     * transforms) are grouped and optimized via [optimizeTransformList]. Non-transform nodes (like
     * drawings or clips) act as barriers.
     */
    private fun optimizeTransforms(node: TreeOp.Save) {
        for (i in 0 until node.children.size) {
            val child = node.children[i]
            if (child is TreeOp.Save) {
                optimizeTransforms(child)
            }
        }

        val pendingTransforms = ArrayList<PendingOp>()

        val newChildren =
            buildList(node.children.size) {
                fun flushTransforms() {
                    if (pendingTransforms.isNotEmpty()) {
                        val optimized = optimizeTransformList(pendingTransforms)
                        for (i in 0 until optimized.size) {
                            add(TreeOp.Transform(optimized[i]))
                        }
                        pendingTransforms.clear()
                    }
                }

                for (i in 0 until node.children.size) {
                    val child = node.children[i]
                    when (child) {
                        is TreeOp.Transform -> {
                            pendingTransforms.add(child.op)
                        }
                        else -> {
                            flushTransforms()
                            add(child)
                        }
                    }
                }
                flushTransforms()
            }
        node.children.clear()
        node.children.addAll(newChildren)
    }

    /**
     * Fuses and commutes a list of consecutive [PendingOp] transforms.
     *
     * Consecutive translations, scales, and rotations are fused. Translations are commuted left
     * past scales/rotates/skews when possible to enable further fusing.
     */
    private fun optimizeTransformList(ops: List<PendingOp>): List<PendingOp> =
        buildList(ops.size) {
            for (i in 0 until ops.size) {
                val op = ops[i]
                when (op) {
                    is PendingOp.Translate -> pushTranslate(op.dx, op.dy)
                    is PendingOp.Scale -> pushScale(op.sx, op.sy, op.px, op.py)
                    is PendingOp.Rotate -> pushRotate(op.angle, op.px, op.py)
                    is PendingOp.Skew -> pushSkew(op.sx, op.sy)
                }
            }
        }

    override fun addMatrixTranslate(dx: Float, dy: Float) {
        currentScope.children.add(
            TreeOp.Transform(PendingOp.Translate(RemoteFloat(dx), RemoteFloat(dy)))
        )
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float) {
        currentScope.children.add(
            TreeOp.Transform(
                PendingOp.Scale(
                    RemoteFloat(scaleX),
                    RemoteFloat(scaleY),
                    RemoteFloat(Float.NaN),
                    RemoteFloat(Float.NaN),
                )
            )
        )
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        currentScope.children.add(
            TreeOp.Transform(
                PendingOp.Scale(
                    RemoteFloat(scaleX),
                    RemoteFloat(scaleY),
                    RemoteFloat(centerX),
                    RemoteFloat(centerY),
                )
            )
        )
    }

    override fun addMatrixRotate(angle: Float, centerX: Float, centerY: Float) {
        currentScope.children.add(
            TreeOp.Transform(
                PendingOp.Rotate(RemoteFloat(angle), RemoteFloat(centerX), RemoteFloat(centerY))
            )
        )
    }

    override fun addMatrixSkew(skewX: Float, skewY: Float) {
        currentScope.children.add(
            TreeOp.Transform(PendingOp.Skew(RemoteFloat(skewX), RemoteFloat(skewY)))
        )
    }

    override fun addMatrixSave() {
        val newScope = TreeOp.Save(currentScope)
        currentScope.children.add(newScope)
        currentScope = newScope
    }

    override fun addMatrixRestore() {
        currentScope = currentScope.parent ?: currentScope
    }

    override fun addClipRect(left: Float, top: Float, right: Float, bottom: Float) {
        currentScope.children.add(TreeOp.ClipRect(left, top, right, bottom))
    }

    override fun addClipPath(pathId: Int) {
        currentScope.children.add(TreeOp.ClipPath(pathId))
    }

    private fun markDrawCall() {
        var s: TreeOp.Save? = currentScope
        while (s != null) {
            s.hasDrawCalls = true
            s = s.parent
        }
    }

    /**
     * Pushes a translation operation into the pending operations list, attempting to commute it
     * leftwards past other transformations (Scale, Rotate, Skew) to enable further fusing with
     * existing translations.
     *
     * As the translation commutes past other operations, its offsets are adjusted:
     * - Past [PendingOp.Scale]: Offsets are scaled by the scale factors.
     * - Past [PendingOp.Rotate]: Offsets are rotated by the rotation angle.
     * - Past [PendingOp.Skew]: Offsets are skewed by the skew factors.
     *
     * Commutation is only possible if the encountered transformation has constant values (not
     * dynamic/animated expressions), allowing the adjustment to be computed at creation time. If
     * commutation is blocked, the translation is inserted at the current position.
     */
    private fun MutableList<PendingOp>.pushTranslate(dx: RemoteFloat, dy: RemoteFloat) {
        var currDx = dx
        var currDy = dy
        var i = size - 1
        // Travel backwards through the list to find a Translate to fuse with,
        // or commute past Scale/Rotate/Skew.
        while (i >= 0) {
            val op = this[i]
            when (op) {
                is PendingOp.Translate -> {
                    // Found another Translate, fuse them by adding offsets.
                    this[i] = PendingOp.Translate(op.dx + currDx, op.dy + currDy)
                    return
                }
                is PendingOp.Scale -> {
                    // Commute past Scale: we must scale the translation offsets.
                    // This is only possible if the scale factors are constants.
                    val sxVal = op.sx.constantValueOrNull
                    val syVal = op.sy.constantValueOrNull
                    if (sxVal == null || syVal == null) {
                        break // Cannot commute past dynamic scale, stop here.
                    }
                    currDx = currDx * op.sx
                    currDy = currDy * op.sy
                    i--
                }
                is PendingOp.Rotate -> {
                    // Commute past Rotate: we must rotate the translation offsets.
                    // This is only possible if the rotation angle is constant.
                    val angleVal = op.angle.constantValueOrNull
                    if (angleVal == null) {
                        break // Cannot commute past dynamic rotation, stop here.
                    }
                    val rad = Math.toRadians(angleVal.toDouble())
                    val cos = Math.cos(rad).toFloat()
                    val sin = Math.sin(rad).toFloat()
                    val rx = currDx * cos - currDy * sin
                    val ry = currDx * sin + currDy * cos
                    currDx = rx
                    currDy = ry
                    i--
                }
                is PendingOp.Skew -> {
                    // Commute past Skew: we must skew the translation offsets.
                    // This is only possible if the skew factors are constants.
                    val sxVal = op.sx.constantValueOrNull
                    val syVal = op.sy.constantValueOrNull
                    if (sxVal == null || syVal == null) {
                        break // Cannot commute past dynamic skew, stop here.
                    }
                    val rx = currDx + currDy * sxVal
                    val ry = currDx * syVal + currDy
                    currDx = rx
                    currDy = ry
                    i--
                }
            }
        }
        // Insert the accumulated translation at the position we stopped.
        this.add(i + 1, PendingOp.Translate(currDx, currDy))
    }

    /**
     * Pushes a scale operation into the pending operations list.
     *
     * If the immediate preceding operation is also a [PendingOp.Scale] and shares the exact same
     * pivot point ([px], [py]), the two scales are fused into a single scale by multiplying their
     * scale factors. Otherwise, the scale is appended.
     */
    private fun MutableList<PendingOp>.pushScale(
        sx: RemoteFloat,
        sy: RemoteFloat,
        px: RemoteFloat,
        py: RemoteFloat,
    ) {
        if (isNotEmpty()) {
            val last = last()
            // If the last op is also a Scale with the same pivot, we can fuse them.
            if (
                last is PendingOp.Scale && last.px.cacheKeysMatch(px) && last.py.cacheKeysMatch(py)
            ) {
                // Fuse by multiplying the scale factors.
                this[size - 1] = PendingOp.Scale(last.sx * sx, last.sy * sy, px, py)
                return
            }
        }
        // Otherwise, append the new scale.
        add(PendingOp.Scale(sx, sy, px, py))
    }

    /**
     * Pushes a rotation operation into the pending operations list.
     *
     * If the immediate preceding operation is also a [PendingOp.Rotate] and shares the exact same
     * pivot point ([px], [py]), the two rotations are fused into a single rotation by adding their
     * angles. Otherwise, the rotation is appended.
     */
    private fun MutableList<PendingOp>.pushRotate(
        angle: RemoteFloat,
        px: RemoteFloat,
        py: RemoteFloat,
    ) {
        if (isNotEmpty()) {
            val last = last()
            // If the last op is also a Rotate with the same pivot, we can fuse them.
            if (
                last is PendingOp.Rotate && last.px.cacheKeysMatch(px) && last.py.cacheKeysMatch(py)
            ) {
                // Fuse by adding the rotation angles.
                this[size - 1] = PendingOp.Rotate(last.angle + angle, px, py)
                return
            }
        }
        // Otherwise, append the new rotation.
        add(PendingOp.Rotate(angle, px, py))
    }

    private fun MutableList<PendingOp>.pushSkew(sx: RemoteFloat, sy: RemoteFloat) {
        add(PendingOp.Skew(sx, sy))
    }

    // Overridden drawing and state methods
    override fun addPaint(paint: PaintBundle): Unit = recordDraw { it.addPaint(paint) }

    override fun drawBitmap(
        imageId: Int,
        imageWidth: Int,
        imageHeight: Int,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
        contentDescriptionId: Int,
    ): Unit = recordDraw {
        it.drawBitmap(
            imageId,
            imageWidth,
            imageHeight,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
            contentDescriptionId,
        )
    }

    override fun addDrawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ): Unit = recordDraw { it.addDrawArc(left, top, right, bottom, startAngle, sweepAngle) }

    override fun addDrawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ): Unit = recordDraw { it.addDrawSector(left, top, right, bottom, startAngle, sweepAngle) }

    override fun addDrawBitmap(
        imageId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        contentDescriptionId: Int,
    ): Unit = recordDraw {
        it.addDrawBitmap(imageId, left, top, right, bottom, contentDescriptionId)
    }

    override fun drawScaledBitmap(
        imageId: Int,
        srcLeft: Float,
        srcTop: Float,
        srcRight: Float,
        srcBottom: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        scaleType: Int,
        scaleFactor: Float,
        contentDescriptionId: Int,
    ): Unit = recordDraw {
        it.drawScaledBitmap(
            imageId,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
            scaleType,
            scaleFactor,
            contentDescriptionId,
        )
    }

    override fun addDrawCircle(centerX: Float, centerY: Float, radius: Float): Unit = recordDraw {
        it.addDrawCircle(centerX, centerY, radius)
    }

    override fun addDrawLine(x1: Float, y1: Float, x2: Float, y2: Float): Unit = recordDraw {
        it.addDrawLine(x1, y1, x2, y2)
    }

    override fun addDrawOval(left: Float, top: Float, right: Float, bottom: Float): Unit =
        recordDraw {
            it.addDrawOval(left, top, right, bottom)
        }

    override fun addDrawPath(pathId: Int): Unit = recordDraw { it.addDrawPath(pathId) }

    override fun addDrawRect(left: Float, top: Float, right: Float, bottom: Float): Unit =
        recordDraw {
            it.addDrawRect(left, top, right, bottom)
        }

    override fun addDrawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ): Unit = recordDraw { it.addDrawRoundRect(left, top, right, bottom, radiusX, radiusY) }

    override fun addDrawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float): Unit =
        recordDraw {
            it.addDrawTextOnPath(textId, pathId, hOffset, vOffset)
        }

    override fun addDrawTextOnCircle(
        textId: Int,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        warpRadiusOffset: Float,
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
    ): Unit = recordDraw {
        it.addDrawTextOnCircle(
            textId,
            centerX,
            centerY,
            radius,
            startAngle,
            warpRadiusOffset,
            alignment,
            placement,
        )
    }

    override fun addDrawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ): Unit = recordDraw {
        it.addDrawTextRun(textId, start, end, contextStart, contextEnd, x, y, rtl)
    }

    override fun addDrawBitmapFontTextRun(
        textId: Int,
        bitmapFontId: Int,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        glyphSpacing: Float,
    ): Unit = recordDraw {
        it.addDrawBitmapFontTextRun(textId, bitmapFontId, start, end, x, y, glyphSpacing)
    }

    override fun addDrawBitmapFontTextRunOnPath(
        textId: Int,
        bitmapFontId: Int,
        pathId: Int,
        start: Int,
        end: Int,
        yAdj: Float,
        glyphSpacing: Float,
    ): Unit = recordDraw {
        it.addDrawBitmapFontTextRunOnPath(
            textId,
            bitmapFontId,
            pathId,
            start,
            end,
            yAdj,
            glyphSpacing,
        )
    }

    override fun drawBitmapTextAnchored(
        textId: Int,
        bitmapFontId: Int,
        start: Float,
        end: Float,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        glyphSpacing: Float,
    ): Unit = recordDraw {
        it.drawBitmapTextAnchored(textId, bitmapFontId, start, end, x, y, panX, panY, glyphSpacing)
    }

    override fun drawTextAnchored(
        textId: Int,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: Int,
    ): Unit = recordDraw { it.drawTextAnchored(textId, x, y, panX, panY, flags) }

    override fun addDrawTweenPath(
        path1Id: Int,
        path2Id: Int,
        tween: Float,
        start: Float,
        stop: Float,
    ): Unit = recordDraw { it.addDrawTweenPath(path1Id, path2Id, tween, start, stop) }

    override fun drawComponentContent(): Unit = recordDraw { it.drawComponentContent() }

    override fun drawOnBitmap(imageId: Int, mode: Int, color: Int): Unit = recordDraw {
        it.drawOnBitmap(imageId, mode, color)
    }

    override fun addDrawContentOperation(): Unit = recordDraw { it.addDrawContentOperation() }

    override fun setTheme(theme: Int): Unit = recordDraw { it.setTheme(theme) }

    override fun addClickArea(
        id: Int,
        metadataId: Int,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        flags: Int,
    ): Unit = recordDraw { it.addClickArea(id, metadataId, x1, y1, x2, y2, flags) }

    override fun addComponentStart(type: Int, id: Int): Unit = recordDraw {
        it.addComponentStart(type, id)
    }

    override fun addComponentStart(type: Int): Unit = recordDraw { it.addComponentStart(type) }

    override fun addContainerEnd(): Unit = recordDraw { it.addContainerEnd() }

    override fun addModifierScroll(direction: Int, max: Float): Unit = recordDraw {
        it.addModifierScroll(direction, max)
    }

    override fun addModifierBackground(color: Int, shape: Int): Unit = recordDraw {
        it.addModifierBackground(color, shape)
    }

    override fun addDynamicModifierBackground(colorId: Int, shape: Int): Unit = recordDraw {
        it.addDynamicModifierBackground(colorId, shape)
    }

    override fun addModifierBackground(r: Float, g: Float, b: Float, a: Float, shape: Int): Unit =
        recordDraw {
            it.addModifierBackground(r, g, b, a, shape)
        }

    override fun addModifierAlignBy(line: Float): Unit = recordDraw { it.addModifierAlignBy(line) }

    override fun addModifierBorder(
        borderWidth: Float,
        borderRoundedCorner: Float,
        color: Int,
        shape: Int,
    ): Unit = recordDraw { it.addModifierBorder(borderWidth, borderRoundedCorner, color, shape) }

    override fun addModifierBorder(
        borderWidth: Float,
        borderRoundedCorner: Float,
        colorId: Int,
        shape: Int,
        dummy: Boolean,
    ): Unit = recordDraw {
        it.addModifierBorder(borderWidth, borderRoundedCorner, colorId, shape, dummy)
    }

    override fun addModifierDynamicBorder(
        borderWidth: Float,
        borderRoundedCorner: Float,
        colorId: Int,
        shape: Int,
    ): Unit = recordDraw {
        it.addModifierDynamicBorder(borderWidth, borderRoundedCorner, colorId, shape)
    }

    override fun addModifierDynamicBorder(
        borderWidth: Float,
        borderRoundedCorner: Float,
        colorId: Int,
        shape: Int,
        dummy: Boolean,
    ): Unit = recordDraw {
        it.addModifierDynamicBorder(borderWidth, borderRoundedCorner, colorId, shape, dummy)
    }

    override fun addModifierPadding(left: Float, top: Float, right: Float, bottom: Float): Unit =
        recordDraw {
            it.addModifierPadding(left, top, right, bottom)
        }

    override fun addModifierOffset(x: Float, y: Float): Unit = recordDraw {
        it.addModifierOffset(x, y)
    }

    override fun addModifierZIndex(value: Float): Unit = recordDraw { it.addModifierZIndex(value) }

    override fun addModifierRipple(): Unit = recordDraw { it.addModifierRipple() }

    override fun addModifierMarquee(
        iterations: Int,
        animationMode: Int,
        repeatDelayMillis: Float,
        initialDelayMillis: Float,
        spacing: Float,
        velocity: Float,
    ): Unit = recordDraw {
        it.addModifierMarquee(
            iterations,
            animationMode,
            repeatDelayMillis,
            initialDelayMillis,
            spacing,
            velocity,
        )
    }

    override fun addModifierGraphicsLayer(attributes: java.util.HashMap<Int, Any>): Unit =
        recordDraw {
            it.addModifierGraphicsLayer(attributes)
        }
}

/**
 * Represents a pending matrix transformation operation that can potentially be optimized.
 *
 * Unlike [TreeOp]s which form the main hierarchical tree, [PendingOp]s are flat, intermediate
 * representations of transforms. They are accumulated in lists during the transform optimization
 * pass, where they are fused and commuted before being converted back to [TreeOp.Transform] nodes
 * for final flushing.
 */
internal sealed class PendingOp {
    /** Flushes this transformation to the [writer]. */
    abstract fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState)

    /** Represents a translation transformation. */
    class Translate(val dx: RemoteFloat, val dy: RemoteFloat) : PendingOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixTranslate(dx, dy, creationState)
        }
    }

    /** Represents a scale transformation. */
    class Scale(
        val sx: RemoteFloat,
        val sy: RemoteFloat,
        val px: RemoteFloat,
        val py: RemoteFloat,
    ) : PendingOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixScale(sx, sy, px, py, creationState)
        }
    }

    /** Represents a rotation transformation. */
    class Rotate(val angle: RemoteFloat, val px: RemoteFloat, val py: RemoteFloat) : PendingOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixRotate(angle, px, py, creationState)
        }
    }

    /** Represents a skew transformation. */
    class Skew(val sx: RemoteFloat, val sy: RemoteFloat) : PendingOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            writer.addMatrixSkew(sx, sy, creationState)
        }
    }
}

/**
 * Represents an operation in the in-memory drawing tree built during the composition phase.
 *
 * These operations are optimized (elided, flattened, fused) before being flushed to the actual
 * [RemoteComposeBuffer] during serialization.
 */
internal sealed class TreeOp {
    /** Flushes this operation (and its children, if any) to the [writer]. */
    abstract fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState)

    /**
     * Represents a save/restore group (corresponding to [RemoteComposeBuffer.addMatrixSave] and
     * [RemoteComposeBuffer.addMatrixRestore]).
     *
     * @property parent The parent [Save] scope, or null if this is the root scope.
     * @property children The list of child operations inside this save/restore block.
     */
    class Save(val parent: Save? = null, val children: MutableList<TreeOp> = ArrayList()) :
        TreeOp() {
        /**
         * Indicates whether this scope or any of its descendants contain actual drawing calls. Used
         * during the elision pass to discard empty scopes.
         */
        var hasDrawCalls = false

        /** The elision strategy decided for this scope during the elision pass. */
        var elisionMode = ElisionMode.PRESERVE

        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) {
            when (elisionMode) {
                ElisionMode.DISCARD -> {}
                ElisionMode.INLINE -> {
                    for (i in 0 until children.size) {
                        children[i].write(writer, creationState)
                    }
                }
                ElisionMode.PRESERVE -> {
                    writer.addMatrixSave()
                    for (i in 0 until children.size) {
                        children[i].write(writer, creationState)
                    }
                    writer.addMatrixRestore()
                }
            }
        }
    }

    /** The strategy for rendering a [Save] node during flush. */
    enum class ElisionMode {
        /**
         * Keep the save/restore bounds and write [RemoteComposeBuffer.addMatrixSave] and
         * [RemoteComposeBuffer.addMatrixRestore].
         */
        PRESERVE,

        /**
         * Discard the save/restore bounds but write all the children. This inlines the children
         * into the parent scope.
         */
        INLINE,

        /** Discard the save/restore block and all of its children. */
        DISCARD,
    }

    /**
     * Represents a matrix transformation operation (Translate, Scale, Rotate, Skew).
     *
     * @property op The underlying [PendingOp] transformation.
     */
    class Transform(val op: PendingOp) : TreeOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) =
            op.write(writer, creationState)
    }

    /** Represents a rectangular clipping operation. */
    class ClipRect(val left: Float, val top: Float, val right: Float, val bottom: Float) :
        TreeOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) =
            writer.addClipRect(left, top, right, bottom)
    }

    /**
     * Represents a path clipping operation.
     *
     * @property pathId The ID of the path to clip to.
     */
    class ClipPath(val pathId: Int) : TreeOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) =
            writer.addClipPath(pathId)
    }

    /**
     * Represents an actual drawing or state-setting operation (e.g., drawRect, addPaint).
     *
     * @property action The lambda that executes the drawing operation on the receiver buffer.
     */
    class Draw(val action: (RemoteComposeBuffer) -> Unit) : TreeOp() {
        override fun write(writer: RemoteComposeBuffer, creationState: RemoteComposeCreationState) =
            action(writer)
    }
}

private fun RemoteComposeBuffer.addMatrixTranslate(
    dx: RemoteFloat,
    dy: RemoteFloat,
    state: RemoteComposeCreationState,
) {
    this.addMatrixTranslate(
        dx.getFloatIdForCreationState(state),
        dy.getFloatIdForCreationState(state),
    )
}

private fun RemoteComposeBuffer.addMatrixScale(
    sx: RemoteFloat,
    sy: RemoteFloat,
    px: RemoteFloat,
    py: RemoteFloat,
    state: RemoteComposeCreationState,
) {
    this.addMatrixScale(
        sx.getFloatIdForCreationState(state),
        sy.getFloatIdForCreationState(state),
        px.getFloatIdForCreationState(state),
        py.getFloatIdForCreationState(state),
    )
}

private fun RemoteComposeBuffer.addMatrixRotate(
    angle: RemoteFloat,
    px: RemoteFloat,
    py: RemoteFloat,
    state: RemoteComposeCreationState,
) {
    this.addMatrixRotate(
        angle.getFloatIdForCreationState(state),
        px.getFloatIdForCreationState(state),
        py.getFloatIdForCreationState(state),
    )
}

private fun RemoteComposeBuffer.addMatrixSkew(
    sx: RemoteFloat,
    sy: RemoteFloat,
    state: RemoteComposeCreationState,
) {
    this.addMatrixSkew(sx.getFloatIdForCreationState(state), sy.getFloatIdForCreationState(state))
}
