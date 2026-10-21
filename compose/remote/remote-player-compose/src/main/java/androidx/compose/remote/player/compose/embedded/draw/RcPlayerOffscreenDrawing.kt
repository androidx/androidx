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

import android.graphics.Bitmap
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.player.compose.embedded.resources.resolveBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer

/**
 * The stack of draw targets redirected by `DRAW_TO_BITMAP` during a single draw pass.
 *
 * This is transient draw-phase state, not composition state: it is created per [executeOperations]
 * call (callers may pass one in to observe [isDrawingToOffscreen]), threaded through nested
 * operation lists, and unwound by the end of the pass. It must not be remembered or shared across
 * frames.
 */
internal class OffscreenDrawState {
    private class SavedTarget(
        val canvas: Canvas,
        val graphicsLayer: GraphicsLayer?,
        val alpha: Float,
    )

    private val stack = ArrayList<SavedTarget>()

    /** True while draw operations are redirected to an offscreen bitmap. */
    val isDrawingToOffscreen: Boolean
        get() = stack.isNotEmpty()

    /** The number of redirected targets currently on the stack. */
    val depth: Int
        get() = stack.size

    /**
     * Redirects [drawScope] to [target], saving the current canvas, graphics layer, and paint alpha
     * so [pop] can restore them. The paint alpha is reset so offscreen content is drawn opaque;
     * it's applied again when the bitmap is drawn back on screen.
     */
    fun push(drawScope: DrawScope, paintState: ComposeLocalPaint, target: Canvas) {
        val drawContext = drawScope.drawContext
        stack.add(SavedTarget(drawContext.canvas, drawContext.graphicsLayer, paintState.alpha))
        drawContext.canvas = target
        drawContext.graphicsLayer = null
        paintState.alpha = 1f
    }

    /** Restores the target saved by the matching [push]. No-op if the stack is empty. */
    fun pop(drawScope: DrawScope, paintState: ComposeLocalPaint) {
        val saved = stack.removeLastOrNull() ?: return
        val drawContext = drawScope.drawContext
        drawContext.canvas = saved.canvas
        drawContext.graphicsLayer = saved.graphicsLayer
        paintState.alpha = saved.alpha
    }

    /** Pops until [depth] is at most [targetDepth]. */
    fun popTo(targetDepth: Int, drawScope: DrawScope, paintState: ComposeLocalPaint) {
        while (stack.size > targetDepth) pop(drawScope, paintState)
    }
}

/**
 * Offscreen render target (`DRAW_TO_BITMAP`): redirects the [DrawScope]'s canvas to one backed by
 * the target bitmap so subsequent draw ops accumulate there; `bitmapId == 0` restores the previous
 * canvas from the stack (mirrors `DrawToBitmap.paint()`).
 */
internal fun DrawScope.drawToBitmapOp(
    op: DrawToBitmap,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext,
    offscreenState: OffscreenDrawState,
) {
    val bitmapId = derefId(op.bitmapId, read)
    val mode = op.mode
    val color = op.color
    if (bitmapId == 0) {
        offscreenState.pop(this, paintState)
    } else {
        if ((mode and DrawToBitmap.MODE_COMPONENT_ID) != 0) {
            var compId = color
            val resolvedInt = read.getInteger(color)
            if (resolvedInt != 0) {
                compId = resolvedInt
            } else {
                val resolvedFloat = read.getFloat(color)
                if (!resolvedFloat.isNaN() && resolvedFloat != 0f) {
                    compId = resolvedFloat.toInt()
                }
            }
            val targetComp =
                remoteContext.document?.getComponent(compId) ?: remoteContext.mLastComponent
            val bd = remoteContext.getObject(bitmapId) as? BitmapData
            if (bd != null) {
                val prevLast = remoteContext.mLastComponent
                remoteContext.mLastComponent = targetComp
                bd.ensureOffscreenBitmap(remoteContext)
                remoteContext.mLastComponent = prevLast
            }
        } else {
            val bd = remoteContext.getObject(bitmapId) as? BitmapData
            bd?.ensureOffscreenBitmap(remoteContext)
        }
        val stored = resolveBitmap(remoteContext, bitmapId)
        if (stored != null) {
            // The target must be a mutable bitmap to back a Canvas. Decoded document bitmaps are
            // immutable, so draw into a mutable copy and store it back under the same id, so a
            // later DRAW_BITMAP of this id reads the rendered content.
            val target =
                if (stored.isMutable) {
                    stored
                } else {
                    stored.copy(Bitmap.Config.ARGB_8888, true).also {
                        remoteContext.mRemoteComposeState.cacheData(bitmapId, it)
                    }
                }
            val initColor = if ((mode and DrawToBitmap.MODE_COMPONENT_ID) != 0) 0 else color
            if ((mode and DrawToBitmap.MODE_NO_INITIALIZE) == 0) {
                target.eraseColor(initColor)
            }
            offscreenState.push(this, paintState, Canvas(target.asImageBitmap()))
        }
    }
}

/**
 * Draws the component's content (`DRAW_CONTENT`), applying the current paint's alpha, color filter
 * and blend mode to it as a group via [contentLayer] (mirroring the View player's saveLayer).
 *
 * The content is drawn directly when no group effect is set, when there is no [contentLayer] (the
 * root and canvas draw passes, where [onDrawContent] is a no-op), or while drawing to an offscreen
 * bitmap, where a hardware [GraphicsLayer] can't be drawn and the effect is instead applied when
 * the bitmap is drawn back.
 */
internal fun DrawScope.drawContentWithPaint(
    paintState: ComposeLocalPaint,
    contentLayer: GraphicsLayer?,
    isDrawingToOffscreen: Boolean,
    contentOrigin: Offset,
    onDrawContent: () -> Unit,
) {
    if (isDrawingToOffscreen) {
        onDrawContent()
        return
    }
    val hasGroupEffect =
        paintState.alpha < 1f || paintState.colorFilter != null || paintState.isBlendModeSet
    if (contentLayer == null || !hasGroupEffect) {
        if (contentOrigin != Offset.Zero) {
            drawContext.canvas.save()
            drawContext.canvas.translate(contentOrigin.x, contentOrigin.y)
            try {
                onDrawContent()
            } finally {
                drawContext.canvas.restore()
            }
        } else {
            onDrawContent()
        }
        return
    }
    contentLayer.alpha = paintState.alpha
    contentLayer.blendMode = paintState.blendMode
    contentLayer.colorFilter = paintState.colorFilter
    contentLayer.record { onDrawContent() }
    if (contentOrigin != Offset.Zero) {
        drawContext.canvas.save()
        drawContext.canvas.translate(contentOrigin.x, contentOrigin.y)
        try {
            drawLayer(contentLayer)
        } finally {
            drawContext.canvas.restore()
        }
    } else {
        drawLayer(contentLayer)
    }
}
