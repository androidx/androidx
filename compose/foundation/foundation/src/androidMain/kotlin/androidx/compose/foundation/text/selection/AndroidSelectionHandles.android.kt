/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.Handle.SelectionEnd
import androidx.compose.foundation.text.Handle.SelectionStart
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopLeft
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopMiddle
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopRight
import androidx.compose.foundation.text.selection.SelectionHandleAnchor.Left
import androidx.compose.foundation.text.selection.SelectionHandleAnchor.Right
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.ceil

@Composable
internal actual fun SelectionHandle(
    offsetProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    lineHeight: Float,
    modifier: Modifier,
) {
    val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
    // The left selection handle's top right is placed at the given position, and vice versa.
    val handleReferencePoint = if (isLeft) TopRight else TopLeft

    // Propagate the view configuration to the popup.
    val viewConfiguration = LocalViewConfiguration.current
    HandlePopup(positionProvider = offsetProvider, handleReferencePoint = handleReferencePoint) {
        CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
            SelectionHandleIcon(
                modifier = modifier.semantics {
                    val position = offsetProvider.provide()
                    this[SelectionHandleInfoKey] = SelectionHandleInfo(
                        handle = if (isStartHandle) SelectionStart else SelectionEnd,
                        position = position,
                        anchor = if (isLeft) Left else Right,
                        visible = position.isSpecified,
                    )
                },
                iconVisible = { offsetProvider.provide().isSpecified },
                isLeft = isLeft,
            )
        }
    }
}

@Composable
/*@VisibleForTesting*/
internal fun SelectionHandleIcon(
    modifier: Modifier,
    iconVisible: () -> Boolean,
    isLeft: Boolean,
) {
    Spacer(
        modifier
            .size(HandleWidth, HandleHeight)
            .drawSelectionHandle(iconVisible, isLeft)
    )
}

internal fun Modifier.drawSelectionHandle(
    iconVisible: () -> Boolean,
    isLeft: Boolean
): Modifier = composed {
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.drawWithCache {
        val radius = size.width / 2f
        val handleImage = createHandleImage(radius)
        val colorFilter = ColorFilter.tint(handleColor)
        onDrawWithContent {
            drawContent()
            if (!iconVisible()) return@onDrawWithContent
            if (isLeft) {
                // Flip the selection handle horizontally.
                scale(scaleX = -1f, scaleY = 1f) {
                    drawImage(
                        image = handleImage,
                        colorFilter = colorFilter
                    )
                }
            } else {
                drawImage(
                    image = handleImage,
                    colorFilter = colorFilter
                )
            }
        }
    }
}

/**
 * The cache for the image mask created to draw selection/cursor handle, so that we don't need to
 * recreate them.
 */
private object HandleImageCache {
    var imageBitmap: ImageBitmap? = null
    var canvas: Canvas? = null
    var canvasDrawScope: CanvasDrawScope? = null
}

/**
 * Create an image bitmap for the basic shape of a selection handle or cursor handle. It is an
 * circle with a rectangle covering its left top part.
 *
 * To draw the right selection handle, directly draw this image bitmap.
 * To draw the left selection handle, mirror the canvas first and then draw this image bitmap.
 * To draw the cursor handle, translate and rotated the canvas 45 degrees, then draw this image
 * bitmap.
 *
 * @param radius the radius of circle in selection/cursor handle.
 * CanvasDrawScope objects so that we only recreate them when necessary.
 */
