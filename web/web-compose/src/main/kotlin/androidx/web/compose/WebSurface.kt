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

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
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
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.NoOpUpdate
import androidx.web.WebContent
import androidx.web.WebContentView
import androidx.web.WebFeature
import androidx.web.WebSurfaceChromium

/**
 * Presents the web page hosted by [WebContent].
 *
 * Before calling this composable, verify that [WebFeature.isFeatureSupported] returns `true` for
 * [WebFeature.WEB_SURFACE].
 *
 * @param content The [WebContent] hosting the web session to present.
 * @param modifier The [Modifier] to be applied to this layout node.
 * @throws IllegalStateException if [content] is already attached to a [WebContentView] or another
 *   [WebSurface].
 * @see WebContent
 * @see WebFeature.WEB_SURFACE
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
    WebSurface(
        content = content,
        modifier = modifier,
        bridgeFactory = ::WebViewBridge,
    )
}

/**
 * Presents the web page hosted by [WebContent], using a [WebViewBridge] to access
 * [android.webkit.WebView] APIs that are not yet available directly on [WebContent].
 *
 * When this composable enters composition, [bridgeFactory] is invoked on the UI thread to create
 * and attach a [WebViewBridge] to [content]. The provided [Context] _must_ be used to construct the
 * [WebViewBridge]. In addition to creating the [WebViewBridge], the [bridgeFactory] block can also
 * be used to perform one-off initializations and set constant properties. The [bridgeUpdate] block
 * can run multiple times (on the UI thread as well) due to recomposition, and it is the right place
 * to set new properties or trigger state-driven updates on the [WebViewBridge]. Note that
 * [bridgeUpdate] will also run once right after the [bridgeFactory] block completes. When this
 * composable leaves composition, [bridgeRelease] is invoked and the [WebViewBridge] is
 * automatically detached from [content].
 *
 * Before calling this composable, verify that [WebFeature.isFeatureSupported] returns `true` for
 * [WebFeature.WEB_SURFACE].
 *
 * @param content The [WebContent] hosting the web session to present.
 * @param modifier The [Modifier] to be applied to this layout node.
 * @param bridgeFactory The block creating the [WebViewBridge] to be attached to [content]. This
 *   block is called once when the composable enters composition, or if the [content] instance
 *   changes. The provided [Context] must be passed to the [WebViewBridge] constructor.
 * @param bridgeUpdate A callback to be invoked after the [WebViewBridge] is created and upon
 *   recomposition to update the information and state of the [WebViewBridge].
 * @param bridgeRelease A callback invoked as a signal that the [WebViewBridge] instance has exited
 *   the composition hierarchy entirely and is being detached from [content]. Any external
 *   references or resources tied to the [WebViewBridge] should be freed at this time.
 * @throws IllegalStateException if [content] is already attached to a [WebContentView] or another
 *   [WebSurface].
 * @see WebContent
 * @see WebViewBridge
 * @see WebFeature.WEB_SURFACE
 */
