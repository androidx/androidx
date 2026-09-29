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

package androidx.web.compose

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.web.WebContent
import androidx.web.WebContentView
import androidx.web.WebFeature
import androidx.web.WebSurfaceChromium

/**
 * A composable that renders [WebContent] directly into the hardware canvas via Chromium's draw
 * functor without intermediate View hierarchy nodes.
 *
 * @param content The [WebContent] instance to render.
 * @param modifier The modifier to be applied to the layout.
 */
@Suppress("MissingJvmstatic")
@Composable
@RequiresFeature(
    name = WebFeature.WEB_SURFACE,
    enforcement = "androidx.web.WebFeature#isFeatureSupported",
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WebSurface(
    @NonNull content: WebContent,
    modifier: Modifier = Modifier,
) {
    Layout(modifier = modifier.clipToBounds().then(WebSurfaceElement(content))) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

internal data class WebSurfaceElement(val content: WebContent) :
    ModifierNodeElement<WebSurfaceNode>() {

    override fun create(): WebSurfaceNode = WebSurfaceNode(content)

    override fun update(node: WebSurfaceNode) {
        node.update(content)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "webSurface"
        properties["content"] = content
    }
}

internal class WebSurfaceNode(private var content: WebContent) :
    Modifier.Node(), DrawModifierNode, PointerInputModifierNode, GlobalPositionAwareModifierNode {

    private var webSurface: WebSurfaceChromium? = null
    private var holder: HeadlessViewHolder? = null
    private val viewListener: (WebContentView?) -> Unit = { holder?.view = it }

    // If Chromium invalidates during draw (e.g. computeScroll), Compose ignores
    // the synchronous invalidateDraw() call because the layer's dirty flag is
    // cleared at the end of draw. So we just track if we need to validate
    // again.
    private var isDrawing: Boolean = false

    private fun onInvalidate() {
        if (!isAttached) return
        if (isDrawing) {
            requireView().postOnAnimation(::onInvalidate)
        } else {
            invalidateDraw()
        }
    }

    fun update(newContent: WebContent) {
        if (content != newContent) {
            if (isAttached) {
                content.setCurrentViewListener(null)
            }
            content = newContent
            if (isAttached) {
                content.setCurrentViewListener(viewListener)
                webSurface?.setWebContent(newContent)
            }
        }
        if (isAttached) {
            invalidateDraw()
        }
    }

    override fun onAttach() {
        super.onAttach()
        (requireView() as? ViewGroup)?.let { host ->
            holder = HeadlessViewHolder(host.context).also { host.addView(it) }
        }
        val surface = WebSurfaceChromium.create(::onInvalidate).also { webSurface = it }
        content.setCurrentViewListener(viewListener)
        surface.setWebContent(content)
        invalidateDraw()
    }

    override fun onDetach() {
        content.setCurrentViewListener(null)
        webSurface?.setWebContent(null)
        (requireView() as? ViewGroup)?.removeView(holder)
        holder = null
        webSurface?.destroy()
        webSurface = null
        super.onDetach()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val size = coordinates.size
        holder?.updateSize(size.width, size.height)
        webSurface?.setSize(size.width, size.height)
        val position = coordinates.positionInRoot()
        holder?.translationX = position.x
        holder?.translationY = position.y
    }

    override fun ContentDrawScope.draw() {
        isDrawing = true
        try {
            drawIntoCanvas { canvas ->
                webSurface?.draw(canvas.nativeCanvas)
            }
        } finally {
            isDrawing = false
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        pointerEvent.motionEvent
            ?.takeIf { pass == PointerEventPass.Main }
            ?.let { motionEvent ->
                if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                    val view = holder?.view
                    if (view != null && !view.hasFocus()) {
                        view.requestFocus()
                    }
                }
                val offset = requireLayoutCoordinates().positionInRoot()
                val consumed =
                    motionEvent.withOffset(-offset.x, -offset.y) {
                        webSurface?.onTouchEvent(it) == true
                    }
                pointerEvent.changes.takeIf { consumed }?.fastForEach { it.consume() }
            }
    }

    override fun onCancelPointerInput() {
        val now = SystemClock.uptimeMillis()
        val cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        webSurface?.onTouchEvent(cancelEvent)
        cancelEvent.recycle()
    }
}

private inline fun MotionEvent.withOffset(
    dx: Float,
    dy: Float,
    block: (MotionEvent) -> Boolean,
): Boolean {
    offsetLocation(dx, dy)
    return try {
        block(this)
    } finally {
        offsetLocation(-dx, -dy)
    }
}
