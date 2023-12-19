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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import java.awt.*
import java.awt.Cursor
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.im.InputMethodRequests
import javax.swing.JLayeredPane
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.*
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
    private val container: JLayeredPane,
    private val windowContext: PlatformWindowContext,
    private var exceptionHandler: WindowExceptionHandler?,

    val coroutineContext: CoroutineContext,

    skiaLayerComponentFactory: (ComposeSceneMediator) -> SkiaLayerComponent,
    composeSceneFactory: (ComposeSceneMediator) -> ComposeScene,
) {
    private var isDisposed = false
    private val invisibleComponent = InvisibleComponent()

    val skikoView: SkikoView = DesktopSkikoView()

    private val platformComponent = DesktopPlatformComponent()
    private val textInputService = DesktopTextInputService(platformComponent)
    private val _platformContext = DesktopPlatformContext()
    val platformContext: PlatformContext get() = _platformContext

    private val skiaLayerComponent by lazy { skiaLayerComponentFactory(this) }
    val contentComponent by skiaLayerComponent::contentComponent
    var transparency: Boolean
        get() = skiaLayerComponent.transparency
        set(value) {
            skiaLayerComponent.transparency = value || useInteropBlending
        }
    var fullscreen by skiaLayerComponent::fullscreen
    val windowHandle by skiaLayerComponent::windowHandle
    val renderApi by skiaLayerComponent::renderApi

    private val containerListener = object : ContainerListener {
        private val clipMap = mutableMapOf<Component, ClipComponent>()

        override fun componentAdded(e: ContainerEvent) {
            if (useInteropBlending) {
                return
            }
            val component = e.child
            val clipRectangle = ClipComponent(component)
            clipMap[component] = clipRectangle
            skiaLayerComponent.clipComponents.add(clipRectangle)
        }

        override fun componentRemoved(e: ContainerEvent) {
            val component = e.child
            clipMap.remove(component)?.let {
                skiaLayerComponent.clipComponents.remove(it)
            }
        }
    }

    var currentInputMethodRequests: InputMethodRequests? = null
        private set

    private val semanticsOwnerListener = DesktopSemanticsOwnerListener()
    var rootForTestListener: PlatformContext.RootForTestListener? by DelegateRootForTestListener()

    private val scene by lazy { composeSceneFactory(this) }
    val focusManager get() = scene.focusManager
    var compositionLocalContext: CompositionLocalContext?
        get() = scene.compositionLocalContext
        set(value) { scene.compositionLocalContext = value }
    val accessible = ComposeSceneAccessible {
        semanticsOwnerListener.accessibilityControllers
    }

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
            return Dimension(
                (contentSize.width / contentComponent.density.density).toInt(),
                (contentSize.height / contentComponent.density.density).toInt()
            )
        }

    private val density get() = platformComponent.density.density

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

    private val useInteropBlending: Boolean
        get() = ComposeFeatureFlags.useInteropBlending && skiaLayerComponent.interopBlendingSupported

    private val contentLayer: Int = 10
    private val interopLayer: Int
        get() = if (useInteropBlending) 0 else 20


    init {
        container.addToLayer(invisibleComponent, contentLayer)
        container.addToLayer(contentComponent, contentLayer)
        container.addContainerListener(containerListener)

        skiaLayerComponent.transparency = useInteropBlending

        contentComponent.enableInputMethods(false)
        contentComponent.addInputMethodListener(object : InputMethodListener {
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
        })

        contentComponent.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                // We don't reset focus for Compose when the component loses focus temporary.
                // Partially because we don't support restoring focus after clearing it.
                // Focus can be lost temporary when another window or popup takes focus.
                if (!e.isTemporary) {
                    scene.focusManager.requestFocus()
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
        })

        contentComponent.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit
            override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
            override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
            override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
            override fun mouseExited(event: MouseEvent) = onMouseEvent(event)
        })
        contentComponent.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) = onMouseEvent(event)
            override fun mouseMoved(event: MouseEvent) = onMouseEvent(event)
        })
        contentComponent.addMouseWheelListener { event ->
            onMouseWheelEvent(event)
        }
        contentComponent.focusTraversalKeysEnabled = false
        contentComponent.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) = onKeyEvent(event)
            override fun keyReleased(event: KeyEvent) = onKeyEvent(event)
            override fun keyTyped(event: KeyEvent) = onKeyEvent(event)
        })
    }

    private inline fun catchExceptions(body: () -> Unit) {
        try {
            body()
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

    // Decides which AWT events should be delivered, and which should be filtered out
    private val awtEventFilter = object {

        var isPrimaryButtonPressed = false

        fun shouldSendMouseEvent(event: MouseEvent): Boolean {
            // AWT can send events after the window is disposed
            if (isDisposed)
                return false

            // Filter out mouse events that report the primary button has changed state to pressed,
            // but aren't themselves a mouse press event. This is needed because on macOS, AWT sends
            // us spurious enter/exit events that report the primary button as pressed when resizing
            // the window by its corner/edge. This causes false-positives in detectTapGestures.
            // See https://github.com/JetBrains/compose-multiplatform/issues/2850 for more details.
            val eventReportsPrimaryButtonPressed =
                (event.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
            if ((event.button == MouseEvent.BUTTON1) &&
                ((event.id == MouseEvent.MOUSE_PRESSED) ||
                    (event.id == MouseEvent.MOUSE_RELEASED))) {
                isPrimaryButtonPressed = eventReportsPrimaryButtonPressed  // Update state
            }
            if (eventReportsPrimaryButtonPressed && !isPrimaryButtonPressed) {
                return false  // Ignore such events
            }

            return true
        }

        @Suppress("UNUSED_PARAMETER")
        fun shouldSendKeyEvent(event: KeyEvent): Boolean {
            // AWT can send events after the window is disposed
            return !isDisposed
        }
    }

    private fun onMouseEvent(event: MouseEvent): Unit = catchExceptions {
        if (!awtEventFilter.shouldSendMouseEvent(event))
            return@catchExceptions

        if (keyboardModifiersRequireUpdate) {
            keyboardModifiersRequireUpdate = false
            windowContext.setKeyboardModifiers(event.keyboardModifiers)
        }
        scene.onMouseEvent(density, event)
    }

    private fun onMouseWheelEvent(event: MouseWheelEvent): Unit = catchExceptions {
        if (!awtEventFilter.shouldSendMouseEvent(event))
            return@catchExceptions

        scene.onMouseWheelEvent(density, event)
    }

    private fun onKeyEvent(event: KeyEvent) = catchExceptions {
        if (!awtEventFilter.shouldSendKeyEvent(event))
            return@catchExceptions

        textInputService.onKeyEvent(event)
        windowContext.setKeyboardModifiers(event.toPointerKeyboardModifiers())

        val composeEvent = ComposeKeyEvent(event)
        if (onPreviewKeyEvent(composeEvent) ||
            scene.sendKeyEvent(composeEvent) ||
            onKeyEvent(composeEvent)
        ) {
            event.consume()
        }
    }

    fun dispose() {
        check(!isDisposed)
        isDisposed = true

        container.removeContainerListener(containerListener)
        container.remove(contentComponent)
        container.remove(invisibleComponent)

        scene.close()
        skiaLayerComponent.dispose()
        _onComponentAttached = null
    }

    fun onComponentAttached() {
        onChangeComponentDensity()

        _onComponentAttached?.invoke()
        _onComponentAttached = null
    }

    @OptIn(ExperimentalSkikoApi::class)
    private fun JLayeredPane.addToLayer(component: Component, layer: Int) {
        if (skiaLayerComponent.renderApi == GraphicsApi.METAL && contentComponent !is SkiaSwingLayer) {
            // Applying layer on macOS makes our bridge non-transparent
            // But it draws always on top, so we can just add it as-is
            // TODO: Figure out why it makes difference in transparency
            add(component, 0)
        } else {
            setLayer(component, layer)
            add(component, null, -1)
        }
    }

    fun addToComponentLayer(component: Component) {
        container.addToLayer(component, interopLayer)
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
                scene.setContent(content)
            }
        }
    }

    fun onComposeInvalidation() {
        if (isDisposed) return
        skiaLayerComponent.onComposeInvalidation()
    }

    fun onChangeComponentSize() {
        // Convert AWT scaled size to real pixels.
        val scale = contentComponent.density.density

        // TODO: Recalculate it to window related coordinates
        val boundsInWindow = IntRect(
            top = 0,
            left = 0,
            right = (contentComponent.width * scale).toInt(),
            bottom = (contentComponent.height * scale).toInt()
        )

        // TODO: It should be window content area size
        windowContext.setContainerSize(boundsInWindow.size)

        // Zero size will literally limit scene's content size to zero,
        // so it case of late initialization skip this to avoid extra layout run.
        scene.boundsInWindow = boundsInWindow.takeIf { boundsInWindow.size != IntSize.Zero }
    }

    fun onChangeComponentDensity() {
        if (scene.density != contentComponent.density) {
            scene.density = contentComponent.density
            onChangeComponentSize()
        }
    }

    fun onChangeLayoutDirection(layoutDirection: LayoutDirection) {
        scene.layoutDirection = layoutDirection
    }

    fun onRenderApiChanged(action: () -> Unit) {
        skiaLayerComponent.onRenderApiChanged(action)
    }

    fun onChangeWindowFocus() {
        keyboardModifiersRequireUpdate = true
    }

    private inner class DesktopSkikoView : SkikoView {
        override val input: SkikoInput
            get() = SkikoInput.Empty

        override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            catchExceptions {
                val composeCanvas = canvas.asComposeCanvas()
                scene.render(composeCanvas, nanoTime)
            }
        }
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
                coroutineContext = coroutineContext,
                onFocusReceived = {
                    skiaLayerComponent.requestNativeFocusOnAccessible(it)
                }
            ).also {
                it.syncLoop()
            }
        }

        override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers.remove(semanticsOwner)?.dispose()
        }

        override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
            _accessibilityControllers[semanticsOwner]?.onSemanticsChange()
        }
    }

    private inner class DesktopPlatformContext : PlatformContext by PlatformContext.Empty {
        override val windowInfo: WindowInfo get() = windowContext.windowInfo
        override val isWindowTransparent: Boolean get() = windowContext.isWindowTransparent
        override val viewConfiguration: ViewConfiguration = DesktopViewConfiguration()
        override val textInputService: PlatformTextInputService = this@ComposeSceneMediator.textInputService

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            contentComponent.cursor =
                (pointerIcon as? AwtCursor)?.cursor ?: Cursor(Cursor.DEFAULT_CURSOR)
        }
        override val parentFocusManager: FocusManager = DesktopFocusManager()
        override fun requestFocus(): Boolean {
            return contentComponent.hasFocus() || contentComponent.requestFocusInWindow()
        }

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

    private class InvisibleComponent : Component() {
        fun requestFocusTemporary(): Boolean {
            return super.requestFocus(true)
        }
    }
}

private fun ComposeScene.onMouseEvent(
    density: Float,
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
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event,
        button = event.getPointerButton()
    )
}

private fun MouseEvent.getPointerButton(): PointerButton? {
    if (button == MouseEvent.NOBUTTON) return null
    return when (button) {
        MouseEvent.BUTTON2 -> PointerButton.Tertiary
        MouseEvent.BUTTON3 -> PointerButton.Secondary
        else -> PointerButton(button - 1)
    }
}

private fun ComposeScene.onMouseWheelEvent(
    density: Float,
    event: MouseWheelEvent
) {
    sendPointerEvent(
        eventType = PointerEventType.Scroll,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
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
