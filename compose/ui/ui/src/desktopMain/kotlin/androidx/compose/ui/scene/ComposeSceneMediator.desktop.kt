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

import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.awt.AwtEventListener
import androidx.compose.ui.awt.AwtEventListeners
import androidx.compose.ui.awt.OnlyValidPrimaryMouseButtonFilter
import androidx.compose.ui.awt.isFocusGainedHandledBySwingPanel
import androidx.compose.ui.awt.runOnEDTThread
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.internal
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.pointer.AwtCursor
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.AwtDragAndDropManager
import androidx.compose.ui.platform.DelegateRootForTestListener
import androidx.compose.ui.platform.DesktopTextInputService
import androidx.compose.ui.platform.EmptyViewConfiguration
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.a11y.AccessibilityController
import androidx.compose.ui.platform.a11y.ComposeSceneAccessible
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.SwingInteropContainer
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import androidx.compose.ui.window.sizeInPx
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.awt.event.FocusEvent
import java.awt.event.FocusEvent.Cause.TRAVERSAL
import java.awt.event.FocusEvent.Cause.TRAVERSAL_BACKWARD
import java.awt.event.FocusEvent.Cause.TRAVERSAL_FORWARD
import java.awt.event.FocusListener
import java.awt.event.InputEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.im.InputMethodRequests
import javax.accessibility.Accessible
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.swing.SkiaSwingLayer

/**
 * Provides a mediator for integrating a Compose scene with AWT/Swing component.
 * It allows setting Compose content by [setContent], this content should be drawn on [contentComponent].
 *
 * This mediator contain 2 components that should be added to the view hierarchy:
 * [contentComponent] the main visible Swing component with skia canvas, on which Compose will be shown
 * [invisibleComponent] service component used to bypass Swing issues:
 * - for forcing refocus on input methods change
 */
