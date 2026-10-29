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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.NoOpUpdate
import androidx.web.WebContent
import androidx.web.WebContentView
import androidx.web.WebFeature
import androidx.web.WebSurfaceChromium
import kotlin.math.hypot

/**
 * Creates and retains a [WebContent] instance across configuration changes and transient
 * compositions, automatically [closing][WebContent.close] it when retired.
 *
 * Before calling this composable, verify that [WebFeature.isFeatureSupported] returns `true` for
 * [WebFeature.WEB_CONTENT].
 *
 * @param block configuration block applied to [WebContent.Builder] when creating the [WebContent]
 * @return a retained [WebContent] instance
 * @see WebContent
 * @see WebSurface
 * @see WebFeature.WEB_CONTENT
 */
@Suppress("MissingJvmstatic")
@Composable
@RequiresFeature(
    name = WebFeature.WEB_CONTENT,
    enforcement = "androidx.web.WebFeature#isFeatureSupported",
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun retainWebContent(block: WebContent.Builder.() -> Unit = {}): WebContent {
    val webContent = retain { WebContent(block) }
    RetainedEffect(webContent) {
        onRetire {
            webContent.close()
        }
    }
    return webContent
}

/**
 * Presents the web page hosted by [WebContent].
 *
 * [WebSurface] has no intrinsic size and defaults to the minimum constraints of its parent, so a
 * size should be specified via [modifier] (for example using `Modifier.fillMaxSize()`,
 * `Modifier.size()`, or `Modifier.weight()`).
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
public fun WebSurface(content: WebContent, modifier: Modifier = Modifier) {
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
 * [WebSurface] has no intrinsic size and defaults to the minimum constraints of its parent, so a
 * size should be specified via [modifier] (for example using `Modifier.fillMaxSize()`,
 * `Modifier.size()`, or `Modifier.weight()`).
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
    content: WebContent,
    modifier: Modifier = Modifier,
    bridgeFactory: (Context) -> T,
    bridgeUpdate: (T) -> Unit = NoOpUpdate,
    bridgeRelease: (T) -> Unit = NoOpUpdate,
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
            Modifier.onKeyEvent { state.node?.dispatchKeyEvent(it.nativeKeyEvent) == true }
                .focusProperties {
                    canFocus = false
                    onExit = { state.node?.onExitFocus() }
                }
                .focusTarget()
                .then(modifier)
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

// TODO: Expose scroll position and programmatic scroll control on WebSurface / WebContent instead
// of relying on View scroll APIs.
internal class WebSurfaceNode<T : WebViewBridge>(private var state: WebSurfaceState<T>) :
    DelegatingNode(),
    DrawModifierNode,
    PointerInputModifierNode,
    GlobalPositionAwareModifierNode,
    ObserverModifierNode,
    SemanticsModifierNode,
    CompositionLocalConsumerModifierNode,
    ViewTreeObserver.OnGlobalFocusChangeListener,
    WebSurfaceChromium.EventListener {

    private var webSurface: WebSurfaceChromium? = null
    private var size = IntSize.Zero
    private var holder: HeadlessViewHolder? = null
    private var pinnedHandle: PinnableContainer.PinnedHandle? = null
    private var attachedViewTreeObserver: ViewTreeObserver? = null

    /** Prevents reentrant focus synchronization between Compose and the underlying View tree. */
    private var isUpdatingFocus: Boolean = false

    // If Chromium invalidates during draw (e.g. computeScroll), Compose ignores
    // the synchronous invalidateDraw() call because the layer's dirty flag is
    // cleared at the end of draw. So we just track if we need to validate
    // again.
    private var isDrawing: Boolean = false

    /** Only changed through [transitionTo]. */
    private var touchState: TouchState = TouchState.Idle

    // Where the surface was in the root when the gesture started. Touch events come in root
    // coordinates and we translate them by this for the whole gesture, so a parent moving the
    // surface mid-gesture (e.g. a LazyColumn scrolling from our overscroll) doesn't look like
    // finger movement to Chromium.
    private var touchOriginX: Float = 0f
    private var touchOriginY: Float = 0f

    /** Where the gesture started in root coordinates, for the touch slop check. */
    private var touchDownX: Float = 0f
    private var touchDownY: Float = 0f

    /**
     * Whether the page takes part in nested scrolling, i.e. gets the first claim on drags and
     * passes what it didn't use to parents. Read from the bridge, so apps opt out with
     * [View.setNestedScrollingEnabled] as they would for a View child, see
     * [WebViewBridge.setNestedScrollingEnabled].
     */
    private val nestedScrollEnabled: Boolean
        get() = state.bridge?.isNestedScrollingEnabled != false

    private val focusTargetNode =
        delegate(
            FocusTargetModifierNode(
                focusability = Focusability.Always,
                onFocusChange = ::onFocusStateChange,
            )
        )

    /** Connects our drags to nested scroll parents. */
    private val nestedScrollHandover = delegate(NestedScrollHandoverNode())

    override fun onInvalidate() {
        if (!isAttached) return
        if (isDrawing) {
            requireView().postOnAnimation(::onInvalidate)
        } else {
            invalidateDraw()
        }
    }

    // Chromium reports what the page didn't scroll, which nestedScrollHandover passes on to
    // parents. With nested scrolling disabled on the bridge, parents take drags themselves, so this
    // is dropped.
    override fun onOverScroll(deltaX: Float, deltaY: Float, velocityX: Float, velocityY: Float) {
        if (!isAttached || !nestedScrollEnabled) return
        nestedScrollHandover.onPageOverScroll(
            delta = Offset(deltaX, deltaY),
            velocity = Velocity(velocityX, velocityY),
            isDragging = touchState == TouchState.Dragging,
        )
    }

    private fun runUpdate() {
        val currentBridge = state.bridge ?: return
        if (!isAttached) return
        observeReads { state.bridgeUpdate(currentBridge) }
    }

    override fun onObservedReadsChanged() {
        runUpdate()
    }

    /**
     * Requesting or clearing focus on either Compose or the View will synchronously trigger a focus
     * change callback on the other. We use [isUpdatingFocus] so they don't keep calling back into
     * each other in a loop.
     */
    private inline fun focusUpdate(block: () -> Unit) {
        if (isUpdatingFocus) return
        isUpdatingFocus = true
        try {
            block()
        } finally {
            isUpdatingFocus = false
        }
    }

    private fun onFocusStateChange(previousState: FocusState, currentState: FocusState) {
        if (!isAttached) return
        val isFocused = currentState.isFocused
        if (isFocused == previousState.isFocused) return
        // Pin this item in lazy layouts (e.g. LazyColumn) while focused, matching
        // Modifier.focusable(). Unfocused WebSurfaces are disposed offscreen as normal, but
        // disposing a focused WebSurface would detach HeadlessViewHolder from the window, which
        // tears down any active IME InputConnection, closes the soft keyboard, and drops focus.
        pinnedHandle?.release()
        pinnedHandle = if (isFocused) currentValueOf(LocalPinnableContainer)?.pin() else null
        if (isUpdatingFocus || !isFocused) return
        val view = holder?.view ?: return
        focusUpdate {
            if (!view.hasFocus()) {
                view.requestFocus()
            }
            if (view.hasFocus() && !focusTargetNode.focusState.isFocused) {
                requestFocusWithDescendantPriority()
            }
        }
    }

    fun onExitFocus() {
        currentValueOf(LocalFocusManager).clearFocus(force = true)
    }

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        if (!isAttached || isUpdatingFocus) return
        val currentHolder = holder ?: return
        val subViewLostFocus =
            oldFocus != null && (oldFocus === currentHolder || oldFocus.parent === currentHolder)
        val subViewGotFocus =
            newFocus != null && (newFocus === currentHolder || newFocus.parent === currentHolder)
        if (subViewGotFocus && !subViewLostFocus) {
            if (!focusTargetNode.focusState.isFocused) {
                requestFocusWithDescendantPriority()
            }
        } else if (subViewLostFocus && !subViewGotFocus) {
            if (focusTargetNode.focusState.isFocused && !requireView().hasFocus()) {
                currentValueOf(LocalFocusManager).clearFocus(force = false)
            }
        }
    }

    private fun requestFocusWithDescendantPriority() {
        val hostGroup = requireView() as? ViewGroup
        val previousFocusability =
            hostGroup?.descendantFocusability ?: ViewGroup.FOCUS_BEFORE_DESCENDANTS
        hostGroup?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        try {
            focusTargetNode.requestFocus()
        } finally {
            hostGroup?.descendantFocusability = previousFocusability
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        holder?.let { this[WebSurfaceHolderKey] = it }
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean = holder?.dispatchKeyEventToView(event) == true

    fun onBridgeAttached(bridge: T) {
        if (!isAttached) return
        holder?.view = bridge
        if (focusTargetNode.focusState.isFocused) bridge.requestFocus()
        createSurface()
        runUpdate()
        invalidateDraw()
    }

    fun onBridgeDetached() {
        pinnedHandle?.release()
        pinnedHandle = null
        releaseSurface()
        focusUpdate { holder?.view = null }
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
        val hostView = requireView()
        attachedViewTreeObserver =
            hostView.viewTreeObserver.also { it.addOnGlobalFocusChangeListener(this) }
        (hostView as? ViewGroup)?.let { host ->
            installHostAccessibilityDelegate(host)
            // Add ahead of AndroidViewsHandler so FOCUS_AFTER_DESCENDANTS checks holders first.
            holder = HeadlessViewHolder(host.context, this).also { host.addView(it, 0) }
        }
        state.bridge?.let { onBridgeAttached(it) }
    }

    override fun onDetach() {
        if (focusTargetNode.focusState.isFocused) {
            onExitFocus()
        }
        attachedViewTreeObserver?.takeIf { it.isAlive }?.removeOnGlobalFocusChangeListener(this)
        attachedViewTreeObserver = null
        onBridgeDetached()
        if (state.node === this) {
            state.node = null
        }
        (requireView() as? ViewGroup)?.removeView(holder)
        holder = null
        super.onDetach()
    }

    private fun createSurface() {
        releaseSurface()
        webSurface =
            WebSurfaceChromium.create(this).also {
                it.setSize(size.width, size.height)
                it.setWebContent(state.content)
            }
    }

    private fun releaseSurface() {
        val surface = webSurface ?: return
        webSurface = null
        transitionTo(TouchState.Idle)
        surface.destroy()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // This runs on every layout pass that moves the surface, e.g. each frame of a parent
        // scroll. Resizing the page is expensive (Chromium relayouts and draws a new frame), so
        // only do it when the size actually changed; moving just updates the View's position.
        if (coordinates.size != size) {
            size = coordinates.size
            webSurface?.setSize(size.width, size.height)
        }
        // The holder is new after every attach, so it can be unsized even when our size didn't
        // change, e.g. when this node is reused in a lazy list.
        holder?.let {
            if (it.width != size.width || it.height != size.height) {
                it.updateSize(size.width, size.height)
            }
        }
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
        // It takes drags for parents in the Initial pass, before we forward events to Chromium.
        nestedScrollHandover.onPointerEvent(pointerEvent, pass)
        val motionEvent = pointerEvent.motionEvent ?: return
        if (motionEvent.isTouchAction) {
            if (onTouchEvent(motionEvent, pass, consumed = pointerEvent.isConsumed)) {
                pointerEvent.consume()
            }
        } else if (pass == PointerEventPass.Main && !pointerEvent.isConsumed) {
            dispatchGenericMotionEvent(motionEvent, pointerEvent)
        }
    }

    override fun onCancelPointerInput() {
        nestedScrollHandover.onCancelPointerInput()
        cancelTouch()
    }

    /**
     * Advances [touchState] and forwards the event to Chromium if the current state handles this
     * [pass]. Returns true if Chromium handled it, in which case the caller consumes it. [consumed]
     * is whether another node already consumed the event.
     */
    private fun onTouchEvent(
        motionEvent: MotionEvent,
        pass: PointerEventPass,
        consumed: Boolean,
    ): Boolean {
        if (!consumed) {
            when (touchState) {
                TouchState.Idle ->
                    if (
                        pass == PointerEventPass.Main &&
                            motionEvent.actionMasked == MotionEvent.ACTION_DOWN
                    ) {
                        return startTouch(motionEvent)
                    }
                TouchState.Touching ->
                    if (
                        pass == PointerEventPass.Main &&
                            motionEvent.actionMasked == MotionEvent.ACTION_MOVE &&
                            takesGesture(motionEvent)
                    ) {
                        transitionTo(TouchState.Dragging)
                    }
                TouchState.Dragging -> {}
            }
        }
        // startTouch already forwarded the down in the Main pass.
        if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) return false
        if (pass != touchState.activePass) return false
        if (consumed) {
            cancelTouch()
            return false
        }
        return forwardTouchEvent(motionEvent)
    }

    /**
     * Starts a gesture with [motionEvent], which is a down, and forwards it to Chromium. Returns
     * true if Chromium handled it, in which case the caller consumes it.
     */
    private fun startTouch(motionEvent: MotionEvent): Boolean {
        val position = requireLayoutCoordinates().positionInRoot()
        touchOriginX = position.x
        touchOriginY = position.y
        touchDownX = motionEvent.x
        touchDownY = motionEvent.y
        holder?.run {
            // ViewGroup only clears a request not to intercept when it dispatches a down, which
            // the bridge never gets, so clear it ourselves.
            view?.requestDisallowInterceptTouchEvent(false)
            disallowIntercept = false
        }
        transitionTo(TouchState.Touching)
        return forwardTouchEvent(motionEvent)
    }

    /** Forwards [motionEvent] to Chromium and returns true if Chromium handled it. */
    private fun forwardTouchEvent(motionEvent: MotionEvent): Boolean {
        val handled =
            motionEvent.withOffset(-touchOriginX, -touchOriginY) {
                webSurface?.onTouchEvent(it) == true
            }
        if (!handled || motionEvent.isTouchEnd) {
            transitionTo(TouchState.Idle)
        }
        return handled
    }

    /** Cancels the gesture in Chromium, if there is one. */
    private fun cancelTouch() {
        if (touchState == TouchState.Idle) return
        transitionTo(TouchState.Idle)
        nestedScrollHandover.onPageTouchCancelled()
        val now = SystemClock.uptimeMillis()
        val cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        webSurface?.onTouchEvent(cancelEvent)
        cancelEvent.recycle()
    }

    /** The only place [touchState] changes, so leaving [TouchState.Dragging] always ends it. */
    private fun transitionTo(newState: TouchState) {
        val oldState = touchState
        if (oldState == newState) return
        touchState = newState
        if (oldState == TouchState.Dragging) {
            nestedScrollHandover.onDragEnded()
        }
    }

    /**
     * Whether we take the gesture from the Compose gesture detectors around us. Like a child View,
     * we do as soon as Chromium asks its parents not to intercept, e.g. for a text selection drag.
     * With nested scrolling enabled we also do once the finger moves past touch slop.
     */
    private fun takesGesture(motionEvent: MotionEvent): Boolean =
        holder?.disallowIntercept == true || (nestedScrollEnabled && exceedsTouchSlop(motionEvent))

    private fun exceedsTouchSlop(motionEvent: MotionEvent): Boolean {
        val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        return hypot(motionEvent.x - touchDownX, motionEvent.y - touchDownY) >= touchSlop
    }

    private fun dispatchGenericMotionEvent(motionEvent: MotionEvent, pointerEvent: PointerEvent) {
        val offset = requireLayoutCoordinates().positionInRoot()
        val handled =
            motionEvent.withOffset(-offset.x, -offset.y) {
                holder?.dispatchGenericMotionEventToView(it) == true
            }
        if (handled) {
            pointerEvent.consume()
        }
    }

    /**
     * Decides in which pass we forward touch events to Chromium, i.e. before or after the Compose
     * gesture detectors around us see them.
     *
     * Chromium recognizes gestures asynchronously, so it can't tell us when to take over a gesture
     * from Compose. We do what a scrollable does instead and take over once the finger moves past
     * touch slop. With nested scrolling disabled on the bridge we leave drags to our parents like a
     * View inside a ScrollView. Either way, like a child View, we take over as soon as Chromium
     * asks its parents not to intercept, see [takesGesture].
     */
    private enum class TouchState(val activePass: PointerEventPass?) {
        /** No finger down. */
        Idle(activePass = null),

        /**
         * Finger down, but the gesture isn't ours yet. We take the down in the Main pass, before
         * click handlers on us or our ancestors see it, so the page gets taps and long presses like
         * a child View does. We forward the rest in the Final pass, after the Compose gesture
         * detectors, so one that takes the gesture before we do still can, in which case Chromium
         * gets a cancel. With nested scrolling disabled we only leave this state if Chromium asks
         * for the gesture, so a parent can take drags at any point and the page gets whatever
         * nobody takes.
         */
        Touching(activePass = PointerEventPass.Final),

        /**
         * The gesture is ours: Chromium asked for it, or the finger moved past touch slop with
         * nested scrolling enabled and nobody else taking the gesture. We forward and consume in
         * the Main pass, before parents see the events, like a scrollable. Leaving this state ends
         * the nested scroll.
         */
        Dragging(activePass = PointerEventPass.Main),
    }
}

internal val PointerEvent.isConsumed: Boolean
    get() = changes.fastAny { it.isConsumed }

internal fun PointerEvent.consume() {
    changes.fastForEach { it.consume() }
}

private val MotionEvent.isTouchAction: Boolean
    get() =
        when (actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_OUTSIDE,
            MotionEvent.ACTION_CANCEL -> true
            else -> false
        }

private val MotionEvent.isTouchEnd: Boolean
    get() = actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL

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
