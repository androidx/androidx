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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.ConditionalOperations
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
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.MatrixRestore
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.MatrixSave
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.ImpulseProcess
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.player.compose.utils.getPath
import androidx.compose.remote.player.compose.utils.getTweenPath
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

/** Dereference a PaintOperation image/path id (mirrors PaintOperation.getId's PTR_DEREFERENCE). */
private fun derefId(rawId: Int, context: RemoteContext): Int =
    if ((rawId and PaintOperation.PTR_DEREFERENCE) != 0) {
        context.mRemoteComposeState.getInteger(rawId and PaintOperation.VALUE_MASK)
    } else {
        rawId and PaintOperation.VALUE_MASK
    }

/**
 * Returns the decoded [Bitmap] for [id], decoding it on first use (lazy bitmap loading).
 *
 * The embedded player registers each [BitmapData]'s metadata at setup (via `putObject`, so declared
 * width/height stay available to `ImageAttribute` without a decode) but defers the costly pixel
 * decode until the bitmap is actually needed — when it is drawn, or when its Image component first
 * composes. The decoded bitmap is cached in the state's data map (`getFromId`), so later lookups
 * are cheap. Returns null if there is no bitmap or metadata for the id.
 */
internal fun resolveBitmap(remoteContext: RemoteContext, id: Int): Bitmap? {
    val cached = remoteContext.mRemoteComposeState.getFromId(id)
    if (cached is Bitmap) return cached
    // Not decoded yet: find the registered BitmapData and decode it now (apply = putObject +
    // loadBitmap, which caches the decoded Bitmap under the id).
    val data = remoteContext.mRemoteComposeState.getObject(id) as? BitmapData ?: return null
    data.apply(remoteContext)
    return remoteContext.mRemoteComposeState.getFromId(id) as? Bitmap
}

/**
 * Resolves a document image draw to a [Bitmap] through the pluggable [RcImageLoader] (on [graph]),
 * falling back to the embedded decode ([resolveBitmap]). Reading the loader's `State` here
 * registers the draw as an observer, so a host's asynchronously-loaded image re-runs the draw when
 * it arrives.
 *
 * The canvas blit ops need a [Bitmap] (for src/dst sub-rect blitting), so only a [BitmapDrawable]
 * from the loader is used directly; any other host [android.graphics.drawable.Drawable] falls back
 * to the embedded bitmap. (The composable Image layout, by contrast, can render any Drawable.)
 */
internal fun resolveCanvasBitmap(
    graph: GraphContext?,
    remoteContext: RemoteContext,
    id: Int,
): Bitmap? {
    val loaded = graph?.imageLoader?.loadImage(id)?.value
    if (loaded is BitmapDrawable) return loaded.bitmap
    return resolveBitmap(remoteContext, id)
}

/** Backstop on [LoopOperation] iterations so a malformed bound can't hang the draw thread. */
private const val MAX_LOOP_ITERATIONS = 100_000

internal fun resolveFloat(value: Float, fallback: Float, context: RemoteContext): Float {
    // A NaN-encoded value is a variable reference. [context] is the draw read context — normally
    // the
    // GraphContext, which resolves time ids from the Compose frame clock and computed ids through
    // their derivedStateOf (so a time/variable-driven value re-runs this draw when it changes), and
    // a plain leaf id through the shared snapshot store. There is no separate draw-path variable
    // map.
    return if (value.isNaN()) context.getFloat(Utils.idFromNan(value)) else fallback
}

