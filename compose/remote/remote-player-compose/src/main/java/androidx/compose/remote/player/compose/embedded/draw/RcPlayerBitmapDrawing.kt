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

import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.DrawBitmap
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapScaled
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.resources.resolveCanvasBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal fun DrawScope.drawBitmapOp(
    op: DrawBitmap,
    paintState: ComposeLocalPaint,
    graph: GraphContext?,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val left = resolveFloat(op.left, op.outputLeft, read)
    val top = resolveFloat(op.top, op.outputTop, read)
    val right = resolveFloat(op.right, op.outputRight, read)
    val bottom = resolveFloat(op.bottom, op.outputBottom, read)

    val bitmap = resolveCanvasBitmap(graph, remoteContext, op.imageId)
    if (bitmap != null) {
        val image = bitmap.asImageBitmap()
        val dstW = right - left
        val dstH = bottom - top
        if (dstW > 0f && dstH > 0f) {
            // Scale the whole bitmap into the [left,top,right,bottom] destination rect, matching
            // DrawBitmap.paint() (the previous code ignored right/bottom and drew at natural size
            // in the top-left).
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(left.toInt(), top.toInt()),
                dstSize = IntSize(dstW.toInt(), dstH.toInt()),
                alpha = paintState.effectiveAlpha(),
                colorFilter = paintState.colorFilter,
                blendMode = paintState.blendMode,
                filterQuality = paintState.filterQuality,
            )
        } else {
            drawImage(
                image = image,
                topLeft = Offset(left, top),
                alpha = paintState.effectiveAlpha(),
                colorFilter = paintState.colorFilter,
                blendMode = paintState.blendMode,
            )
        }
    }
}

internal fun DrawScope.drawBitmapScaledOp(
    op: DrawBitmapScaled,
    paintState: ComposeLocalPaint,
    graph: GraphContext?,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val rawImageId = op.imageId
    val imageId =
        if ((rawImageId and PaintOperation.PTR_DEREFERENCE) != 0) {
            remoteContext.mRemoteComposeState.getInteger(rawImageId and PaintOperation.VALUE_MASK)
        } else {
            rawImageId and PaintOperation.VALUE_MASK
        }
    val bitmap = resolveCanvasBitmap(graph, remoteContext, imageId)
    if (bitmap != null) {
        val srcLeft = resolveFloat(op.srcLeft, op.outSrcLeft, read)
        val srcTop = resolveFloat(op.srcTop, op.outSrcTop, read)
        val srcRight = resolveFloat(op.srcRight, op.outSrcRight, read)
        val srcBottom = resolveFloat(op.srcBottom, op.outSrcBottom, read)
        val dstLeft = resolveFloat(op.dstLeft, op.outDstLeft, read)
        val dstTop = resolveFloat(op.dstTop, op.outDstTop, read)
        val dstRight = resolveFloat(op.dstRight, op.outDstRight, read)
        val dstBottom = resolveFloat(op.dstBottom, op.outDstBottom, read)
        val scaleFactor = resolveFloat(op.scaleFactor, op.outScaleFactor, read)

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
            op.scaleType,
            scaleFactor,
        )
        drawContext.canvas.save()
        drawContext.canvas.clipRect(dstLeft, dstTop, dstRight, dstBottom)
        drawImage(
            image = bitmap.asImageBitmap(),
            srcOffset = IntOffset(srcLeft.toInt(), srcTop.toInt()),
            srcSize = IntSize((srcRight - srcLeft).toInt(), (srcBottom - srcTop).toInt()),
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
            alpha = paintState.effectiveAlpha(),
            colorFilter = paintState.colorFilter,
            blendMode = paintState.blendMode,
            filterQuality = paintState.filterQuality,
        )
        drawContext.canvas.restore()
    }
}

/**
 * Integer-coordinate bitmap blit (src rect -> dst rect); the image id may be a `PTR_DEREFERENCE`
 * indirection. Mirrors `DrawBitmapInt.paint()`.
 */
internal fun DrawScope.drawBitmapIntOp(
    op: DrawBitmapInt,
    paintState: ComposeLocalPaint,
    graph: GraphContext?,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val imageId = derefId(op.imageId, read)
    val bitmap = resolveCanvasBitmap(graph, remoteContext, imageId)
    if (bitmap != null) {
        val srcLeft = op.srcLeft
        val srcTop = op.srcTop
        val srcRight = op.srcRight
        val srcBottom = op.srcBottom
        val dstLeft = op.dstLeft
        val dstTop = op.dstTop
        val dstRight = op.dstRight
        val dstBottom = op.dstBottom
        val dstW = dstRight - dstLeft
        val dstH = dstBottom - dstTop
        if (dstW > 0 && dstH > 0) {
            drawImage(
                image = bitmap.asImageBitmap(),
                srcOffset = IntOffset(srcLeft, srcTop),
                srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                dstOffset = IntOffset(dstLeft, dstTop),
                dstSize = IntSize(dstW, dstH),
                alpha = paintState.effectiveAlpha(),
                colorFilter = paintState.colorFilter,
                blendMode = paintState.blendMode,
                filterQuality = paintState.filterQuality,
            )
        }
    }
}