internal class ComposeSceneMediator(
    private val container: JComponent,
    private val windowContext: PlatformWindowContext,
    private var exceptionHandler: WindowExceptionHandler?,
    eventListener: AwtEventListener? = null,

    /**
     * @see PlatformContext.measureDrawLayerBounds
     */
    private val measureDrawLayerBounds: Boolean = false,

    val coroutineContext: CoroutineContext,

    skiaLayerComponentFactory: (ComposeSceneMediator) -> SkiaLayerComponent,
    composeSceneFactory: (ComposeSceneMediator) -> ComposeScene,
) : SkikoRenderDelegate {
    private var isDisposed = false
    private val invisibleComponent = InvisibleComponent()

    private val semanticsOwnerListener = DesktopSemanticsOwnerListener()
    var rootForTestListener: PlatformContext.RootForTestListener? by DelegateRootForTestListener()
    val accessible: Accessible = ComposeSceneAccessible {
        semanticsOwnerListener.accessibilityControllers
    }

    private val platformComponent = DesktopPlatformComponent()
    private val textInputService = DesktopTextInputService(platformComponent)
    private val _platformContext = DesktopPlatformContext()
    val platformContext: PlatformContext get() = _platformContext

    private val skiaLayerComponent: SkiaLayerComponent by lazy { skiaLayerComponentFactory(this) }
    val contentComponent by skiaLayerComponent::contentComponent
    var fullscreen by skiaLayerComponent::fullscreen
    val windowHandle by skiaLayerComponent::windowHandle
    val renderApi by skiaLayerComponent::renderApi

    /**
     * @see ComposeFeatureFlags.useInteropBlending
     */
    private val useInteropBlending: Boolean
        get() = ComposeFeatureFlags.useInteropBlending && skiaLayerComponent.interopBlendingSupported

    /**
     * Adding any components below [contentComponent] makes our bridge non-transparent on macOS.
     * But as it draws always on top, so we can just add it as-is.
     * TODO: Figure out why it makes difference in transparency
     */
    @OptIn(ExperimentalSkikoApi::class)
    private val metalOrderHack
        get() = renderApi == GraphicsApi.METAL && contentComponent !is SkiaSwingLayer

    /**
     * A container that controls interop views/components. It is used to add and remove
     * native views/components to [container].
     */
    private val interopContainer = SwingInteropContainer(
        root = container,
        placeInteropAbove = !useInteropBlending || metalOrderHack,
        requestRedraw = ::onComposeInvalidation
    )

    private val containerListener = object : ContainerListener {
        private val clipMap = mutableMapOf<Component, ClipRectangle>()

        override fun componentAdded(e: ContainerEvent) {
            val component = e.child
            if (useInteropBlending) {
                // In case of interop blending, compose might draw content above this [component].
                // But due to implementation of [JLayeredPane]'s lightweight/heavyweight mixing
                // logic, it doesn't send mouse events to parents or another layers.
                // In case if [component] is placed above [contentComponent] (see addToLayer),
                // subscribe to mouse events from interop views to handle such input.
                component.subscribeToMouseEvents(mouseListener)
            } else {
                // Without interop blending, just add clip region to make proper
                // "interop always on top" behaviour.
                addClipComponent(component)
            }
        }

        override fun componentRemoved(e: ContainerEvent) {
            val component = e.child
            removeClipComponent(component)
            component.unsubscribeFromMouseEvents(mouseListener)
        }

        private fun addClipComponent(component: Component) {
            val clipRectangle = interopContainer.getClipRectForComponent(component)
            clipMap[component] = clipRectangle
            skiaLayerComponent.clipComponents.add(clipRectangle)
        }

        private fun removeClipComponent(component: Component) {
            clipMap.remove(component)?.let {
                skiaLayerComponent.clipComponents.remove(it)
            }
        }
    }
    private val inputMethodListener = object : InputMethodListener {
        override fun caretPositionChanged(event: InputMethodEvent?) {
            if (isDisposed) return
            // Which OSes and which input method could produce such events? We need to have some
            // specific cases in mind before implementing this
        }

        override fun inputMethodTextChanged(event: InputMethodEvent) {
            if (isDisposed) return
            catchExceptions {
                textInputService.inputMethodTextChanged(event)
            }
        }
    }
    private val focusListener = object : FocusListener {
        override fun focusGained(e: FocusEvent) {
            // We don't reset focus for Compose when the component loses focus temporary.
            // Partially because we don't support restoring focus after clearing it.
            // Focus can be lost temporary when another window or popup takes focus.
            if (!e.isTemporary && !e.isFocusGainedHandledBySwingPanel(container)) {
                when (e.cause) {
                    TRAVERSAL_BACKWARD -> {
                        if (!focusManager.takeFocus(FocusDirection.Previous)) {
                            platformContext.parentFocusManager.moveFocus(FocusDirection.Previous)
                        }
                    }
                    TRAVERSAL, TRAVERSAL_FORWARD -> {
                        if (!focusManager.takeFocus(FocusDirection.Next)) {
                            platformContext.parentFocusManager.moveFocus(FocusDirection.Next)
                        }
                    }
                    else -> Unit
                }
            }
        }

        override fun focusLost(e: FocusEvent) {
            // We don't reset focus for Compose when the component loses focus temporary.
            // Partially because we don't support restoring focus after clearing it.
            // Focus can be lost temporary when another window or popup takes focus.
            if (!e.isTemporary) {
                scene.focusManager.releaseFocus()
            }
        }
    }
    private val mouseListener = object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
        override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
        override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
        override fun mouseExited(event: MouseEvent) = onMouseEvent(event)
        override fun mouseDragged(event: MouseEvent) = onMouseEvent(event)
        override fun mouseMoved(event: MouseEvent) = onMouseEvent(event)
        override fun mouseWheelMoved(event: MouseWheelEvent) = onMouseWheelEvent(event)
    }
    private val keyListener = object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) = onKeyEvent(event)
        override fun keyReleased(event: KeyEvent) = onKeyEvent(event)
        override fun keyTyped(event: KeyEvent) = onKeyEvent(event)
    }

    private val eventListener = if (eventListener != null) {
        AwtEventListeners(OnlyValidPrimaryMouseButtonFilter, eventListener)
    } else {
        OnlyValidPrimaryMouseButtonFilter
    }

    var currentInputMethodRequests: InputMethodRequests? = null
        private set

    /**
     * The bounds of scene relative to [container]. Might be null if it's equal to [container] size.
     *
     * It makes sense in cases when real [container] size doesn't match desired value.
     * For example if we want to show dialog in a separate window with size of this
     * dialog, but constrains (and scene size) should remain the size of the main window.
     */
    var sceneBoundsInPx: Rect? = null

    private var offsetInWindow = Point(0, 0)
        set(value) {
            if (field != value) {
                field = value
                scene.invalidatePositionInWindow()
            }
        }

    private val scene by lazy { composeSceneFactory(this) }
    val focusManager get() = scene.focusManager
    var compositionLocalContext: CompositionLocalContext?
        get() = scene.compositionLocalContext
        set(value) { scene.compositionLocalContext = value }

    /**
     * Provides the size of ComposeScene content inside infinity constraints
     *
     * This is needed for the bridge between Compose and Swing since
     * in some cases, Swing's LayoutManagers need
     * to calculate the preferred size of the content without max/min constraints
     * to properly lay it out.
     *
     * Example: Compose content inside Popup without a preferred size.
     * Swing will calculate the preferred size of the Compose content and set Popup's side for that.
     *
     * See [androidx.compose.ui.awt.ComposePanelTest] test `initial panel size of LazyColumn with border layout`
     */
    val preferredSize: Dimension
        get() {
            val contentSize = scene.calculateContentSize()
            val scale = scene.density.density
            return Dimension(
                (contentSize.width / scale).toInt(),
                (contentSize.height / scale).toInt()
            )
        }

    /**
     * Keyboard modifiers state might be changed when window is not focused, so window doesn't
     * receive any key events.
     * This flag is set when window focus changes. Then we can rely on it when handling the
     * first movementEvent to get the actual keyboard modifiers state from it.
     * After window gains focus, the first motionEvent.metaState (after focus gained) is used
     * to update windowInfo.keyboardModifiers.
     *
     * TODO: needs to be set `true` when focus changes:
     * (Window focus change is implemented in JB fork, but not upstreamed yet).
     */
    private var keyboardModifiersRequireUpdate = false

    private val dragAndDropManager = AwtDragAndDropManager(container, getScene = { scene })

    init {
        // Transparency is used during redrawer creation that triggered by [addNotify], so
        // it must be set to correct value before adding to the hierarchy to handle cases
        // when [container] is already [isDisplayable].
        skiaLayerComponent.transparency = useInteropBlending

        container.add(invisibleComponent)
        container.add(contentComponent)

        // Adding a listener after adding [invisibleComponent] and [contentComponent]
        // to react only on changes with [interopLayer].
        container.addContainerListener(containerListener)

        // AwtDragAndDropManager support
        container.transferHandler = dragAndDropManager.transferHandler
        container.dropTarget = dragAndDropManager.dropTarget

        // It will be enabled dynamically. See DesktopPlatformComponent
        contentComponent.enableInputMethods(false)
        contentComponent.focusTraversalKeysEnabled = false

        subscribe(contentComponent)
    }

    private inline fun catchExceptions(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            exceptionHandler?.onException(e) ?: throw e
        }
    }

    private fun resetFocus() {
        if (contentComponent.isFocusOwner) {
            invisibleComponent.requestFocusTemporary()
            contentComponent.requestFocus()
        }
    }

    private fun subscribe(component: Component) {
        component.addInputMethodListener(inputMethodListener)
        component.addFocusListener(focusListener)
        component.addKeyListener(keyListener)
        component.subscribeToMouseEvents(mouseListener)
    }

    private fun unsubscribe(component: Component) {
        component.removeInputMethodListener(inputMethodListener)
        component.removeFocusListener(focusListener)
        component.removeKeyListener(keyListener)
        component.unsubscribeFromMouseEvents(mouseListener)
    }

    private var isMouseEventProcessing = false
    private inline fun processMouseEvent(block: () -> Unit) {
        // Filter out mouse event if [ComposeScene] is already processing this mouse event
        if (isMouseEventProcessing) {
            return
        }

        // Track if [event] is currently processing to avoid recursion in case if [SwingPanel]
        // manually spawns a new AWT event for interop view.
        // See [InteropPointerInputModifier] for details.
        isMouseEventProcessing = true
        try {
            block()
        } finally {
            isMouseEventProcessing = false
        }
    }

    private val MouseEvent.position: Offset
        get() {
            val pointInContainer = SwingUtilities.convertPoint(component, point, container)
            val offset = sceneBoundsInPx?.topLeft ?: Offset.Zero
            val density = contentComponent.density
            return Offset(pointInContainer.x.toFloat(), pointInContainer.y.toFloat()) * density.density - offset
        }

    private fun onMouseEvent(event: MouseEvent): Unit = catchExceptions {
        // AWT can send events after the window is disposed
        if (isDisposed) {
            return
        }
        if (eventListener.onMouseEvent(event)) {
            return
        }
        if (keyboardModifiersRequireUpdate) {
            keyboardModifiersRequireUpdate = false
            windowContext.setKeyboardModifiers(event.keyboardModifiers)
        }
        processMouseEvent {
            scene.onMouseEvent(event.position, event)
        }
    }

    private fun onMouseWheelEvent(event: MouseWheelEvent): Unit = catchExceptions {
        // AWT can send events after the window is disposed
        if (isDisposed) {
            return
        }
        if (eventListener.onMouseEvent(event)) {
            return
        }
        processMouseEvent {
            scene.onMouseWheelEvent(event.position, event)
        }
    }

    private fun onKeyEvent(event: KeyEvent) = catchExceptions {
        // AWT can send events after the window is disposed
        if (isDisposed) {
            return
        }
        if (eventListener.onKeyEvent(event)) {
            return
        }
        val composeEvent = event.toComposeEvent()
        textInputService.onKeyEvent(event)
        windowContext.setKeyboardModifiers(composeEvent.internal.modifiers)
        if (onPreviewKeyEvent(composeEvent) ||
            scene.sendKeyEvent(composeEvent) ||
            onKeyEvent(composeEvent)
        ) {
            event.consume()
        }
    }

    fun dispose() {
        check(!isDisposed) { "ComposeSceneMediator is already disposed" }
        isDisposed = true

        unsubscribe(contentComponent)

        // Since rendering will not happen after, we needs to execute all scheduled updates
        interopContainer.dispose()
        container.removeContainerListener(containerListener)
        container.remove(contentComponent)
        container.remove(invisibleComponent)
        container.transferHandler = null
        container.dropTarget = null

        scene.close()
        skiaLayerComponent.dispose()
        _onComponentAttached = null
    }

    fun onComponentAttached() {
        onChangeDensity()

        _onComponentAttached?.invoke()
        _onComponentAttached = null
    }

    private var onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false }
    private var onKeyEvent: (ComposeKeyEvent) -> Boolean = { false }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
    ) {
        this.onPreviewKeyEvent = onPreviewKeyEvent
        this.onKeyEvent = onKeyEvent
    }

    private var _onComponentAttached: (() -> Unit)? = null
    private fun runOnceComponentAttached(block: () -> Unit) {
        if (contentComponent.isDisplayable) {
            block()
        } else {
            _onComponentAttached = block
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        runOnceComponentAttached {
            catchExceptions {
                scene.setContent {
                    interopContainer {
                        content()
                    }
                }
            }
        }
    }

    fun onComposeInvalidation() = runOnEDTThread {
        catchExceptions {
            if (isDisposed) return@catchExceptions
            skiaLayerComponent.onComposeInvalidation()
        }
    }

    fun onComponentPositionChanged() = catchExceptions {
        if (!container.isDisplayable) return

        offsetInWindow = windowContext.offsetInWindow(container)
    }

    fun onComponentSizeChanged() = catchExceptions {
        if (!container.isDisplayable) return

        val size = sceneBoundsInPx?.size ?: container.sizeInPx
        scene.size = IntSize(
            // container.sizeInPx can be negative
            width = size.width.coerceAtLeast(0f).roundToInt(),
            height = size.height.coerceAtLeast(0f).roundToInt()
        )
    }

    fun onChangeDensity(density: Density = container.density) = catchExceptions {
        if (scene.density != density) {
            scene.density = density
            onComponentSizeChanged()
        }
    }

    fun onWindowTransparencyChanged(value: Boolean) {
        skiaLayerComponent.transparency = value || useInteropBlending
    }

    fun onLayoutDirectionChanged(layoutDirection: LayoutDirection) {
        scene.layoutDirection = layoutDirection
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) = catchExceptions {
        interopContainer.postponingExecutingScheduledUpdates {
            canvas.withSceneOffset {
                scene.render(asComposeCanvas(), nanoTime)
            }
        }
    }

    private inline fun Canvas.withSceneOffset(block: Canvas.() -> Unit) {
        // Offset of scene relative to [container]
        val sceneBoundsOffset = sceneBoundsInPx?.topLeft ?: Offset.Zero
        // Offset of canvas relative to [container]
        val contentOffset = with(contentComponent) {
            val scale = density.density
            Offset(x * scale, y * scale)
        }
        val sceneOffset = sceneBoundsOffset - contentOffset
        save()
        translate(sceneOffset.x, sceneOffset.y)
        block()
        restore()
    }

    fun onRenderApiChanged(action: () -> Unit) {
        skiaLayerComponent.onRenderApiChanged(action)
    }

    fun onChangeWindowFocus() {
        keyboardModifiersRequireUpdate = true
    }

    private inner class DesktopViewConfiguration : ViewConfiguration by EmptyViewConfiguration {
        override val touchSlop: Float get() = with(platformComponent.density) { 18.dp.toPx() }
    }

    private inner class DesktopFocusManager : FocusManager {
        override fun clearFocus(force: Boolean) {
            val root = contentComponent.rootPane
            root?.focusTraversalPolicy?.getDefaultComponent(root)?.requestFocusInWindow()
        }

        override fun moveFocus(focusDirection: FocusDirection): Boolean =
            when (focusDirection) {
                FocusDirection.Next -> {
                    val toFocus = contentComponent.focusCycleRootAncestor?.let { root ->
                        val policy = root.focusTraversalPolicy
                        policy.getComponentAfter(root, contentComponent)
                            ?: policy.getDefaultComponent(root)
                    }
                    val hasFocus = toFocus?.hasFocus() == true
                    !hasFocus && toFocus?.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_FORWARD) == true
                }

                FocusDirection.Previous -> {
                    val toFocus = contentComponent.focusCycleRootAncestor?.let { root ->
                        val policy = root.focusTraversalPolicy
                        policy.getComponentBefore(root, contentComponent)
                            ?: policy.getDefaultComponent(root)
                    }
                    val hasFocus = toFocus?.hasFocus() == true
                    !hasFocus && toFocus?.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_BACKWARD) == true
                }

                else -> false
            }
    }

    private inner class DesktopSemanticsOwnerListener : PlatformContext.SemanticsOwnerListener {
        /**
         * A new [SemanticsOwner] is always created above existing ones. So, usage of [LinkedHashMap]
         * is required here to keep insertion-order (that equal to [SemanticsOwner]s order).
         */
        private val _accessibilityControllers = linkedMapOf<SemanticsOwner, AccessibilityController>()
        val accessibilityControllers get() = _accessibilityControllers.values.reversed()

        override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
            check(semanticsOwner !in _accessibilityControllers)
            _accessibilityControllers[semanticsOwner] = AccessibilityController(
                owner = semanticsOwner,
                desktopComponent = platformComponent,
                onFocusReceived = {
                    skiaLayerComponent.requestNativeFocusOnAccessible(it)
                }
            ).also {
                it.launchSyncLoop(coroutineContext)
            }
        }

        override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers.remove(semanticsOwner)?.dispose()
        }

        override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers[semanticsOwner]?.onSemanticsChange()
        }

        override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {
            _accessibilityControllers[semanticsOwner]?.onLayoutChanged(nodeId = semanticsNodeId)
        }
    }

    private inner class DesktopPlatformContext : PlatformContext by PlatformContext.Empty {
        override val windowInfo: WindowInfo get() = windowContext.windowInfo
        override val isWindowTransparent: Boolean get() = windowContext.isWindowTransparent

        override fun convertLocalToWindowPosition(localPosition: Offset): Offset =
            windowContext.convertLocalToWindowPosition(container, localPosition)

        override fun convertWindowToLocalPosition(positionInWindow: Offset): Offset =
            windowContext.convertWindowToLocalPosition(container, positionInWindow)

        override fun convertLocalToScreenPosition(localPosition: Offset): Offset =
            windowContext.convertLocalToScreenPosition(container, localPosition)

        override fun convertScreenToLocalPosition(positionOnScreen: Offset): Offset =
            windowContext.convertScreenToLocalPosition(container, positionOnScreen)

        override val measureDrawLayerBounds: Boolean = this@ComposeSceneMediator.measureDrawLayerBounds
        override val viewConfiguration: ViewConfiguration = DesktopViewConfiguration()
        override val textInputService = this@ComposeSceneMediator.textInputService

        private val textInputSessionMutex = SessionMutex<DesktopTextInputSession>()

        override suspend fun textInputSession(
            session: suspend PlatformTextInputSessionScope.() -> Nothing
        ): Nothing = textInputSessionMutex.withSessionCancellingPrevious(
            sessionInitializer = {
                DesktopTextInputSession(coroutineScope = it)
            },
            session = session
        )

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            contentComponent.cursor =
                (pointerIcon as? AwtCursor)?.cursor ?: Cursor(Cursor.DEFAULT_CURSOR)
        }
        override val parentFocusManager: FocusManager = DesktopFocusManager()
        override fun requestFocus(): Boolean {
            // Don't check hasFocus(), and don't check the returning result
            // Swing returns "false" if the window isn't visible or isn't active,
            // but the component will always receive the focus after activation.
            //
            // if we return false - we don't allow changing the focus, and it breaks requesting
            // focus at start and in inactive mode
            contentComponent.requestFocusInWindow()
            return true
        }

        override fun startDrag(
            transferData: DragAndDropTransferData,
            decorationSize: Size,
            drawDragDecoration: DrawScope.() -> Unit
        ) = dragAndDropManager.startDrag(
            transferData, decorationSize, drawDragDecoration
        )

        override val rootForTestListener
            get() = this@ComposeSceneMediator.rootForTestListener
        override val semanticsOwnerListener
            get() = this@ComposeSceneMediator.semanticsOwnerListener
    }

    private inner class DesktopPlatformComponent : PlatformComponent {
        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            currentInputMethodRequests = inputMethodRequests
            contentComponent.enableInputMethods(true)
            // Without resetting the focus, Swing won't update the status (doesn't show/hide popup)
            // enableInputMethods is design to used per-Swing component level at init stage,
            // not dynamically
            resetFocus()
        }

        override fun disableInput() {
            currentInputMethodRequests = null
            contentComponent.enableInputMethods(false)
            // Without resetting the focus, Swing won't update the status (doesn't show/hide popup)
            // enableInputMethods is design to used per-Swing component level at init stage,
            // not dynamically
            resetFocus()
        }

        override val locationOnScreen: Point
            get() = contentComponent.locationOnScreen

        override val density: Density
            get() = contentComponent.density
    }

    @OptIn(InternalComposeUiApi::class)
    private inner class DesktopTextInputSession(
        coroutineScope: CoroutineScope,
    ) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {

        private val innerSessionMutex = SessionMutex<Nothing?>()

        override suspend fun startInputMethod(
            request: PlatformTextInputMethodRequest
        ): Nothing = innerSessionMutex.withSessionCancellingPrevious(
            // This session has no data, just init/dispose tasks.
            sessionInitializer = { null }
        ) {
            (suspendCancellableCoroutine<Nothing> { continuation ->
                textInputService.startInput(
                    value = request.state,
                    imeOptions = request.imeOptions,
                    onEditCommand = request.onEditCommand,
                    onImeActionPerformed = request.onImeAction ?: {}
                )

                continuation.invokeOnCancellation {
                    textInputService.stopInput()
                }
            })
        }
    }


    private class InvisibleComponent : Component() {
        fun requestFocusTemporary(): Boolean {
            return super.requestFocus(true)
        }
    }
}