internal fun DrawScope.executeOperations(
    operations: List<Operation>,
    remoteContext: RemoteContext,
    textMeasurer: TextMeasurer,
    paintState: ComposeLocalPaint = ComposeLocalPaint(),
    onDrawContent: () -> Unit = {},
    graph: GraphContext? = null,
) {
    // Reads route through the GraphContext when present: it resolves time ids from the Compose
    // frame
    // clock and computed ids through their derivedStateOf (reactive, chains), and a leaf id falls
    // through to the same shared snapshot store. So reading a time/variable-driven value here
    // registers this draw as an observer of the relevant Compose state — the draw re-runs when that
    // state changes, with no per-frame applyOperations refreshing the store. WRITES (op.apply,
    // overrideFloat, loadFloat, bitmap decode) must NOT go through the graph — it suppresses writes
    // during its derived evaluation — so they stay on `remoteContext` (the real store).
    // GraphContext
    // shares that store, so leaf reads are identical either way.
    val read: RemoteContext = graph ?: remoteContext
    var canvasLevel = 0
    // For DRAW_TO_BITMAP: the original on-screen canvas, saved the first time the draw target is
    // redirected to an offscreen bitmap so it can be restored (on a `bitmapId == 0` reset, and
    // defensively at the end of the op stream).
    var mainCanvas: Canvas? = null
    operations.fastForEach { op ->
        if (op is VariableSupport) {
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
                // remoteContext for shader/texture bitmap decode (a write); read for reactive
                // colors.
                updatePaintFromBundle(op.mPaintData, paintState, remoteContext, read)
            }
            is FloatExpression -> {
                // Evaluate reactively (time/variables via the graph); write the result to the real
                // store so later ops/draws in this stream that read the id by store see it.
                val v = op.evaluate(read)
                remoteContext.loadFloat(op.mId, v)
            }
            is ColorConstant -> op.apply(remoteContext)
            is NamedVariable -> op.apply(remoteContext)
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
            is ImpulseProcess,
            is ImpulseOperation -> {
                // Impulse containers wrap their children (commonly the particle loop). The trigger
                // /
                // duration gating isn't modelled here — the children always run — so impulse-driven
                // content (e.g. particles) renders continuously rather than on event.
                executeOperations(
                    (op as Container).list,
                    remoteContext,
                    textMeasurer,
                    paintState,
                    onDrawContent,
                    graph,
                )
            }
            is ConditionalOperations -> {
                // Conditional draw: resolve the two operands, compare per type, and draw the child
                // ops only when the condition holds. Mirrors ConditionalOperations.paint.
                op.updateVariables(read)
                val data = op.readData()
                val a = data.varAOut
                val b = data.varBOut
                val run =
                    when (data.type) {
                        ConditionalOperations.TYPE_EQ -> a == b
                        ConditionalOperations.TYPE_NEQ -> a != b
                        ConditionalOperations.TYPE_LT -> a < b
                        ConditionalOperations.TYPE_LTE -> a <= b
                        ConditionalOperations.TYPE_GT -> a > b
                        else -> a >= b // TYPE_GTE
                    }
                if (run)
                    executeOperations(
                        op.list,
                        remoteContext,
                        textMeasurer,
                        paintState,
                        onDrawContent,
                        graph,
                    )
            }
            is LoopOperation -> {
                // Loop: resolve from/until/step (NaN-id bounds → variables), then run the child
                // list
                // once per iteration, loading the index variable each pass. Mirrors
                // LoopOperation.paint, with a hard iteration cap as an infinite-loop backstop.
                op.updateVariables(read)
                val data = op.readData()
                val from = data.fromOut
                val until = data.untilOut
                val step = data.stepOut
                val indexId = data.indexVariableId
                if (step != 0f && (step > 0f) == (from < until)) {
                    var i = from
                    var guard = 0
                    while (i < until && guard < MAX_LOOP_ITERATIONS) {
                        if (indexId != 0) remoteContext.loadFloat(indexId, i)
                        executeOperations(
                            op.list,
                            remoteContext,
                            textMeasurer,
                            paintState,
                            onDrawContent,
                            graph,
                        )
                        i += step
                        guard++
                    }
                }
            }
            is FloatFunctionCall -> {
                // Invoke a defined float function: load the resolved argument values
                // (updateVariables
                // ran above) into the function's parameter variables, then run its body, which
                // writes
                // its outputs to the context for downstream ops to read. Mirrors FloatFunctionCall
                // .paint. FloatFunctionDefine itself has no draw effect, so it needs no case.
                val data = op.readData()
                val fn = data.function as? FloatFunctionDefine
                val outArgs = data.outArgs
                if (fn != null && outArgs != null) {
                    val argIds = fn.args
                    for (j in outArgs.indices) {
                        if (j < argIds.size) remoteContext.loadFloat(argIds[j], outArgs[j])
                    }
                    fn.execute(remoteContext)
                }
            }
            is DrawCircle -> {
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val brush = paintState.brush
                if (brush != null) {
                    val data = op.readDataReflection()
                    drawCircle(
                        brush = brush,
                        center = Offset(data.v1, data.v2),
                        radius = data.v3,
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    val data = op.readDataReflection()
                    val v1 = resolveFloat(data.value1, data.v1, read)
                    val v2 = resolveFloat(data.value2, data.v2, read)
                    val v3 = resolveFloat(data.value3, data.v3, read)

                    drawCircle(
                        color = paintState.effectiveColor(),
                        center = Offset(v1, v2),
                        radius = v3,
                        style = style,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is DrawRect -> {
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val data = op.readDataReflection()
                val x1 = resolveFloat(data.x1Value, data.x1, read)
                val y1 = resolveFloat(data.y1Value, data.y1, read)
                val x2 = resolveFloat(data.x2Value, data.x2, read)
                val y2 = resolveFloat(data.y2Value, data.y2, read)

                val brush = paintState.brush
                if (brush != null) {
                    drawRect(
                        brush = brush,
                        topLeft = Offset(x1, y1),
                        size = Size(x2 - x1, y2 - y1),
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    drawRect(
                        color = paintState.effectiveColor(),
                        topLeft = Offset(x1, y1),
                        size = Size(x2 - x1, y2 - y1),
                        style = style,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is DrawLine -> {
                val data = op.readDataReflection()
                val x1 = resolveFloat(data.x1Value, data.x1, read)
                val y1 = resolveFloat(data.y1Value, data.y1, read)
                val x2 = resolveFloat(data.x2Value, data.x2, read)
                val y2 = resolveFloat(data.y2Value, data.y2, read)

                drawLine(
                    color = paintState.effectiveColor(),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = paintState.strokeWidth,
                    cap = mapStrokeCap(paintState.strokeCap),
                    blendMode = paintState.blendMode,
                )
            }
            is DrawOval -> {
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val data = op.readDataReflection()
                val brush = paintState.brush
                if (brush != null) {
                    drawOval(
                        brush = brush,
                        topLeft = Offset(data.x1, data.y1),
                        size = Size(data.x2 - data.x1, data.y2 - data.y1),
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    drawOval(
                        color = paintState.effectiveColor(),
                        topLeft = Offset(data.x1, data.y1),
                        size = Size(data.x2 - data.x1, data.y2 - data.y1),
                        style = style,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is DrawRoundRect -> {
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val data = op.readDataReflection()
                val v1 = resolveFloat(data.value1, data.v1, read)
                val v2 = resolveFloat(data.value2, data.v2, read)
                val v3 = resolveFloat(data.value3, data.v3, read)
                val v4 = resolveFloat(data.value4, data.v4, read)
                val v5 = resolveFloat(data.value5, data.v5, read)
                val v6 = resolveFloat(data.value6, data.v6, read)

                val brush = paintState.brush
                if (brush != null) {
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(v1, v2),
                        size = Size(v3 - v1, v4 - v2),
                        cornerRadius = CornerRadius(v5, v6),
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    drawRoundRect(
                        color = paintState.effectiveColor(),
                        topLeft = Offset(v1, v2),
                        size = Size(v3 - v1, v4 - v2),
                        cornerRadius = CornerRadius(v5, v6),
                        style = style,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is DrawSector -> {
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val data = op.readDataReflection()
                val v1 = resolveFloat(data.value1, data.v1, read)
                val v2 = resolveFloat(data.value2, data.v2, read)
                val v3 = resolveFloat(data.value3, data.v3, read)
                val v4 = resolveFloat(data.value4, data.v4, read)
                val v5 = resolveFloat(data.value5, data.v5, read)
                val v6 = resolveFloat(data.value6, data.v6, read)

                val brush = paintState.brush
                if (brush != null) {
                    drawArc(
                        brush = brush,
                        startAngle = v5,
                        sweepAngle = v6,
                        useCenter = true,
                        topLeft = Offset(v1, v2),
                        size = Size(v3 - v1, v4 - v2),
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    drawArc(
                        color = paintState.effectiveColor(),
                        startAngle = v5,
                        sweepAngle = v6,
                        useCenter = true,
                        topLeft = Offset(v1, v2),
                        size = Size(v3 - v1, v4 - v2),
                        style = style,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is DrawArc -> {
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val data = op.readDataReflection()
                val v1 = resolveFloat(data.value1, data.v1, read)
                val v2 = resolveFloat(data.value2, data.v2, read)
                val v3 = resolveFloat(data.value3, data.v3, read)
                val v4 = resolveFloat(data.value4, data.v4, read)
                val v5 = resolveFloat(data.value5, data.v5, read)
                val v6 = resolveFloat(data.value6, data.v6, read)

                val brush = paintState.brush
                if (brush != null) {
                    drawArc(
                        brush = brush,
                        startAngle = v5,
                        sweepAngle = v6,
                        useCenter = false,
                        topLeft = Offset(v1, v2),
                        size = Size(v3 - v1, v4 - v2),
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    drawArc(
                        color = paintState.effectiveColor(),
                        startAngle = v5,
                        sweepAngle = v6,
                        useCenter = false,
                        topLeft = Offset(v1, v2),
                        size = Size(v3 - v1, v4 - v2),
                        style = style,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is DrawBitmap -> {
                val data = op.readDataReflection()
                val left = resolveFloat(data.left, data.outputLeft, read)
                val top = resolveFloat(data.top, data.outputTop, read)
                val right = resolveFloat(data.right, data.outputRight, read)
                val bottom = resolveFloat(data.bottom, data.outputBottom, read)

                val bitmap = resolveCanvasBitmap(graph, remoteContext, data.id)
                if (bitmap != null) {
                    val image = bitmap.asImageBitmap()
                    val dstW = right - left
                    val dstH = bottom - top
                    if (dstW > 0f && dstH > 0f) {
                        // Scale the whole bitmap into the [left,top,right,bottom] destination rect,
                        // matching DrawBitmap.paint() (the previous code ignored right/bottom and
                        // drew
                        // at natural size in the top-left).
                        drawImage(
                            image = image,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(image.width, image.height),
                            dstOffset = IntOffset(left.toInt(), top.toInt()),
                            dstSize = IntSize(dstW.toInt(), dstH.toInt()),
                            alpha = paintState.alpha,
                            colorFilter = paintState.colorFilter,
                            blendMode = paintState.blendMode,
                        )
                    } else {
                        drawImage(
                            image = image,
                            topLeft = Offset(left, top),
                            alpha = paintState.alpha,
                            colorFilter = paintState.colorFilter,
                            blendMode = paintState.blendMode,
                        )
                    }
                }
            }
            is DrawPath -> {
                val data = op.readDataReflection()
                val path = remoteContext.mRemoteComposeState.getPath(data.id, data.start, data.end)
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val brush = paintState.brush
                if (brush != null) {
                    drawPath(
                        path = path,
                        brush = brush,
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    drawPath(
                        path = path,
                        color = paintState.effectiveColor(),
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is MatrixSave -> {
                drawContext.canvas.save()
            }
            is MatrixRestore -> {
                drawContext.canvas.restore()
            }
            is MatrixTranslate -> {
                val data = op.readDataReflection()
                val v1 = resolveFloat(data.value1, data.v1, read)
                val v2 = resolveFloat(data.value2, data.v2, read)
                drawContext.transform.translate(v1, v2)
            }
            is MatrixScale -> {
                val data = op.readDataReflection()
                val x1 = resolveFloat(data.x1Value, data.x1, read)
                val y1 = resolveFloat(data.y1Value, data.y1, read)
                val x2 = resolveFloat(data.x2Value, data.x2, read)
                val y2 = resolveFloat(data.y2Value, data.y2, read)
                drawContext.transform.scale(x1, y1, Offset(x2, y2))
            }
            is MatrixRotate -> {
                val data = op.readDataReflection()
                val v1 = resolveFloat(data.value1, data.v1, read)
                val v2 = resolveFloat(data.value2, data.v2, read)
                val v3 = resolveFloat(data.value3, data.v3, read)
                drawContext.transform.rotate(v1, Offset(v2, v3))
            }
            is MatrixSkew -> {
                val data = op.readDataReflection()
                val sx = resolveFloat(data.value1, data.v1, read)
                val sy = resolveFloat(data.value2, data.v2, read)
                val skewMatrix =
                    Matrix().apply {
                        this[1, 0] = sx // SkewX
                        this[0, 1] = sy // SkewY
                    }
                drawContext.transform.transform(skewMatrix)
            }
            is ClipRect -> {
                // Imperative clip on the underlying canvas (bounded by the surrounding
                // MATRIX_SAVE/RESTORE the document emits, like the other canvas-state ops here).
                val data = op.readDataReflection()
                val x1 = resolveFloat(data.x1Value, data.x1, read)
                val y1 = resolveFloat(data.y1Value, data.y1, read)
                val x2 = resolveFloat(data.x2Value, data.x2, read)
                val y2 = resolveFloat(data.y2Value, data.y2, read)
                drawContext.canvas.clipRect(x1, y1, x2, y2)
            }
            is ClipPath -> {
                // mId/mRegionOp are package-private in remote-core (left unchanged), so read
                // reflectively. Map the region op to Compose's ClipOp (only Intersect/Difference).
                val data = op.readData()
                val pathId = data.id
                val regionOp = data.regionOp
                val path = remoteContext.mRemoteComposeState.getPath(pathId, 0f, 1f)
                val clipOp =
                    if (regionOp == ClipPath.PATH_CLIP_DIFFERENCE) ClipOp.Difference
                    else ClipOp.Intersect
                drawContext.canvas.clipPath(path, clipOp)
            }
            is DrawText -> {
                // Canvas text run. Mirrors DrawText.paint / the View player's drawTextRun: resolve
                // text by id, substring by [start, end), build a framework Paint from the current
                // paint state, and draw via the native canvas. (Layout text uses Compose `Text`;
                // this is the canvas/DrawScope path.)
                val full = read.getText(op.mTextID)
                if (full != null && !paintState.textSize.isNaN()) {
                    val len = full.length
                    val start = op.mStart.coerceIn(0, len)
                    val end =
                        if (op.mEnd == -1 || op.mEnd > len) len else op.mEnd.coerceIn(start, len)
                    val text = full.substring(start, end)
                    val x = resolveFloat(op.mX, op.mOutX, read)
                    val y = resolveFloat(op.mY, op.mOutY, read)
                    // textMeasurer is non-null
                    val fontStyle = paintState.fontStyle
                    val fontWeight = FontWeight(paintState.fontWeight)
                    // TODO: Support proper font family resolution (see aosp/4187117)
                    val fontFamily =
                        when (paintState.fontFamily) {
                            1 -> FontFamily.SansSerif
                            2 -> FontFamily.Serif
                            3 -> FontFamily.Monospace
                            else -> FontFamily.Default
                        }

                    val style =
                        if (paintState.isStroke)
                            Stroke(
                                width = paintState.strokeWidth,
                                cap = mapStrokeCap(paintState.strokeCap),
                                join = mapStrokeJoin(paintState.strokeJoin),
                            )
                        else Fill

                    val textStyle =
                        if (paintState.brush != null) {
                            TextStyle(
                                brush = paintState.brush,
                                alpha = paintState.alpha,
                                fontSize = paintState.textSize.toSp(),
                                fontWeight = fontWeight,
                                fontStyle = fontStyle,
                                fontFamily = fontFamily,
                                drawStyle = style,
                            )
                        } else {
                            TextStyle(
                                color = paintState.effectiveColor(),
                                fontSize = paintState.textSize.toSp(),
                                fontWeight = fontWeight,
                                fontStyle = fontStyle,
                                fontFamily = fontFamily,
                                drawStyle = style,
                            )
                        }

                    val textLayoutResult = textMeasurer.measure(text = text, style = textStyle)

                    // Assuming y is baseline
                    val baseline = textLayoutResult.getLineBaseline(0)
                    val topLeft = Offset(x, y - baseline)

                    drawText(textLayoutResult = textLayoutResult, topLeft = topLeft)
                }
            }
            is DrawTextOnPath -> {
                // Lay text along a path. mTextId is public; mPathId/mOutHOffset/mOutVOffset are
                // package-private (read reflectively). updateVariables (run above) has already
                // resolved the offsets into mOut*. The path id may be a PTR_DEREFERENCE
                // indirection,
                // mirroring PaintOperation.getId.
                val full = read.getText(op.mTextId)
                if (full != null && !paintState.textSize.isNaN()) {
                    val data = op.readData()
                    val pathId = derefId(data.pathId, read)
                    val hOffset = data.hOffset
                    val vOffset = data.vOffset
                    val path = remoteContext.mRemoteComposeState.getPath(pathId, 0f, 1f)
                    drawContext.canvas.nativeCanvas.drawTextOnPath(
                        full,
                        path.asAndroidPath(),
                        hOffset,
                        vOffset,
                        paintState.toNativeTextPaint(read),
                    )
                }
            }
            is DrawTextAnchored -> {
                // Draw text positioned about an anchor point (mX, mY) with pan in [-1, 1]. All
                // fields
                // are package-private (read reflectively); updateVariables (run above) resolved
                // them
                // into mOut*. Replicates DrawTextAnchored.getHorizontalOffset/getVerticalOffset
                // using
                // measured text bounds.
                val data = op.readData()
                val textId = data.textId
                val full = read.getText(textId)
                if (full != null && !paintState.textSize.isNaN()) {
                    val nativePaint = paintState.toNativeTextPaint(read)
                    val flags = data.flags
                    val baseline = (flags and DrawTextAnchored.BASELINE_RELATIVE) != 0
                    val bounds = Rect()
                    nativePaint.getTextBounds(full, 0, full.length, bounds)
                    val outX = data.x
                    val outY = data.y
                    val outPanX = data.panX
                    val outPanY = data.panY
                    val textWidth = (bounds.right - bounds.left).toFloat()
                    val textHeight = (bounds.bottom - bounds.top).toFloat()
                    val hOffset = (0f - textWidth) * (1f + outPanX) / 2f - bounds.left
                    val x = outX + hOffset
                    val y =
                        if (outPanY.isNaN()) {
                            outY
                        } else {
                            outY +
                                (0f - textHeight) * (1f - outPanY) / 2f +
                                (if (baseline) textHeight / 2f else -bounds.top.toFloat())
                        }
                    drawContext.canvas.nativeCanvas.drawText(full, x, y, nativePaint)
                }
            }
            is DrawBitmapScaled -> {
                val data = op.readDataReflection()
                val imageId =
                    if ((data.imageId and PaintOperation.PTR_DEREFERENCE) != 0) {
                        remoteContext.mRemoteComposeState.getInteger(
                            data.imageId and PaintOperation.VALUE_MASK
                        )
                    } else {
                        data.imageId and PaintOperation.VALUE_MASK
                    }
                val bitmap = resolveCanvasBitmap(graph, remoteContext, imageId)
                if (bitmap != null) {
                    val srcLeft = resolveFloat(data.srcLeft, data.outSrcLeft, read)
                    val srcTop = resolveFloat(data.srcTop, data.outSrcTop, read)
                    val srcRight = resolveFloat(data.srcRight, data.outSrcRight, read)
                    val srcBottom = resolveFloat(data.srcBottom, data.outSrcBottom, read)
                    val dstLeft = resolveFloat(data.dstLeft, data.outDstLeft, read)
                    val dstTop = resolveFloat(data.dstTop, data.outDstTop, read)
                    val dstRight = resolveFloat(data.dstRight, data.outDstRight, read)
                    val dstBottom = resolveFloat(data.dstBottom, data.outDstBottom, read)
                    val scaleFactor = resolveFloat(data.scaleFactor, data.outScaleFactor, read)

                    val imageScaling = ImageScaling()
                    imageScaling.setup(
                        srcLeft,
                        srcTop,
                        srcRight,
                        srcBottom,
                        dstLeft,
                        dstTop,
                        dstRight,
                        dstBottom,
                        data.scaleType,
                        scaleFactor,
                    )
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(dstLeft, dstTop, dstRight, dstBottom)
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        srcOffset = IntOffset(srcLeft.toInt(), srcTop.toInt()),
                        srcSize =
                            IntSize((srcRight - srcLeft).toInt(), (srcBottom - srcTop).toInt()),
                        dstOffset =
                            IntOffset(
                                imageScaling.mFinalDstLeft.toInt(),
                                imageScaling.mFinalDstTop.toInt(),
                            ),
                        dstSize =
                            IntSize(
                                (imageScaling.mFinalDstRight - imageScaling.mFinalDstLeft).toInt(),
                                (imageScaling.mFinalDstBottom - imageScaling.mFinalDstTop).toInt(),
                            ),
                        alpha = paintState.alpha,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                    drawContext.canvas.restore()
                }
            }
            is DrawBitmapInt -> {
                // Integer-coordinate bitmap blit (src rect -> dst rect). All fields are
                // package-private (read reflectively); the image id may be a PTR_DEREFERENCE
                // indirection. Mirrors DrawBitmapInt.paint() (the int-coordinate bitmap blit).
                val data = op.readData()
                val imageId = derefId(data.imageId, read)
                val bitmap = resolveCanvasBitmap(graph, remoteContext, imageId)
                if (bitmap != null) {
                    val srcLeft = data.srcLeft
                    val srcTop = data.srcTop
                    val srcRight = data.srcRight
                    val srcBottom = data.srcBottom
                    val dstLeft = data.dstLeft
                    val dstTop = data.dstTop
                    val dstRight = data.dstRight
                    val dstBottom = data.dstBottom
                    val dstW = dstRight - dstLeft
                    val dstH = dstBottom - dstTop
                    if (dstW > 0 && dstH > 0) {
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            srcOffset = IntOffset(srcLeft, srcTop),
                            srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                            dstOffset = IntOffset(dstLeft, dstTop),
                            dstSize = IntSize(dstW, dstH),
                            alpha = paintState.alpha,
                            colorFilter = paintState.colorFilter,
                            blendMode = paintState.blendMode,
                        )
                    }
                }
            }
            is DrawTweenPath -> {
                // Draw an interpolated path between two paths. mPath1Id/mPath2Id and the resolved
                // mOut* (updateVariables ran above) are package-private (read reflectively). Path
                // ids
                // may be PTR_DEREFERENCE indirections.
                val data = op.readData()
                val path1Id = derefId(data.path1Id, read)
                val path2Id = derefId(data.path2Id, read)
                val tween = data.tween
                val start = data.start
                val stop = data.stop
                val path =
                    remoteContext.mRemoteComposeState.getTweenPath(
                        path1Id,
                        path2Id,
                        tween,
                        start,
                        stop,
                    )
                val style =
                    if (paintState.isStroke)
                        Stroke(
                            width = paintState.strokeWidth,
                            cap = mapStrokeCap(paintState.strokeCap),
                            join = mapStrokeJoin(paintState.strokeJoin),
                        )
                    else Fill
                val brush = paintState.brush
                if (brush != null) {
                    drawPath(
                        path = path,
                        brush = brush,
                        alpha = paintState.alpha,
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                } else {
                    drawPath(
                        path = path,
                        color = paintState.effectiveColor(),
                        style = style,
                        colorFilter = paintState.colorFilter,
                        blendMode = paintState.blendMode,
                    )
                }
            }
            is DrawToBitmap -> {
                // Offscreen render target: redirect the DrawScope's canvas to one backed by the
                // target bitmap so subsequent draw ops accumulate there; `bitmapId == 0` restores
                // the
                // on-screen canvas (mirrors DrawToBitmap.paint()). MODE_NO_INITIALIZE
                // skips the erase-to-color. Compose's DrawScope draws (incl. brushes) follow the
                // swapped canvas because the framework draws target drawContext.canvas.
                val data = op.readData()
                val bitmapId = derefId(data.bitmapId, read)
                val mode = data.mode
                val color = data.color
                if (mainCanvas == null) mainCanvas = drawContext.canvas
                if (bitmapId == 0) {
                    drawContext.canvas = mainCanvas!!
                } else {
                    val stored = resolveBitmap(remoteContext, bitmapId)
                    if (stored != null) {
                        // The target must be a mutable bitmap to back a Canvas. Decoded document
                        // bitmaps are immutable, so draw into a mutable copy and store it back
                        // under
                        // the same id, so a later DRAW_BITMAP of this id reads the rendered
                        // content.
                        val target =
                            if (stored.isMutable) {
                                stored
                            } else {
                                stored.copy(Bitmap.Config.ARGB_8888, true).also {
                                    remoteContext.mRemoteComposeState.cacheData(bitmapId, it)
                                }
                            }
                        if ((mode and DrawToBitmap.MODE_NO_INITIALIZE) == 0) {
                            target.eraseColor(color)
                        }
                        drawContext.canvas = Canvas(target.asImageBitmap())
                    }
                }
            }
            is DrawTextOnCircle -> {
                // Curved text: lay the string along an arc of the given circle and draw via the
                // native canvas. Mirrors DrawTextOnCircle.paint / the View player's
                // drawTextOnCircle
                // — build an arc Path sized to the measured text and drawTextOnPath onto it.
                // mTextId is
                // public; the geometry/alignment/placement fields are package-private (read
                // reflectively).
                val full = read.getText(op.mTextId)
                if (full != null && !paintState.textSize.isNaN()) {
                    val data = op.readData()
                    val centerX = data.centerX
                    val centerY = data.centerY
                    val radius = data.radius
                    val startAngle = data.startAngle
                    val warpRadiusOffset = data.warpRadiusOffset
                    val alignment = data.alignment
                    val placement = data.placement
                    val nativePaint = paintState.toNativeTextPaint(read)
                    val textWidth = nativePaint.measureText(full)
                    val finalRadius = radius + warpRadiusOffset
                    val clockwise = placement == DrawTextOnCircle.Placement.OUTSIDE
                    var sweepDegrees =
                        Math.toDegrees((textWidth / finalRadius).toDouble()).toFloat()
                    var finalStartAngle = startAngle
                    if (!clockwise) {
                        sweepDegrees = -sweepDegrees
                        when (alignment) {
                            DrawTextOnCircle.Alignment.CENTER ->
                                finalStartAngle = startAngle + kotlin.math.abs(sweepDegrees) / 2f
                            DrawTextOnCircle.Alignment.END ->
                                finalStartAngle = startAngle + kotlin.math.abs(sweepDegrees)
                            else -> {}
                        }
                    } else {
                        when (alignment) {
                            DrawTextOnCircle.Alignment.CENTER ->
                                finalStartAngle = startAngle - sweepDegrees / 2f
                            DrawTextOnCircle.Alignment.END ->
                                finalStartAngle = startAngle - sweepDegrees
                            else -> {}
                        }
                    }
                    val textPath =
                        AndroidPath().apply {
                            addArc(
                                centerX - finalRadius,
                                centerY - finalRadius,
                                centerX + finalRadius,
                                centerY + finalRadius,
                                finalStartAngle,
                                sweepDegrees,
                            )
                        }
                    drawContext.canvas.nativeCanvas.drawTextOnPath(
                        full,
                        textPath,
                        0f,
                        0f,
                        nativePaint,
                    )
                }
            }
            is DrawBitmapFontText -> {
                // Bitmap-font text: walk the string glyph-by-glyph via BitmapFontData.lookupGlyph
                // and
                // blit each glyph's bitmap into its laid-out dst rect (with margins, kerning, and
                // glyph spacing). Mirrors DrawBitmapFontText.paint. Fields are package-private
                // (read
                // reflectively); the font's kerning table likewise.
                val data = op.readData()
                val textId = data.textId
                val fontId = data.fontId
                val full = read.getText(textId)
                val font = remoteContext.getObject(fontId) as? BitmapFontData
                if (full != null && font != null) {
                    val start = data.start
                    val end = data.end
                    val text =
                        when {
                            end == -1 || end > full.length ->
                                full.substring(start.coerceIn(0, full.length))
                            else ->
                                full.substring(
                                    start.coerceIn(0, full.length),
                                    end.coerceIn(start, full.length),
                                )
                        }
                    val kerning = font.getKerningTable()
                    val y = data.y
                    val glyphSpacing = data.glyphSpacing
                    var xPos = data.x
                    var pos = 0
                    var prevGlyph = ""
                    while (pos < text.length) {
                        val glyph = font.lookupGlyph(text, pos)
                        if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                            pos++
                            prevGlyph = ""
                            continue
                        }
                        pos += glyph.mChars!!.length
                        if (glyph.mBitmapId == -1) {
                            // A glyph id of -1 represents a space: advance by its margins only.
                            xPos += glyph.mMarginLeft + glyph.mMarginRight
                            prevGlyph = glyph.mChars!!
                            continue
                        }
                        xPos += glyph.mMarginLeft
                        kerning?.get(prevGlyph + glyph.mChars)?.let { xPos += it }
                        val glyphBitmap = resolveBitmap(remoteContext, glyph.mBitmapId)
                        if (
                            glyphBitmap != null && glyph.mBitmapWidth > 0 && glyph.mBitmapHeight > 0
                        ) {
                            drawImage(
                                image = glyphBitmap.asImageBitmap(),
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(glyphBitmap.width, glyphBitmap.height),
                                dstOffset = IntOffset(xPos.toInt(), (y + glyph.mMarginTop).toInt()),
                                dstSize =
                                    IntSize(
                                        glyph.mBitmapWidth.toInt(),
                                        glyph.mBitmapHeight.toInt(),
                                    ),
                                blendMode = paintState.blendMode,
                            )
                        }
                        xPos += glyph.mBitmapWidth + glyph.mMarginRight + glyphSpacing
                        prevGlyph = glyph.mChars!!
                    }
                }
            }
            is DrawBitmapTextAnchored -> {
                // Bitmap-font text anchored about (outX, outY) with pan in [-1, 1]. Measures the
                // run
                // (xMax + tallest glyph) to derive the pan offsets, then blits each glyph. Mirrors
                // DrawBitmapTextAnchored.measure/getHorizontalOffset/getVerticalOffset/paint.
                val data = op.readData()
                val textId = data.textId
                val fontId = data.fontId
                val full = read.getText(textId)
                val font = remoteContext.getObject(fontId) as? BitmapFontData
                if (full != null && font != null) {
                    val start = data.start
                    val end = data.end
                    val text =
                        full.substring(
                            start.coerceAtLeast(0),
                            if (end < 0 || end > full.length) full.length else end,
                        )
                    val kerning = font.getKerningTable()
                    val glyphSpacing = data.glyphSpacing
                    // measure(): xMax is the advance width; yMin/yMax bound the glyph heights.
                    var xMeasure = 0f
                    var yMin = 1000f
                    var yMax = -Float.MAX_VALUE
                    run {
                        var pos = 0
                        while (pos < text.length) {
                            val glyph = font.lookupGlyph(text, pos)
                            if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                                pos++
                                continue
                            }
                            pos += glyph.mChars!!.length
                            xMeasure += glyph.mMarginLeft + glyph.mMarginRight
                            if (glyph.mBitmapId != -1) xMeasure += glyph.mBitmapWidth
                            yMax =
                                maxOf(
                                    yMax,
                                    (glyph.mBitmapHeight + glyph.mMarginTop + glyph.mMarginBottom)
                                        .toFloat(),
                                )
                            yMin = minOf(yMin, glyph.mMarginTop.toFloat())
                            xMeasure += glyphSpacing
                        }
                    }
                    val outPanX = data.panX
                    val outPanY = data.panY
                    val textWidth = xMeasure
                    val textHeight = yMax - yMin
                    val hOffset = (0f - textWidth) * (1f + outPanX) / 2f
                    val vOffset = (0f - textHeight) * (1f - outPanY) / 2f - yMin
                    var xPos = data.x + hOffset
                    val yPos = data.y + vOffset
                    var pos = 0
                    var prevGlyph = ""
                    while (pos < text.length) {
                        val glyph = font.lookupGlyph(text, pos)
                        if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                            pos++
                            prevGlyph = ""
                            continue
                        }
                        pos += glyph.mChars!!.length
                        if (glyph.mBitmapId == -1) {
                            xPos += glyph.mMarginLeft + glyph.mMarginRight
                            prevGlyph = glyph.mChars!!
                            continue
                        }
                        xPos += glyph.mMarginLeft
                        kerning?.get(prevGlyph + glyph.mChars)?.let { xPos += it }
                        val glyphBitmap = resolveBitmap(remoteContext, glyph.mBitmapId)
                        if (
                            glyphBitmap != null && glyph.mBitmapWidth > 0 && glyph.mBitmapHeight > 0
                        ) {
                            drawImage(
                                image = glyphBitmap.asImageBitmap(),
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(glyphBitmap.width, glyphBitmap.height),
                                dstOffset =
                                    IntOffset(xPos.toInt(), (yPos + glyph.mMarginTop).toInt()),
                                dstSize =
                                    IntSize(
                                        glyph.mBitmapWidth.toInt(),
                                        glyph.mBitmapHeight.toInt(),
                                    ),
                                blendMode = paintState.blendMode,
                            )
                        }
                        xPos += glyph.mBitmapWidth + glyph.mMarginRight + glyphSpacing
                        prevGlyph = glyph.mChars!!
                    }
                }
            }
            is DrawBitmapFontTextOnPath -> {
                // Bitmap-font text laid along a path: each glyph is positioned + rotated by the
                // path
                // matrix at its mid-run fraction (position + tangent). Mirrors
                // DrawBitmapFontTextOnPath.measureWidth/paint. Path id may be a PTR_DEREFERENCE.
                val data = op.readData()
                val textId = data.textId
                val fontId = data.fontId
                val full = read.getText(textId)
                val font = remoteContext.getObject(fontId) as? BitmapFontData
                if (full != null && font != null) {
                    val start = data.start
                    val end = data.end
                    val text =
                        when {
                            end == -1 || end > full.length ->
                                full.substring(start.coerceIn(0, full.length))
                            else ->
                                full.substring(
                                    start.coerceIn(0, full.length),
                                    end.coerceIn(start, full.length),
                                )
                        }
                    val kerning = font.getKerningTable()
                    val yAdj = data.yAdj
                    val glyphSpacing = data.glyphSpacing
                    // measureWidth(): total advance (margins + kerning + glyph widths, no spacing).
                    var width = 0f
                    run {
                        var pos = 0
                        var prev = ""
                        while (pos < text.length) {
                            val glyph = font.lookupGlyph(text, pos)
                            if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                                pos++
                                prev = ""
                                continue
                            }
                            pos += glyph.mChars!!.length
                            if (glyph.mBitmapId == -1) {
                                width += glyph.mMarginLeft + glyph.mMarginRight
                                prev = ""
                                continue
                            }
                            width += glyph.mMarginLeft
                            kerning?.get(prev + glyph.mChars)?.let { width += it }
                            width += glyph.mBitmapWidth + glyph.mMarginRight
                            prev = glyph.mChars!!
                        }
                    }
                    val pathId = derefId(data.pathId, read)
                    val composePath = remoteContext.mRemoteComposeState.getPath(pathId, 0f, 1f)
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(composePath, false)
                    val pathLength = pathMeasure.length
                    if (width > 0f && pathLength > 0f) {
                        var progress = 0f
                        var pos = 0
                        var prevGlyph = ""
                        while (pos < text.length) {
                            val glyph = font.lookupGlyph(text, pos)
                            if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                                pos++
                                prevGlyph = ""
                                continue
                            }
                            pos += glyph.mChars!!.length
                            if (glyph.mBitmapId == -1) {
                                progress += glyph.mMarginLeft + glyph.mMarginRight
                                prevGlyph = ""
                                continue
                            }
                            progress += glyph.mMarginLeft
                            kerning?.get(prevGlyph + glyph.mChars)?.let { progress += it }
                            val halfGlyphWidth = 0.5f * glyph.mBitmapWidth
                            val fraction = (progress + halfGlyphWidth) / width
                            val glyphBitmap = resolveBitmap(remoteContext, glyph.mBitmapId)
                            if (
                                glyphBitmap != null &&
                                    glyph.mBitmapWidth > 0 &&
                                    glyph.mBitmapHeight > 0
                            ) {
                                val position = pathMeasure.getPosition(fraction * pathLength)
                                val tangent = pathMeasure.getTangent(fraction * pathLength)
                                val angle =
                                    Math.toDegrees(
                                            Math.atan2(tangent.y.toDouble(), tangent.x.toDouble())
                                        )
                                        .toFloat()

                                val composeMatrix =
                                    Matrix().apply {
                                        translate(position.x, position.y)
                                        rotateZ(angle)
                                    }

                                withTransform({
                                    transform(composeMatrix)
                                    translate(-halfGlyphWidth, yAdj + glyph.mMarginTop)
                                    val scaleX = glyph.mBitmapWidth / glyphBitmap.width.toFloat()
                                    val scaleY = glyph.mBitmapHeight / glyphBitmap.height.toFloat()
                                    scale(scaleX, scaleY, pivot = Offset.Zero)
                                }) {
                                    drawImage(
                                        image = glyphBitmap.asImageBitmap(),
                                        alpha = paintState.alpha,
                                    )
                                }
                            }
                            progress += glyph.mBitmapWidth + glyph.mMarginRight + glyphSpacing
                            prevGlyph = glyph.mChars!!
                        }
                    }
                }
            }
            is DrawContent -> {
                onDrawContent()
            }
            is PathData -> {
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
        }
    }
    // If a DRAW_TO_BITMAP redirect was left active (document didn't reset with bitmapId 0), restore
    // the on-screen canvas so it isn't leaked to subsequent drawing on this DrawScope.
    mainCanvas?.let { drawContext.canvas = it }
}
