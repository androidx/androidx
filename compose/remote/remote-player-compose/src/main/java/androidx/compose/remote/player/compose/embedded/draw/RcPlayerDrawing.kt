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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.draw

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DataMapLookup
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawBitmap
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapScaled
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawContent
import androidx.compose.remote.core.operations.DrawLine
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.DrawSector
import androidx.compose.remote.core.operations.DrawText
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.HapticFeedback
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.MatrixRestore
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.MatrixSave
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathAppend
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathExpression
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.PlaySound
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.UpdateDynamicFloatList
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.ImpulseProcess
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.remote.player.compose.embedded.doc.applyTrackingPathChange
import androidx.compose.remote.player.compose.embedded.doc.withOpCountReset
import androidx.compose.remote.player.compose.embedded.resources.resolveImageAttributeDimensions
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.util.fastForEach
import androidx.tracing.trace

/** Dereference a PaintOperation image/path id (mirrors PaintOperation.getId's PTR_DEREFERENCE). */
internal fun derefId(rawId: Int, context: RemoteContext): Int =
    if ((rawId and PaintOperation.PTR_DEREFERENCE) != 0) {
        context.mRemoteComposeState.getInteger(rawId and PaintOperation.VALUE_MASK)
    } else {
        rawId and PaintOperation.VALUE_MASK
    }

/** Backstop on [LoopOperation] iterations so a malformed bound can't hang the draw thread. */
private const val MAX_LOOP_ITERATIONS = 100_000

/**
 * Encodes the iteration ordinals of the enclosing [LoopOperation]s into one `loopScope` key, so an
 * impulse keeps separate fire state per iteration. Unique for up to three nested loops.
 */
private const val LOOP_SCOPE_RADIX = MAX_LOOP_ITERATIONS + 1L

internal fun resolveFloat(value: Float, fallback: Float, context: RemoteContext): Float {
    // A NaN-encoded value is a variable reference. [context] is the draw read context — normally
    // the GraphContext, which resolves time ids from the Compose frame clock and computed ids
    // through their derivedStateOf (so a time/variable-driven value re-runs this draw when it
    // changes), and a plain leaf id through the shared snapshot store. There is no separate
    // draw-path variable map.
    return MaybeVariableFloat(value).resolveOr(fallback, context)
}

internal fun DrawScope.executeOperations(
    operations: List<Operation>,
    remoteContext: RemoteContext,
    textMeasurer: TextMeasurer,
    paintState: ComposeLocalPaint = ComposeLocalPaint(),
    onDrawContent: () -> Unit = {},
    graph: GraphContext? = null,
    offscreenState: OffscreenDrawState = OffscreenDrawState(),
    contentLayer: GraphicsLayer? = null,
    contentOrigin: Offset = Offset.Zero,
): Unit =
    trace("RcPlayer:executeOperations") {
        remoteContext.withOpCountReset {
            graph?.clearLastOpCount()
            executeOperationsInPass(
                operations = operations,
                remoteContext = remoteContext,
                textMeasurer = textMeasurer,
                paintState = paintState,
                onDrawContent = onDrawContent,
                graph = graph,
                offscreenState = offscreenState,
                contentLayer = contentLayer,
                contentOrigin = contentOrigin,
            )
        }
    }