private fun ComposeScene.onMouseEvent(
    position: Offset,
    event: MouseEvent
) {
    val eventType = when (event.id) {
        MouseEvent.MOUSE_PRESSED -> PointerEventType.Press
        MouseEvent.MOUSE_RELEASED -> PointerEventType.Release
        MouseEvent.MOUSE_DRAGGED -> PointerEventType.Move
        MouseEvent.MOUSE_MOVED -> PointerEventType.Move
        MouseEvent.MOUSE_ENTERED -> PointerEventType.Enter
        MouseEvent.MOUSE_EXITED -> PointerEventType.Exit
        else -> PointerEventType.Unknown
    }
    sendPointerEvent(
        eventType = eventType,
        position = position,
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event,
        button = event.composePointerButton
    )
}

internal val MouseEvent.composePointerButton: PointerButton? get() {
    if (button == MouseEvent.NOBUTTON) return null
    return when (button) {
        MouseEvent.BUTTON2 -> PointerButton.Tertiary
        MouseEvent.BUTTON3 -> PointerButton.Secondary
        else -> PointerButton(button - 1)
    }
}

private fun ComposeScene.onMouseWheelEvent(
    position: Offset,
    event: MouseWheelEvent
) {
    sendPointerEvent(
        eventType = PointerEventType.Scroll,
        position = position,
        scrollDelta = if (event.isShiftDown) {
            Offset(event.preciseWheelRotation.toFloat(), 0f)
        } else {
            Offset(0f, event.preciseWheelRotation.toFloat())
        },
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}


private val MouseEvent.buttons get() = PointerButtons(
    // We should check [event.button] because of case where [event.modifiersEx] does not provide
    // info about the pressed mouse button when using touchpad on MacOS 12 (AWT only).
    // When the [Tap to click] feature is activated on Mac OS 12, half of all clicks are not
    // handled because [event.modifiersEx] may not provide info about the pressed mouse button.
    isPrimaryPressed = ((modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON1))
        && !isMacOsCtrlClick,
    isSecondaryPressed = (modifiersEx and MouseEvent.BUTTON3_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON3)
        || isMacOsCtrlClick,
    isTertiaryPressed = (modifiersEx and MouseEvent.BUTTON2_DOWN_MASK) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == MouseEvent.BUTTON2),
    isBackPressed = (modifiersEx and MouseEvent.getMaskForButton(4)) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == 4),
    isForwardPressed = (modifiersEx and MouseEvent.getMaskForButton(5)) != 0
        || (id == MouseEvent.MOUSE_PRESSED && button == 5),
)