@Suppress("MissingJvmstatic")
@Composable
@RequiresFeature(
    name = WebFeature.WEB_SURFACE,
    enforcement = "androidx.web.WebFeature#isFeatureSupported",
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : WebViewBridge> WebSurface(
    @NonNull content: WebContent,
    modifier: Modifier = Modifier,
    @NonNull bridgeFactory: (Context) -> T,
    @NonNull bridgeUpdate: (T) -> Unit = NoOpUpdate,
    @NonNull bridgeRelease: (T) -> Unit = NoOpUpdate,
) {
    val context = LocalContext.current
    val state =
        remember(content, context) {
            WebSurfaceState(
                content = content,
                context = context,
                bridgeFactory = bridgeFactory,
                bridgeUpdate = bridgeUpdate,
                bridgeRelease = bridgeRelease,
            )
        }
    Layout(
        modifier =
            modifier
                .clipToBounds()
                .then(
                    WebSurfaceElement(
                        state = state,
                        bridgeFactory = bridgeFactory,
                        bridgeUpdate = bridgeUpdate,
                        bridgeRelease = bridgeRelease,
                    )
                )
    ) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

internal class WebSurfaceState<T : WebViewBridge>(
    val content: WebContent,
    val context: Context,
    var bridgeFactory: (Context) -> T,
    var bridgeUpdate: (T) -> Unit,
    var bridgeRelease: (T) -> Unit,
) : RememberObserver {
    var bridge: T? = null
        private set

    var node: WebSurfaceNode<T>? = null

    override fun onRemembered() {
        check(!content.isAttached()) {
            "WebContent is already attached to a WebContentView or another WebSurface."
        }
        val createdBridge = content.attach(context, bridgeFactory)
        bridge = createdBridge
        node?.onBridgeAttached(createdBridge)
    }

    override fun onForgotten() {
        val currentBridge = bridge ?: return
        bridge = null
        node?.onBridgeDetached()
        node = null
        (currentBridge.parent as? ViewGroup)?.removeView(currentBridge)
        bridgeRelease(currentBridge)
        content.detach()
    }

    override fun onAbandoned() {
        // Nothing to do as [onRemembered] was not called.
    }
}

internal class WebSurfaceElement<T : WebViewBridge>(
    val state: WebSurfaceState<T>,
    val bridgeFactory: (Context) -> T,
    val bridgeUpdate: (T) -> Unit,
    val bridgeRelease: (T) -> Unit,
) : ModifierNodeElement<WebSurfaceNode<T>>() {

    override fun create(): WebSurfaceNode<T> = WebSurfaceNode(state)

    override fun update(node: WebSurfaceNode<T>) {
        node.update(state, bridgeFactory, bridgeUpdate, bridgeRelease)
    }

    override fun InspectorInfo.inspectableProperties() {
        // Internal modifier element; parameters are already exposed on the WebSurface composable.
        // We will fail lint checks if we don't override this.
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSurfaceElement<*>) return false
        return state === other.state &&
            bridgeFactory === other.bridgeFactory &&
            bridgeUpdate === other.bridgeUpdate &&
            bridgeRelease === other.bridgeRelease
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + bridgeFactory.hashCode()
        result = 31 * result + bridgeUpdate.hashCode()
        result = 31 * result + bridgeRelease.hashCode()
        return result
    }
}

// TODO: Integrate Chromium's AccessibilityNodeProvider with Compose's semantics and accessibility
// tree so screen readers and UI automation can inspect and interact with WebSurface content.
// TODO: Expose scroll position and programmatic scroll control on WebSurface / WebContent instead
// of relying on View scroll APIs.
internal class WebSurfaceNode<T : WebViewBridge>(private var state: WebSurfaceState<T>) :
    Modifier.Node(),
    DrawModifierNode,
    PointerInputModifierNode,
    GlobalPositionAwareModifierNode,
    ObserverModifierNode {

    private var webSurface: WebSurfaceChromium? = null
    private var holder: HeadlessViewHolder? = null

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

    private fun runUpdate() {
        val currentBridge = state.bridge ?: return
        if (!isAttached) return
        observeReads { state.bridgeUpdate(currentBridge) }
    }

    override fun onObservedReadsChanged() {
        runUpdate()
    }

    fun onBridgeAttached(bridge: T) {
        if (!isAttached) return
        holder?.view = bridge
        webSurface?.setWebContent(state.content)
        runUpdate()
        invalidateDraw()
    }

    fun onBridgeDetached() {
        webSurface?.setWebContent(null)
        holder?.view = null
    }

    fun update(
        newState: WebSurfaceState<T>,
        newBridgeFactory: (Context) -> T,
        newBridgeUpdate: (T) -> Unit,
        newBridgeRelease: (T) -> Unit,
    ) {
        newState.bridgeFactory = newBridgeFactory
        newState.bridgeUpdate = newBridgeUpdate
        newState.bridgeRelease = newBridgeRelease
        if (state !== newState) {
            if (state.node === this) {
                state.node = null
            }
            state = newState
            state.node = this
            val currentBridge = newState.bridge
            if (currentBridge != null) {
                onBridgeAttached(currentBridge)
            } else {
                onBridgeDetached()
            }
        } else {
            runUpdate()
            if (isAttached) {
                invalidateDraw()
            }
        }
    }

    override fun onAttach() {
        super.onAttach()
        state.node = this
        (requireView() as? ViewGroup)?.let { host ->
            holder = HeadlessViewHolder(host.context).also { host.addView(it) }
        }
        // TODO: Ensure WebSurface uses a transparent base background color by default so Compose
        // Modifier.background on WebSurface or parent layouts is not covered by WebView's default
        // white background.
        webSurface = WebSurfaceChromium.create(::onInvalidate)
        state.bridge?.let { onBridgeAttached(it) }
    }

    override fun onDetach() {
        webSurface?.setWebContent(null)
        holder?.view = null
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
                    // TODO: Integrate WebSurfaceNode with Compose's focus system
                    // (FocusTargetModifierNode) so Modifier.onFocusChanged and FocusRequester work
                    // natively on WebSurface.
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