private fun DrawScope.executeOperationsInPass(
    operations: List<Operation>,
    remoteContext: RemoteContext,
    textMeasurer: TextMeasurer,
    paintState: ComposeLocalPaint = ComposeLocalPaint(),
    onDrawContent: () -> Unit = {},
    graph: GraphContext? = null,
    offscreenState: OffscreenDrawState,
    contentLayer: GraphicsLayer? = null,
    contentOrigin: Offset = Offset.Zero,
    loopScope: Long = 0L,
) {
    // Reads route through the GraphContext when present: it resolves time ids from the Compose
    // frame clock and computed ids via per-pass DAG memoization over reactive leaf states, and a
    // leaf id falls through to the same shared snapshot store. So reading a time/variable-driven
    // value here registers this draw as an observer of the relevant Compose state — the draw
    // re-runs when that state changes, with no per-frame applyOperations refreshing the store.
    // WRITES (op.apply, overrideFloat, loadFloat, bitmap decode) must NOT go through the graph — it
    // suppresses writes during evaluation — so they stay on `remoteContext` (the real store).
    // GraphContext shares that store, so leaf reads are identical either way.
    val read: RemoteContext = graph ?: remoteContext
    var paintEvalContext: GraphPaintContext? = null
    fun evalPaintContext(): GraphPaintContext =
        (paintEvalContext
                ?: GraphPaintContext(remoteContext, read, textMeasurer).also {
                    paintEvalContext = it
                })
            .also {
                it.paintState = paintState
                it.textMeasurer = textMeasurer
            }
    var canvasLevel = 0
    val initialOffscreenDepth = offscreenState.depth
    operations.fastForEach { op ->
        remoteContext.incrementOpCount()
        if (
            op is VariableSupport &&
                op !is TouchExpression &&
                op !is LoopOperation &&
                op !is ConditionalOperations &&
                // Impulses resolve their own window below; their children update only when run.
                op !is ImpulseOperation &&
                op !is ImpulseProcess
        ) {
            op.updateVariables(read)
        }
        when (op) {
            is CanvasLayout -> {
                drawContext.canvas.save()
                canvasLevel++
            }
            is ContainerEnd -> {
                if (canvasLevel > 0) {
                    drawContext.canvas.restore()
                    canvasLevel--
                }
            }
            is PaintData -> {
                // Resolved through read: use the GraphContext for reactive colors; its
                // writeContext handles shader/texture bitmap decode.
                updatePaintFromBundle(op.mPaintData, paintState, read)
            }
            is FloatExpression -> {
                val snapshotState = remoteContext.mRemoteComposeState as? SnapshotRemoteComposeState
                if (
                    graph?.isTouchExpression(op.mId) != true &&
                        snapshotState?.isFloatOverridden(op.mId) != true
                ) {
                    // Evaluate reactively (time/variables via the graph); write the result to the
                    // real store so later ops/draws in this stream that read the id by store see
                    // it.
                    val v = op.evaluate(read)
                    remoteContext.loadFloat(op.mId, v)
                }
            }
            is ColorConstant -> op.apply(remoteContext)
            is ColorExpression -> op.apply(remoteContext)
            is ColorAttribute -> {
                remoteContext.loadColor(op.mColorId, read.getColor(op.mColorId))
                op.paint(evalPaintContext())
            }
            is ImageAttribute -> {
                resolveImageAttributeDimensions(graph, remoteContext, derefId(op.imageId, read))
                op.paint(evalPaintContext())
            }
            is TextMeasure -> {
                graph?.textMeasurer = textMeasurer
                graph?.setTextMeasurePaint(op.mId, paintState.copy())
                op.paint(evalPaintContext())
            }
            is TextAttribute -> {
                // Text bounds depend on the paint at this point of the stream.
                graph?.textMeasurer = textMeasurer
                graph?.setTextMeasurePaint(op.mId, paintState.copy())
                op.paint(evalPaintContext())
            }
            is TimeAttribute,
            is BitmapTextMeasure -> op.paint(evalPaintContext())
            // Data operations in a draw stream run each time the stream does, as in the View
            // player's CanvasOperations, so they follow inputs that change after setup.
            is UpdateDynamicFloatList,
            is DataMapLookup -> op.apply(remoteContext)
            is NamedVariable -> op.apply(remoteContext)
            is BitmapData -> op.apply(remoteContext)
            is ShaderData -> {
                op.enable(
                    (remoteContext.mRemoteComposeState as? SnapshotRemoteComposeState)
                        ?.trustedContent == true
                )
                op.apply(remoteContext)
            }
            // Re-seeds on every paint, as in the View player; wrap it in an impulse to seed once.
            is ParticlesCreate -> drawParticlesCreate(op, evalPaintContext(), graph)
            is ParticlesLoop -> {
                // Particle system: bridged to the core (View player) implementation. Needs the
                // graph for seed state + frame-clock observation; without it skip.
                if (graph != null) drawParticles(op, remoteContext, paintState, graph)
            }
            is ParticlesCompare -> {
                // Particle interaction pass: bridged to the core implementation like
                // ParticlesLoop.
                if (graph != null) drawParticlesCompare(op, remoteContext, paintState, graph)
            }
            is ImpulseOperation -> {
                // Impulse: the body only runs inside the [startAt, startAt + duration] window
                // (seconds of animation time). The first pass in a window runs the setup list once
                // (e.g. a haptic, sound or particle reset); later passes in the window run the
                // trailing ImpulseProcess. Mirrors ImpulseOperation.paint. Reading the animation
                // time through the graph re-runs this draw on each frame tick and keeps the frame
                // loop ticking. Once the window has closed the time is only peeked, so the loop can
                // idle; a change of startAt or duration still re-runs this draw, and a window that
                // reopens reads the time again, which wakes the loop.
                val startAt = resolveFloat(op.startAt, op.startAt, read)
                val duration = resolveFloat(op.duration, op.duration, read)
                val peeked = graph?.timeState?.peekAnimationTime()
                val now =
                    if (peeked != null && peeked > startAt + duration) {
                        peeked
                    } else {
                        read.getFloat(RemoteContext.ID_ANIMATION_TIME)
                    }
                // registerListening moves a trailing ImpulseProcess out of the list; until it has
                // run, the process is still the last child.
                val process = op.process ?: (op.list.lastOrNull() as? ImpulseProcess)
                if (now >= startAt && now <= startAt + duration) {
                    // Fire state is per loop iteration, so every iteration of an impulse inside a
                    // canvas loop runs its own initial pass. Unlike core, moving startAt while the
                    // window is open (a re-trigger, or a start rescheduled ahead) re-runs the
                    // initial pass, as long as the previous start held across a frame; a start that
                    // follows the clock keeps core's "initial pass once, then process" behaviour
                    // instead of re-running the setup on every frame.
                    val state = graph?.impulseWindow(op, loopScope)
                    val initialPass =
                        when {
                            graph == null || state == null -> true
                            startAt != state.startAt ->
                                state.isStartStable.also { if (!it) state.restart(startAt, now) }
                            else -> {
                                state.seen(now)
                                false
                            }
                        }
                    if (initialPass) {
                        graph?.openImpulseWindow(op, loopScope, startAt, now)
                        executeOperationsInPass(
                            if (process != null && op.list.lastOrNull() === process) {
                                op.list.subList(0, op.list.size - 1)
                            } else {
                                op.list
                            },
                            remoteContext,
                            textMeasurer,
                            paintState,
                            onDrawContent,
                            graph,
                            offscreenState,
                            contentLayer,
                            contentOrigin,
                            loopScope = loopScope,
                        )
                    } else if (process != null) {
                        executeOperationsInPass(
                            process.list,
                            remoteContext,
                            textMeasurer,
                            paintState,
                            onDrawContent,
                            graph,
                            offscreenState,
                            contentLayer,
                            contentOrigin,
                            loopScope = loopScope,
                        )
                    }
                } else {
                    // Window closed or not yet open: the next window starts with a fresh initial
                    // pass. Core only resets once the window has closed, so a start rescheduled
                    // ahead while its window was open would skip the setup when it reopens.
                    graph?.clearImpulseWindow(op, loopScope)
                }
            }
            // Only runs through its enclosing ImpulseOperation.
            is ImpulseProcess -> {}
            is ConditionalOperations -> {
                // Conditional draw: resolve the two operands, compare per type, and draw the child
                // ops only when the condition holds. Mirrors ConditionalOperations.paint.
                val a = resolveFloat(op.varA, op.varA, read)
                val b = resolveFloat(op.varB, op.varB, read)
                val run =
                    when (op.type) {
                        ConditionalOperations.TYPE_EQ -> a == b
                        ConditionalOperations.TYPE_NEQ -> a != b
                        ConditionalOperations.TYPE_LT -> a < b
                        ConditionalOperations.TYPE_LTE -> a <= b
                        ConditionalOperations.TYPE_GT -> a > b
                        else -> a >= b // TYPE_GTE
                    }
                if (run)
                    executeOperationsInPass(
                        op.list,
                        remoteContext,
                        textMeasurer,
                        paintState,
                        onDrawContent,
                        graph,
                        offscreenState,
                        contentLayer,
                        contentOrigin,
                        loopScope = loopScope,
                    )
            }
            is LoopOperation -> {
                // Loop: resolve from/until/step (NaN-id bounds → variables), then run the child
                // list once per iteration, loading the index variable each pass. Mirrors
                // LoopOperation.paint, with a hard iteration cap as an infinite-loop backstop.
                // LoopOperation.read accepts a negative step counting down from `from` to an
                // exclusive `until` below it, so a countdown runs while the index is above `until`.
                // NaN or zero steps and bounds that run away from `until` draw nothing.
                val from = resolveFloat(op.from, op.from, read)
                val until = resolveFloat(op.until, op.until, read)
                val step = resolveFloat(op.step, op.step, read)
                val indexId = op.indexVariableId
                val ascending = step > 0f
                if (
                    !from.isNaN() &&
                        !until.isNaN() &&
                        !step.isNaN() &&
                        step != 0f &&
                        (if (ascending) from < until else from > until)
                ) {
                    var i = from
                    var guard = 0
                    while (
                        (if (ascending) i < until else i > until) && guard < MAX_LOOP_ITERATIONS
                    ) {
                        remoteContext.incrementOpCount()
                        if (indexId != 0) remoteContext.loadFloat(indexId, i)
                        executeOperationsInPass(
                            op.list,
                            remoteContext,
                            textMeasurer,
                            paintState,
                            onDrawContent,
                            graph,
                            offscreenState,
                            contentLayer,
                            contentOrigin,
                            // Iteration path for per-iteration impulse fire state.
                            loopScope = loopScope * LOOP_SCOPE_RADIX + guard + 1,
                        )
                        i += step
                        guard++
                    }
                }
            }
            is FloatFunctionCall -> {
                // Invoke a defined float function: load the resolved argument values
                // (updateVariables ran above) into the function's parameter variables, then run its
                // body, which writes its outputs to the context for downstream ops to read. Mirrors
                // FloatFunctionCall.paint. FloatFunctionDefine itself has no draw effect, so it
                // needs no case. A call with no arguments has null args when read from the wire,
                // but still runs the body.
                val fn = op.function
                if (fn != null) {
                    val outArgs = op.outArgs
                    if (outArgs != null) {
                        val argIds = fn.args
                        for (j in outArgs.indices) {
                            if (j < argIds.size) remoteContext.loadFloat(argIds[j], outArgs[j])
                        }
                    }
                    executeOperationsInPass(
                        fn.list,
                        remoteContext,
                        textMeasurer,
                        paintState,
                        onDrawContent,
                        graph,
                        offscreenState,
                        contentLayer,
                        contentOrigin,
                        loopScope = loopScope,
                    )
                }
            }
            is DrawCircle -> drawCircleOp(op, paintState, read)
            is DrawRect -> drawRectOp(op, paintState, read)
            is DrawLine -> drawLineOp(op, paintState, read)
            is DrawOval -> drawOvalOp(op, paintState, read)
            is DrawRoundRect -> drawRoundRectOp(op, paintState, read)
            is DrawSector -> drawSectorOp(op, paintState, read)
            is DrawArc -> drawArcOp(op, paintState, read)
            is DrawBitmap -> drawBitmapOp(op, paintState, graph, remoteContext, read)
            is DrawPath -> drawPathOp(op, paintState, remoteContext, read)
            is MatrixSave -> drawContext.canvas.save()
            is MatrixRestore -> drawContext.canvas.restore()
            is MatrixTranslate -> applyMatrixTranslate(op, read)
            is MatrixScale -> applyMatrixScale(op, read)
            is MatrixRotate -> applyMatrixRotate(op, read)
            is MatrixSkew -> applyMatrixSkew(op, read)
            is ClipRect -> applyClipRect(op, read)
            is ClipPath -> applyClipPath(op, remoteContext, read)
            is DrawText -> drawTextOp(op, paintState, read, textMeasurer)
            is DrawTextOnPath -> drawTextOnPathOp(op, paintState, remoteContext, read)
            is DrawTextAnchored ->
                drawTextAnchoredOp(op, paintState, read, textMeasurer) { evalPaintContext() }
            is DrawBitmapScaled -> drawBitmapScaledOp(op, paintState, graph, remoteContext, read)
            is DrawBitmapInt -> drawBitmapIntOp(op, paintState, graph, remoteContext, read)
            is DrawTweenPath -> drawTweenPathOp(op, paintState, remoteContext, read)
            is DrawToBitmap -> drawToBitmapOp(op, paintState, remoteContext, read, offscreenState)
            is DrawTextOnCircle -> drawTextOnCircleOp(op, paintState, read, textMeasurer)
            is DrawBitmapFontText -> drawBitmapFontTextOp(op, paintState, remoteContext, read)
            is DrawBitmapTextAnchored ->
                drawBitmapTextAnchoredOp(op, paintState, remoteContext, read)
            is DrawBitmapFontTextOnPath ->
                drawBitmapFontTextOnPathOp(op, paintState, remoteContext, read)
            is DrawContent ->
                drawContentWithPaint(
                    paintState = paintState,
                    contentLayer = contentLayer,
                    isDrawingToOffscreen = offscreenState.isDrawingToOffscreen,
                    contentOrigin = contentOrigin,
                    onDrawContent = onDrawContent,
                )
            is PathData -> op.applyTrackingPathChange(remoteContext)
            is PathCreate -> op.apply(remoteContext)
            is PathAppend -> op.apply(remoteContext)
            is PathExpression -> op.apply(remoteContext)
            is PathTween -> applyPathTween(op, remoteContext, read)
            is PathCombine -> applyPathCombine(op, remoteContext, read)
            is MatrixConstant -> {
                if (remoteContext.getObject(op.id) !== op) {
                    op.apply(remoteContext)
                }
            }
            is MatrixExpression -> {
                if (remoteContext.getObject(op.id) !== op) {
                    remoteContext.putObject(op.id, op)
                }
                // Evaluate into op.mValues via `read`: GraphContext drops putObject and the
                // nanoTime loadFloat notification so draw does not dirty SnapshotStateMaps.
                op.apply(read)
            }
            is MatrixVectorMath -> {
                val matrix = read.getObject(op.mMatrixId)
                if (matrix is MatrixExpression) {
                    matrix.updateVariables(read)
                    matrix.apply(read)
                }
                op.apply(remoteContext)
            }
            is ComponentValue -> {
                val w = this.size.width
                val h = this.size.height
                when (op.getType()) {
                    ComponentValue.WIDTH -> remoteContext.loadFloat(op.getValueId(), w)
                    ComponentValue.HEIGHT -> remoteContext.loadFloat(op.getValueId(), h)
                }
            }
            // Performed on the host view through the document's haptic engine (see
            // RcPlayerHapticEffect). It fires each time the stream runs it, so documents gate it
            // with an impulse or conditional.
            is HapticFeedback -> op.apply(remoteContext)
            // Played through the document's sound engine, if the host installed one. Like haptics,
            // it plays each time the stream runs it, so documents gate it with an impulse.
            // SoundExpression / SoundData are registered once at setup, not per draw.
            is PlaySound -> op.apply(remoteContext)
        }
    }
    // If a DRAW_TO_BITMAP redirect was left active (document didn't reset with bitmapId 0), restore
    // the on-screen canvas so it isn't leaked to subsequent drawing on this DrawScope.
    offscreenState.popTo(initialOffscreenDepth, this, paintState)
}
