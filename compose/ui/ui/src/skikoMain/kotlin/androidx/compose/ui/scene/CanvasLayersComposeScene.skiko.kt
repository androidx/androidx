/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.RootNodeOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.window.getDialogScrimBlendMode
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * Constructs a multi-layer [ComposeScene] using the specified parameters. Unlike
 * [PlatformLayersComposeScene], this version doesn't employ [ComposeSceneContext.createPlatformLayer]
 * to position a new [LayoutNode] tree. Rather, it keeps track of the added layers on its own in
 * order to render (and also divide input among them) everything on a single canvas.
 *
 * After [ComposeScene] will no longer needed, you should call [ComposeScene.close] method, so
 * all resources and subscriptions will be properly closed. Otherwise, there can be a memory leak.
 *
 * @param density Initial density of the content which will be used to convert [Dp] units.
 * @param layoutDirection Initial layout direction of the content.
 * @param size The size of the [ComposeScene]. Default value is `null`, which means the size will be
 * determined by the content.
 * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
 * [rememberCoroutineScope]) and run recompositions.
 * @param composeSceneContext The context to share resources between multiple scenes and provide
 * a way for platform interaction.
 * @param invalidate The function to be called when the content need to be recomposed or
 * re-rendered. If you draw your content using [ComposeScene.render] method, in this callback you
 * should schedule the next [ComposeScene.render] in your rendering loop.
 * @return The created [ComposeScene].
 *
 * @see ComposeScene
 */
@InternalComposeUiApi
fun CanvasLayersComposeScene(
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    size: IntSize? = null,
    // TODO: Remove `Dispatchers.Unconfined` as a default
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    composeSceneContext: ComposeSceneContext = ComposeSceneContext.Empty,
    invalidate: () -> Unit = {},
): ComposeScene = CanvasLayersComposeSceneImpl(
    density = density,
    layoutDirection = layoutDirection,
    size = size,
    coroutineContext = coroutineContext,
    composeSceneContext = composeSceneContext,
    invalidate = invalidate
)