internal fun CacheDrawScope.createHandleImage(radius: Float): ImageBitmap {
    // The edge length of the square bounding box of the selection/cursor handle. This is also
    // the size of the bitmap needed for the bitmap mask.
    val edge = ceil(radius).toInt() * 2

    var imageBitmap = HandleImageCache.imageBitmap
    var canvas = HandleImageCache.canvas
    var drawScope = HandleImageCache.canvasDrawScope

    // If the cached bitmap is null or too small, we need to create new bitmap.
    if (
        imageBitmap == null ||
        canvas == null ||
        edge > imageBitmap.width ||
        edge > imageBitmap.height
    ) {
        imageBitmap = ImageBitmap(
            width = edge,
            height = edge,
            config = ImageBitmapConfig.Alpha8
        )
        HandleImageCache.imageBitmap = imageBitmap
        canvas = Canvas(imageBitmap)
        HandleImageCache.canvas = canvas
    }
    if (drawScope == null) {
        drawScope = CanvasDrawScope()
        HandleImageCache.canvasDrawScope = drawScope
    }

    drawScope.draw(
        this,
        layoutDirection,
        canvas,
        Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
    ) {
        // Clear the previously rendered portion within this ImageBitmap as we could
        // be re-using it
        drawRect(
            color = Color.Black,
            size = size,
            blendMode = BlendMode.Clear
        )

        // Draw the rectangle at top left.
        drawRect(
            color = Color(0xFF000000),
            topLeft = Offset.Zero,
            size = Size(radius, radius)
        )
        // Draw the circle
        drawCircle(
            color = Color(0xFF000000),
            radius = radius,
            center = Offset(radius, radius)
        )
    }
    return imageBitmap
}

@Composable
internal fun HandlePopup(
    positionProvider: OffsetProvider,
    handleReferencePoint: HandleReferencePoint,
    content: @Composable () -> Unit
) {
    val popupPositionProvider = remember(handleReferencePoint, positionProvider) {
        HandlePositionProvider(handleReferencePoint, positionProvider)
    }
    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(excludeFromSystemGesture = true, clippingEnabled = false),
        content = content,
    )
}

/**
 * The enum that specifies how a selection/cursor handle is placed to its given position.
 * When this value is [TopLeft], the top left corner of the handle will be placed at the
 * given position.
 * When this value is [TopRight], the top right corner of the handle will be placed at the
 * given position.
 * When this value is [TopMiddle], the handle top edge's middle point will be placed at the given
 * position.
 */
internal enum class HandleReferencePoint {
    TopLeft,
    TopRight,
    TopMiddle
}

/**
 * This [PopupPositionProvider] for [HandlePopup]. It will position the selection handle
 * to the result of [positionProvider] in its anchor layout.
 *
 * @see HandleReferencePoint
 */
internal class HandlePositionProvider(
    private val handleReferencePoint: HandleReferencePoint,
    private val positionProvider: OffsetProvider
) : PopupPositionProvider {

    /**
     * When Handle disappears, it starts reporting its position as [Offset.Unspecified]. Normally,
     * Popup is dismissed immediately when its position becomes unspecified, but for one frame a
     * position update might be requested by soon-to-be-destroyed Popup. In this case, report the
     * last known position as there are no more updates. If the first ever position is provided as
     * unspecified, start with [Offset.Zero] default.
     */
    private var prevPosition: Offset = Offset.Zero

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val position = positionProvider.provide().takeOrElse { prevPosition }
        prevPosition = position

        // We want the cursor to point to the position,
        // so adjust the x-axis based on where the handle is pointing.
        val xAdjustment = when (handleReferencePoint) {
            TopLeft -> 0
            TopMiddle -> popupContentSize.width / 2
            TopRight -> popupContentSize.width
        }

        val offset = position.round()
        val x = anchorBounds.left + offset.x - xAdjustment
        val y = anchorBounds.top + offset.y
        return IntOffset(x, y)
    }
}

/**
 * Computes whether the handle's appearance should be left-pointing or right-pointing.
 */
internal fun isLeft(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean
): Boolean {
    return if (isStartHandle) {
        isHandleLtrDirection(direction, handlesCrossed)
    } else {
        !isHandleLtrDirection(direction, handlesCrossed)
    }
}

/**
 * This method is to check if the selection handles should use the natural Ltr pointing
 * direction.
 * If the context is Ltr and the handles are not crossed, or if the context is Rtl and the handles
 * are crossed, return true.
 *
 * In Ltr context, the start handle should point to the left, and the end handle should point to
 * the right. However, in Rtl context or when handles are crossed, the start handle should point to
 * the right, and the end handle should point to left.
 */
/*@VisibleForTesting*/
internal fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
        direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
