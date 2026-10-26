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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawLine
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.DrawSector
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.player.compose.utils.getPath
import androidx.compose.remote.player.compose.utils.getTweenPath
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawCircleOp(
    op: DrawCircle,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
) {
    val style = paintState.toDrawStyle()
    val v1 = resolveFloat(op.value1, op.v1, read)
    val v2 = resolveFloat(op.value2, op.v2, read)
    val v3 = resolveFloat(op.value3, op.v3, read)
    val brush = paintState.brush
    if (brush != null) {
        drawCircle(
            brush = brush,
            center = Offset(v1, v2),
            radius = v3,
            alpha = paintState.effectiveAlpha(),
            style = style,
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    } else {
        drawCircle(
            color = paintState.effectiveColor(),
            center = Offset(v1, v2),
            radius = v3,
            style = style,
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    }
}

internal fun DrawScope.drawRectOp(
    op: DrawRect,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
) {
    val style = paintState.toDrawStyle()
    val x1 = resolveFloat(op.x1Value, op.rawX1, read)
    val y1 = resolveFloat(op.y1Value, op.rawY1, read)
    val x2 = resolveFloat(op.x2Value, op.rawX2, read)
    val y2 = resolveFloat(op.y2Value, op.rawY2, read)

    val brush = paintState.brush
    if (brush != null) {
        drawRect(
            brush = brush,
            topLeft = Offset(x1, y1),
            size = Size(x2 - x1, y2 - y1),
            alpha = paintState.effectiveAlpha(),
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
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    }
}

internal fun DrawScope.drawLineOp(
    op: DrawLine,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
) {
    val x1 = resolveFloat(op.x1Value, op.rawX1, read)
    val y1 = resolveFloat(op.y1Value, op.rawY1, read)
    val x2 = resolveFloat(op.x2Value, op.rawX2, read)
    val y2 = resolveFloat(op.y2Value, op.rawY2, read)

    val brush = paintState.brush
    if (brush != null) {
        drawLine(
            brush = brush,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = paintState.strokeWidth,
            cap = mapStrokeCap(paintState.strokeCap),
            pathEffect = paintState.pathEffect,
            alpha = paintState.effectiveAlpha(),
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    } else {
        drawLine(
            color = paintState.effectiveColor(),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = paintState.strokeWidth,
            cap = mapStrokeCap(paintState.strokeCap),
            pathEffect = paintState.pathEffect,
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    }
}

internal fun DrawScope.drawOvalOp(
    op: DrawOval,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
) {
    val style = paintState.toDrawStyle()
    val x1 = resolveFloat(op.x1Value, op.rawX1, read)
    val y1 = resolveFloat(op.y1Value, op.rawY1, read)
    val x2 = resolveFloat(op.x2Value, op.rawX2, read)
    val y2 = resolveFloat(op.y2Value, op.rawY2, read)
    val brush = paintState.brush
    if (brush != null) {
        drawOval(
            brush = brush,
            topLeft = Offset(x1, y1),
            size = Size(x2 - x1, y2 - y1),
            alpha = paintState.effectiveAlpha(),
            style = style,
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    } else {
        drawOval(
            color = paintState.effectiveColor(),
            topLeft = Offset(x1, y1),
            size = Size(x2 - x1, y2 - y1),
            style = style,
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    }
}

internal fun DrawScope.drawRoundRectOp(
    op: DrawRoundRect,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
) {
    val style = paintState.toDrawStyle()
    val v1 = resolveFloat(op.value1, op.v1, read)
    val v2 = resolveFloat(op.value2, op.v2, read)
    val v3 = resolveFloat(op.value3, op.v3, read)
    val v4 = resolveFloat(op.value4, op.v4, read)
    val v5 = resolveFloat(op.value5, op.v5, read)
    val v6 = resolveFloat(op.value6, op.v6, read)

    val brush = paintState.brush
    if (brush != null) {
        drawRoundRect(
            brush = brush,
            topLeft = Offset(v1, v2),
            size = Size(v3 - v1, v4 - v2),
            cornerRadius = CornerRadius(v5, v6),
            alpha = paintState.effectiveAlpha(),
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
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    }
}

internal fun DrawScope.drawSectorOp(
    op: DrawSector,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
) {
    val style = paintState.toDrawStyle()
    val v1 = resolveFloat(op.value1, op.v1, read)
    val v2 = resolveFloat(op.value2, op.v2, read)
    val v3 = resolveFloat(op.value3, op.v3, read)
    val v4 = resolveFloat(op.value4, op.v4, read)
    val v5 = resolveFloat(op.value5, op.v5, read)
    val v6 = resolveFloat(op.value6, op.v6, read)

    val brush = paintState.brush
    if (brush != null) {
        drawArc(
            brush = brush,
            startAngle = v5,
            sweepAngle = v6,
            useCenter = true,
            topLeft = Offset(v1, v2),
            size = Size(v3 - v1, v4 - v2),
            alpha = paintState.effectiveAlpha(),
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
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    }
}

internal fun DrawScope.drawArcOp(
    op: DrawArc,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
) {
    val style = paintState.toDrawStyle()
    val v1 = resolveFloat(op.value1, op.v1, read)
    val v2 = resolveFloat(op.value2, op.v2, read)
    val v3 = resolveFloat(op.value3, op.v3, read)
    val v4 = resolveFloat(op.value4, op.v4, read)
    val v5 = resolveFloat(op.value5, op.v5, read)
    val v6 = resolveFloat(op.value6, op.v6, read)

    val brush = paintState.brush
    if (brush != null) {
        drawArc(
            brush = brush,
            startAngle = v5,
            sweepAngle = v6,
            useCenter = false,
            topLeft = Offset(v1, v2),
            size = Size(v3 - v1, v4 - v2),
            alpha = paintState.effectiveAlpha(),
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
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
        )
    }
}

internal fun DrawScope.drawPathOp(
    op: DrawPath,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val pathId = derefId(op.pathId, read)
    val path = remoteContext.mRemoteComposeState.getPath(pathId, op.start, op.end)
    val style = paintState.toDrawStyle()
    val brush = paintState.brush
    if (brush != null) {
        drawPath(
            path = path,
            brush = brush,
            alpha = paintState.effectiveAlpha(),
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

/**
 * Draw an interpolated path between two paths (`updateVariables` ran before dispatch). Path ids may
 * be `PTR_DEREFERENCE` indirections.
 */
internal fun DrawScope.drawTweenPathOp(
    op: DrawTweenPath,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val path1Id = derefId(op.path1Id, read)
    val path2Id = derefId(op.path2Id, read)
    val path =
        remoteContext.mRemoteComposeState.getTweenPath(
            path1Id,
            path2Id,
            op.outTween,
            op.outStart,
            op.outStop,
        )
    val style = paintState.toDrawStyle()
    val brush = paintState.brush
    if (brush != null) {
        drawPath(
            path = path,
            brush = brush,
            alpha = paintState.effectiveAlpha(),
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

internal fun DrawScope.applyMatrixTranslate(op: MatrixTranslate, read: RemoteContext) {
    val v1 = resolveFloat(op.value1, op.v1, read)
    val v2 = resolveFloat(op.value2, op.v2, read)
    drawContext.transform.translate(v1, v2)
}

internal fun DrawScope.applyMatrixScale(op: MatrixScale, read: RemoteContext) {
    val x1 = resolveFloat(op.x1Value, op.rawX1, read)
    val y1 = resolveFloat(op.y1Value, op.rawY1, read)
    val x2 = resolveFloat(op.x2Value, op.rawX2, read)
    val y2 = resolveFloat(op.y2Value, op.rawY2, read)
    drawContext.transform.scale(x1, y1, Offset(x2, y2))
}

internal fun DrawScope.applyMatrixRotate(op: MatrixRotate, read: RemoteContext) {
    val v1 = resolveFloat(op.value1, op.v1, read)
    val v2 = resolveFloat(op.value2, op.v2, read)
    val v3 = resolveFloat(op.value3, op.v3, read)
    drawContext.transform.rotate(v1, Offset(v2, v3))
}

internal fun DrawScope.applyMatrixSkew(op: MatrixSkew, read: RemoteContext) {
    val sx = resolveFloat(op.value1, op.v1, read)
    val sy = resolveFloat(op.value2, op.v2, read)
    val skewMatrix =
        Matrix().apply {
            this[1, 0] = sx // SkewX
            this[0, 1] = sy // SkewY
        }
    drawContext.transform.transform(skewMatrix)
}

/**
 * Imperative clip on the underlying canvas (bounded by the surrounding `MATRIX_SAVE`/`RESTORE` the
 * document emits, like the other canvas-state ops).
 */
internal fun DrawScope.applyClipRect(op: ClipRect, read: RemoteContext) {
    val x1 = resolveFloat(op.x1Value, op.rawX1, read)
    val y1 = resolveFloat(op.y1Value, op.rawY1, read)
    val x2 = resolveFloat(op.x2Value, op.rawX2, read)
    val y2 = resolveFloat(op.y2Value, op.rawY2, read)
    drawContext.canvas.clipRect(x1, y1, x2, y2)
}

/** Map the region op to Compose's [ClipOp] (only Intersect/Difference). */
internal fun DrawScope.applyClipPath(
    op: ClipPath,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val pathId = derefId(op.id, read)
    val path = remoteContext.mRemoteComposeState.getPath(pathId, 0f, 1f)
    val clipOp =
        if (op.regionOp == ClipPath.PATH_CLIP_DIFFERENCE) ClipOp.Difference else ClipOp.Intersect
    drawContext.canvas.clipPath(path, clipOp)
}

internal fun applyPathTween(
    op: PathTween,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val state = remoteContext.mRemoteComposeState
    val path1Id = derefId(op.mPathId1, read)
    val path2Id = derefId(op.mPathId2, read)
    val outId = derefId(op.mOutId, read)
    val tween = op.mTweenOut
    val data1 = state.getPathData(path1Id)
    val data2 = state.getPathData(path2Id)
    val tmp =
        when {
            tween <= 0f || data2 == null -> data1
            tween >= 1f || data1 == null -> data2
            else ->
                FloatArray(data2.size) { i ->
                    if (data1[i].isNaN() || data2[i].isNaN()) data1[i]
                    else (data2[i] - data1[i]) * tween + data1[i]
                }
        }
    if (tmp != null) {
        state.putPathData(outId, tmp)
    }
}

internal fun applyPathCombine(
    op: PathCombine,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val state = remoteContext.mRemoteComposeState
    val path1Id = derefId(op.mPathId1, read)
    val path2Id = derefId(op.mPathId2, read)
    val outId = derefId(op.mOutId, read)
    val p1 = state.getPath(path1Id, 0f, 1f)
    val p2 = state.getPath(path2Id, 0f, 1f)
    val pathOp =
        when (op.operation) {
            PathCombine.OP_DIFFERENCE -> PathOperation.Difference
            PathCombine.OP_INTERSECT -> PathOperation.Intersect
            PathCombine.OP_REVERSE_DIFFERENCE -> PathOperation.ReverseDifference
            PathCombine.OP_UNION -> PathOperation.Union
            PathCombine.OP_XOR -> PathOperation.Xor
            else -> PathOperation.Union
        }
    state.putPath(outId, Path.combine(pathOp, p1, p2))
}