private class CanvasLayersComposeSceneImpl(
    density: Density,
    layoutDirection: LayoutDirection,
    size: IntSize?,
    coroutineContext: CoroutineContext,
    composeSceneContext: ComposeSceneContext,
    invalidate: () -> Unit = {},
) : BaseComposeScene(
    coroutineContext = coroutineContext,
    composeSceneContext = composeSceneContext,
    invalidate = invalidate
) {
    private val mainOwner = RootNodeOwner(
        density = density,
        layoutDirection = layoutDirection,
        size = size,
        coroutineContext = compositionContext.effectCoroutineContext,
        platformContext = composeSceneContext.platformContext,
        snapshotInvalidationTracker = snapshotInvalidationTracker,
        inputHandler = inputHandler,
    )

    override var density: Density = density
        set(value) {
            check(!isClosed) { "density set after ComposeScene is closed" }
            field = value
            mainOwner.density = value
        }

    override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            check(!isClosed) { "layoutDirection set after ComposeScene is closed" }
            field = value
            mainOwner.layoutDirection = value
        }

    override var size: IntSize? = size
        set(value) {
            check(!isClosed) { "size set after ComposeScene is closed" }
            check(value == null || (value.width >= 0f && value.height >= 0)) {
                "Size of ComposeScene cannot be negative"
            }
            field = value
            mainOwner.size = value
            forEachLayer { it.owner.size = value }
        }

    override val focusManager = ComposeSceneFocusManager { focusedOwner.focusOwner }

    override val dragAndDropTarget = ComposeSceneDragAndDropTarget { focusedOwner.dragAndDropOwner }

    private val layers = mutableListOf<AttachedComposeSceneLayer>()
    private val _layersCopyCache = CopiedList {
        it.addAll(layers)
    }
    private val _ownersCopyCache = CopiedList {
        it.add(mainOwner)
        for (layer in layers) {
            it.add(layer.owner)
        }
    }

    private inline fun forEachLayer(action: (AttachedComposeSceneLayer) -> Unit) =
        _layersCopyCache.withCopy {
            it.fastForEach(action)
        }

    private inline fun forEachLayerReversed(action: (AttachedComposeSceneLayer) -> Unit) =
        _layersCopyCache.withCopy {
            it.fastForEachReversed(action)
        }

    private inline fun forEachOwner(action: (RootNodeOwner) -> Unit) =
        _ownersCopyCache.withCopy {
            it.fastForEach(action)
        }

    private var focusedLayer: AttachedComposeSceneLayer? = null
    private val focusedOwner
        get() = focusedLayer?.owner ?: mainOwner
    private var gestureOwner: RootNodeOwner? = null
    private var lastHoverOwner: RootNodeOwner? = null

    init {
        onOwnerAppended(mainOwner)
    }

    override fun close() {
        check(!isClosed) { "ComposeScene is already closed" }
        onOwnerRemoved(mainOwner)
        mainOwner.dispose()
        forEachLayer { it.close() }
        super.close()
    }

    override fun calculateContentSize(): IntSize {
        check(!isClosed) { "calculateContentSize called after ComposeScene is closed" }
        return mainOwner.measureInConstraints(Constraints())
    }

    override fun invalidatePositionInWindow() {
        check(!isClosed) { "invalidatePositionInWindow called after ComposeScene is closed" }
        mainOwner.invalidatePositionInWindow()
    }

    override fun createComposition(content: @Composable () -> Unit): Composition {
        return mainOwner.setContent(
            compositionContext,
            { compositionLocalContext },
            content = content
        )
    }

    override fun hitTestInteropView(position: Offset): InteropView? {
        forEachLayerReversed { layer ->
            if (layer.isInBounds(position)) {
                return layer.owner.hitTestInteropView(position)
            } else if (layer == focusedLayer) {
                return null
            }
        }
        return mainOwner.hitTestInteropView(position)
    }

    override fun processPointerInputEvent(event: PointerInputEvent) {
        when (event.eventType) {
            PointerEventType.Press -> processPress(event)
            PointerEventType.Release -> processRelease(event)
            PointerEventType.Move -> processMove(event)
            PointerEventType.Enter -> processMove(event)
            PointerEventType.Exit -> processMove(event)
            PointerEventType.Scroll -> processScroll(event)
        }

        // Clean gestureOwner when there is no pressed pointers/buttons
        if (!event.isGestureInProgress) {
            gestureOwner = null
        }
    }

    override fun processKeyEvent(keyEvent: KeyEvent): Boolean =
        focusedLayer?.onKeyEvent(keyEvent) ?: mainOwner.onKeyEvent(keyEvent)

    override fun measureAndLayout() {
        forEachOwner { it.measureAndLayout() }
    }

    override fun draw(canvas: Canvas) {
        forEachOwner { it.draw(canvas) }
    }

    /**
     * Find hovered owner for position of first pointer.
     */
    private fun hoveredOwner(event: PointerInputEvent): RootNodeOwner {
        val position = event.pointers.first().position
        return layers.lastOrNull { it.isInBounds(position) }?.owner ?: mainOwner
    }

    /**
     * Check if [focusedLayer] blocks input for this owner.
     */
    private fun isInteractive(owner: RootNodeOwner?): Boolean {
        if (owner == null || focusedLayer == null) {
            return true
        }
        if (owner == mainOwner) {
            return false
        }
        for (layer in layers) {
            if (layer == focusedLayer) {
                return true
            }
            if (layer.owner == owner) {
                return false
            }
        }
        return true
    }

    private fun processPress(event: PointerInputEvent) {
        val currentGestureOwner = gestureOwner
        if (currentGestureOwner != null) {
            currentGestureOwner.onPointerInput(event)
            return
        }
        val position = event.pointers.first().position
        forEachLayerReversed { layer ->

            // If the position of in bounds of the owner - send event to it and stop processing
            if (layer.isInBounds(position)) {
                // The layer doesn't have any offset from [mainOwner], so we don't need to
                // convert event coordinates here.
                layer.owner.onPointerInput(event)
                gestureOwner = layer.owner
                return
            }

            // Input event is out of bounds - send click outside notification
            layer.onOutsidePointerEvent(event)

            // if the owner is in focus, do not pass the event to underlying owners
            if (layer == focusedLayer) {
                return
            }
        }
        mainOwner.onPointerInput(event)
        gestureOwner = mainOwner
    }

    private fun processRelease(event: PointerInputEvent) {
        // Send Release to gestureOwner even if is not hovered or under focusedOwner
        gestureOwner?.onPointerInput(event)
        if (!event.isGestureInProgress) {
            val owner = hoveredOwner(event)
            if (isInteractive(owner)) {
                processHover(event, owner)
            } else if (gestureOwner == null) {
                // If hovered owner is not interactive, then it means that
                // - It's not focusedOwner
                // - It placed under focusedOwner or not exist at all
                // In all these cases the even happened outside focused owner bounds
                focusedLayer?.onOutsidePointerEvent(event)
            }
        }
    }

    private fun processMove(event: PointerInputEvent) {
        var owner = when {
            // All touch events or mouse with pressed button(s)
            event.isGestureInProgress -> gestureOwner

            // Do not generate Enter and Move
            event.eventType == PointerEventType.Exit -> null

            // Find owner under mouse position
            else -> hoveredOwner(event)
        }

        // Even if the owner is not interactive, hover state still need to be updated
        if (!isInteractive(owner)) {
            owner = null
        }
        if (processHover(event, owner)) {
            return
        }
        owner?.onPointerInput(event.copy(eventType = PointerEventType.Move))
    }

    /**
     * Updates hover state and generates [PointerEventType.Enter] and [PointerEventType.Exit]
     * events. Returns true if [event] is consumed.
     */
    private fun processHover(event: PointerInputEvent, owner: RootNodeOwner?): Boolean {
        if (event.pointers.fastAny { it.type != PointerType.Mouse }) {
            // Track hover only for mouse
            return false
        }
        // Cases:
        // - move from outside to the window (owner != null, lastMoveOwner == null): Enter
        // - move from the window to outside (owner == null, lastMoveOwner != null): Exit
        // - move from one point of the window to another (owner == lastMoveOwner): Move
        // - move from one popup to another (owner != lastMoveOwner): [Popup 1] Exit, [Popup 2] Enter
        if (owner == lastHoverOwner) {
            // Owner wasn't changed
            return false
        }
        lastHoverOwner?.onPointerInput(event.copy(eventType = PointerEventType.Exit))
        owner?.onPointerInput(event.copy(eventType = PointerEventType.Enter))
        lastHoverOwner = owner

        // Changing hovering state replaces Move event, so treat it as consumed
        return true
    }

    private fun processScroll(event: PointerInputEvent) {
        val owner = hoveredOwner(event)
        if (isInteractive(owner)) {
            owner.onPointerInput(event)
        }
    }

    override fun createLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        compositionContext: CompositionContext,
    ): ComposeSceneLayer = AttachedComposeSceneLayer(
        density = density,
        layoutDirection = layoutDirection,
        focusable = focusable,
        compositionContext = compositionContext,
    )

    private fun onOwnerAppended(owner: RootNodeOwner) {
        semanticsOwnerListener?.onSemanticsOwnerAppended(owner.semanticsOwner)
    }

    private fun onOwnerRemoved(owner: RootNodeOwner) {
        if (owner == lastHoverOwner) {
            lastHoverOwner = null
        }
        if (owner == gestureOwner) {
            gestureOwner = null
        }
        semanticsOwnerListener?.onSemanticsOwnerRemoved(owner.semanticsOwner)
    }

    private fun attachLayer(layer: AttachedComposeSceneLayer) {
        check(!isClosed) { "attachLayer called after ComposeScene is closed" }
        layers.add(layer)

        if (layer.focusable) {
            requestFocus(layer)
        }
        onOwnerAppended(layer.owner)

        inputHandler.onPointerUpdate()
        updateInvalidations()
    }

    private fun detachLayer(layer: AttachedComposeSceneLayer) {
        check(!isClosed) { "detachLayer called after ComposeScene is closed" }
        layers.remove(layer)

        releaseFocus(layer)
        onOwnerRemoved(layer.owner)

        inputHandler.onPointerUpdate()
        updateInvalidations()
    }

    private fun requestFocus(layer: AttachedComposeSceneLayer) {
        if (isInteractive(layer.owner)) {
            focusedLayer = layer

            // Exit event to lastHoverOwner will be sent via synthetic event on next frame
        }
    }

    private fun releaseFocus(layer: AttachedComposeSceneLayer) {
        if (layer == focusedLayer) {
            focusedLayer = layers.lastOrNull { it.focusable }

            // Enter event to new focusedOwner will be sent via synthetic event on next frame
        }
    }

    private inner class AttachedComposeSceneLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        private val compositionContext: CompositionContext,
    ) : ComposeSceneLayer {
        val owner = RootNodeOwner(
            density = density,
            layoutDirection = layoutDirection,
            coroutineContext = compositionContext.effectCoroutineContext,
            size = this@CanvasLayersComposeSceneImpl.size,
            platformContext = object : PlatformContext by composeSceneContext.platformContext {

                /**
                 * Popup/Dialog shouldn't delegate focus to the parent.
                 */
                override val parentFocusManager: FocusManager
                    get() = PlatformContext.Empty.parentFocusManager

                // TODO: Figure out why real requestFocus is required
                //  even with empty parentFocusManager
            },
            snapshotInvalidationTracker = snapshotInvalidationTracker,
            inputHandler = inputHandler,
        )
        private var composition: Composition? = null
        private var outsidePointerCallback: ((
            eventType: PointerEventType,
            button: PointerButton?
        ) -> Unit)? = null
        private var isClosed = false

        override var density: Density by owner::density
        override var layoutDirection: LayoutDirection by owner::layoutDirection

        /*
         * We cannot set [owner.bounds] as default value because real bounds will be available
         * not immediately, so it will change [lastHoverOwner] for a few frames.
         * This scenario is important when user code relies on hover events to show tooltips.
         */
        override var boundsInWindow: IntRect by mutableStateOf(IntRect.Zero)

        @Deprecated(
            message = "Should not be used in this implementation",
            level = DeprecationLevel.ERROR
        )
        override var compositionLocalContext: CompositionLocalContext? = null

        override var scrimColor: Color? by mutableStateOf(null)
        override var focusable: Boolean = focusable
            set(value) {
                field = value
                if (value) {
                    requestFocus(this)
                } else {
                    releaseFocus(this)
                }
                inputHandler.onPointerUpdate()
                updateInvalidations()
            }

        private val background: Modifier
            get() = scrimColor?.let {
                Modifier.drawBehind {
                    drawRect(
                        color = it,
                        blendMode = getDialogScrimBlendMode(
                            composeSceneContext.platformContext.isWindowTransparent
                        )
                    )
                }
            } ?: Modifier

        private var onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null
        private var onKeyEvent: ((KeyEvent) -> Boolean)? = null

        init {
            attachLayer(this)
        }

        override fun close() {
            if (isClosed) return
            detachLayer(this)
            composition?.dispose()
            composition = null
            owner.dispose()
            isClosed = true
        }

        override fun setKeyEventListener(
            onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
            onKeyEvent: ((KeyEvent) -> Boolean)?,
        ) {
            this.onPreviewKeyEvent = onPreviewKeyEvent
            this.onKeyEvent = onKeyEvent
        }

        fun onKeyEvent(keyEvent: KeyEvent): Boolean {
            return onPreviewKeyEvent?.invoke(keyEvent) == true ||
                owner.onKeyEvent(keyEvent) ||
                onKeyEvent?.invoke(keyEvent) == true
        }

        override fun setOutsidePointerEventListener(
            onOutsidePointerEvent: ((eventType: PointerEventType, button: PointerButton?) -> Unit)?,
        ) {
            outsidePointerCallback = onOutsidePointerEvent
        }

        override fun setContent(content: @Composable () -> Unit) {
            check(!isClosed) { "AttachedComposeSceneLayer is closed" }
            composition?.dispose()
            composition = owner.setContent(
                parent = this@AttachedComposeSceneLayer.compositionContext,
                {
                    /*
                     * Do not use `compositionLocalContext` here - composition locals already
                     * available from `compositionContext` and explicitly overriding it might cause
                     * issues. See https://github.com/JetBrains/compose-multiplatform/issues/4558
                     */
                    null
                }
            ) {
                owner.setRootModifier(background)
                content()
            }
        }

        override fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset =
            positionInWindow // [ComposeScene] is equal to window in this implementation.

        fun isInBounds(position: Offset) = boundsInWindow.contains(position.round())

        fun onOutsidePointerEvent(event: PointerInputEvent) {
            if (!event.isMouseOrSingleTouch()) {
                return
            }
            outsidePointerCallback?.invoke(event.eventType, event.button)
        }
    }
}

private val PointerInputEvent.isGestureInProgress get() = pointers.fastAny { it.down }

private fun PointerInputEvent.isMouseOrSingleTouch() =
    button != null || pointers.size == 1

private class CopiedList<T>(
    private val populate: (MutableList<T>) -> Unit
) : MutableList<T> by mutableListOf() {
    inline fun withCopy(
        block: (List<T>) -> Unit
    ) {
        // In case of recursive calls, allocate new list
        val copy = if (isEmpty()) this else mutableListOf()
        populate(copy)
        try {
            block(copy)
        } finally {
            copy.clear()
        }
    }
}