private val MouseEvent.keyboardModifiers get() = PointerKeyboardModifiers(
    isCtrlPressed = (modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0,
    isMetaPressed = (modifiersEx and InputEvent.META_DOWN_MASK) != 0,
    isAltPressed = (modifiersEx and InputEvent.ALT_DOWN_MASK) != 0,
    isShiftPressed = (modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0,
    isAltGraphPressed = (modifiersEx and InputEvent.ALT_GRAPH_DOWN_MASK) != 0,
    isSymPressed = false,
    isFunctionPressed = false,
    isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
    isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
    isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
)

private fun Component.subscribeToMouseEvents(mouseAdapter: MouseAdapter) {
    addMouseListener(mouseAdapter)
    addMouseMotionListener(mouseAdapter)
    addMouseWheelListener(mouseAdapter)
}

private fun Component.unsubscribeFromMouseEvents(mouseAdapter: MouseAdapter) {
    removeMouseListener(mouseAdapter)
    removeMouseMotionListener(mouseAdapter)
    removeMouseWheelListener(mouseAdapter)
}

private fun getLockingKeyStateSafe(
    mask: Int
): Boolean = try {
    Toolkit.getDefaultToolkit().getLockingKeyState(mask)
} catch (_: Exception) {
    false
}

private val MouseEvent.isMacOsCtrlClick
    get() = (
        hostOs.isMacOS &&
            ((modifiersEx and InputEvent.BUTTON1_DOWN_MASK) != 0) &&
            ((modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0)
        )
