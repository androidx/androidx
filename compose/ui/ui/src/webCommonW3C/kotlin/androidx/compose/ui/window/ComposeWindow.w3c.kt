/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.draganddrop.WebDragAndDropManager
import androidx.compose.ui.events.EventTargetListener
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.pointer.BrowserCursor
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.composeButton
import androidx.compose.ui.input.pointer.composeButtons
import androidx.compose.ui.navigationevent.BackNavigationEventInput
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.DefaultInputModeManager
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WebTextInputService
import androidx.compose.ui.platform.WebTextToolbar
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.platform.accessibility.ComposeWebSemanticsListener
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeSceneDragAndDropNode
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.InteropViewGroup
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.TrackInteropPlacementContainer
import androidx.compose.ui.viewinterop.WebInteropContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.enableSavedStateHandles
import kotlin.coroutines.coroutineContext
import kotlin.math.absoluteValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import org.w3c.dom.AddEventListenerOptions
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTitleElement
import org.w3c.dom.MediaQueryListEvent
import org.w3c.dom.Node
import org.w3c.dom.OPEN
import org.w3c.dom.ShadowRootInit
import org.w3c.dom.ShadowRootMode
import org.w3c.dom.TouchEvent
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

private val actualDensity
    get() = window.devicePixelRatio

private abstract external class ExtendedTouchEvent : TouchEvent {
    val force: Double
}

internal interface ComposeWindowState {
    fun init() {}
    fun sizeFlow(): Flow<IntSize>

    val globalEvents: EventTargetListener

    fun dispose() {
        globalEvents.dispose()
    }

    companion object {
        fun createFromLambda(lambda: suspend () -> IntSize): ComposeWindowState {
            return object : ComposeWindowState {
                override val globalEvents = EventTargetListener(window)
                override fun sizeFlow(): Flow<IntSize> = flow {
                    while (coroutineContext.isActive) {
                        emit(lambda())
                    }
                }
            }
        }
    }
}

private sealed interface KeyboardModeState {
    object Virtual: KeyboardModeState
    object Hardware: KeyboardModeState
}

internal class DefaultWindowState(private val viewportContainer: Element) : ComposeWindowState {
    private val channel = Channel<IntSize>(CONFLATED)

    override val globalEvents = EventTargetListener(window)

    override fun init() {

        globalEvents.addDisposableEvent("resize") {
            channel.trySend(getParentContainerBox())
        }

        initMediaEventListener {
            channel.trySend(getParentContainerBox())
        }

        channel.trySend(getParentContainerBox())
    }

    private fun getParentContainerBox(): IntSize {
        return IntSize(viewportContainer.clientWidth, viewportContainer.clientHeight)
    }

    private fun initMediaEventListener(handler: (Double) -> Unit) {
        val contentScale = actualDensity
        window.matchMedia("(resolution: ${contentScale}dppx)")
            .addEventListener("change", { evt ->
                evt as MediaQueryListEvent
                if (!evt.matches) {
                    handler(contentScale)
                }
                initMediaEventListener(handler)
            }, AddEventListenerOptions(capture = true, once = true))
    }

    override fun sizeFlow() = channel.receiveAsFlow()
}

@OptIn(InternalComposeApi::class)
internal class ComposeWindow(
    private val canvas: HTMLCanvasElement,
    private val rootNode: Node,
    private val interopContainerElement: HTMLDivElement,
    private val a11yContainerElement: HTMLDivElement?,
    private val configuration: ComposeViewportConfiguration,
    content: @Composable () -> Unit,
    private val state: ComposeWindowState
) {
    private var isDisposed = false

    private val density: Density = Density(
        density = actualDensity.toFloat(),
        fontScale = 1f
    )

    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }
    @VisibleForTesting
    internal val archComponentsOwner = DefaultArchitectureComponentsOwner()

    private val navigationEventInput = BackNavigationEventInput()

    private val canvasEvents = EventTargetListener(canvas)

    private var keyboardModeState: KeyboardModeState = KeyboardModeState.Hardware

    // Used in WebTextInputService. Also see https://youtrack.jetbrains.com/issue/CMP-8611
    private var activeTouchOffset: Offset? = null

    private val platformContext: PlatformContext = object : PlatformContext by PlatformContext.Empty() {
        override val windowInfo get() = _windowInfo
        override val architectureComponentsOwner get() = archComponentsOwner
        override val inputModeManager: InputModeManager = DefaultInputModeManager()

        override val dragAndDropManager: PlatformDragAndDropManager = object : WebDragAndDropManager(rootNode, canvasEvents, state.globalEvents, density) {
            override val rootDragAndDropNode: ComposeSceneDragAndDropNode
                get() = scene.rootDragAndDropNode
        }

        @Suppress("RedundantOverride")
        override fun convertLocalToWindowPosition(localPosition: Offset): Offset {
            // TODO (o.karpovich): Currently, CfW uses AttachedComposeSceneLayer, so
            // Window Rect == Canvas Rect, although a canvas might take only a portion of the browser's
            // viewport: Window Rect > Canvas Rect.
            // Update this implementation when implementing https://youtrack.jetbrains.com/issue/CMP-8359
            // The implementation will have to rely on the <canvas> of a particular layer.
            return super.convertLocalToWindowPosition(localPosition)
        }

        @Suppress("RedundantOverride")
        override fun convertWindowToLocalPosition(positionInWindow: Offset): Offset {
            // TODO (o.karpovich): Currently, CfW uses AttachedComposeSceneLayer, so
            // Window Rect == Canvas Rect, although a canvas might take only a portion of the browser's
            // viewport: Window Rect > Canvas Rect.
            // Update this implementation when implementing https://youtrack.jetbrains.com/issue/CMP-8359
            return super.convertWindowToLocalPosition(positionInWindow)
        }

        override val textToolbar: TextToolbar = WebTextToolbar()

        override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener? =
            if (configuration.isA11YEnabled) {
                ComposeWebSemanticsListener(
                    coroutineScope = MainScope(),
                    webSemanticsRoot = a11yContainerElement?.apply {
                        setAttribute("aria-label", "")
                        setAttribute("role", "presentation")
                        setAttribute("aria-live", "polite")
                        id = "cmp_a11y_root"
                        style.opacity = "0"
                        style.setProperty("pointer-events", "none")
                    } ?: error("a11yContainerElement must be provided"),
                )
            } else {
                null
            }

        override val textInputService: WebTextInputService = object : WebTextInputService() {

            override val currentTouchOffset: Offset?
                get() = activeTouchOffset

            override val backingDomInputContainer: HTMLElement
                get() = interopContainerElement

            override fun getNewGeometryForBackingInput(rect: Rect): DpRect {
                val dpRect = rect.toDpRect(density)
                val left = dpRect.left.value
                val top = dpRect.top.value

                return DpRect(DpOffset(left.dp, top.dp), dpRect.size)
            }

            override fun processKeyboardEvent(keyEvent: KeyEvent): Boolean {
                //this@ComposeWindow.processKeyboardEvent(keyboardEvent)
                return scene.sendKeyEvent(keyEvent)
            }
        }

        override val viewConfiguration =
            object : ViewConfiguration by PlatformContext.DefaultViewConfiguration {
                override val touchSlop: Float get() = with(density) { 18.dp.toPx() }
            }

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            if (pointerIcon is BrowserCursor) {
                canvas.style.cursor = pointerIcon.id
            }
        }

        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            coroutineScope {
                WebTextInputSession(this, textInputService)
                    .startInputMethod(request)
            }
        }
    }

    private val skiaLayer: SkiaLayer = SkiaLayer().apply {
        renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                scene.render(canvas.asComposeCanvas(), nanoTime)
            }
        }
    }

    private val scene = CanvasLayersComposeScene(
        coroutineContext = Dispatchers.Main,
        platformContext = platformContext,
        density = density,
        invalidate = skiaLayer::needRedraw,
    )

    private val systemThemeObserver = getSystemThemeObserver()

    private fun <T : Event> addTypedEvent(
        type: String,
        handler: (event: T) -> Unit
    ) {
        canvasEvents.addDisposableEvent(type) { event -> handler(event as T) }
    }

    private fun processKeyboardEvent(keyboardEvent: KeyboardEvent) {
        val keyEvent = keyboardEvent.toComposeEvent()
        val processed = scene.sendKeyEvent(keyEvent) ||
            navigationEventInput.onKeyEvent(keyEvent)
        if (processed) keyboardEvent.preventDefault()
    }

    private fun initEvents(canvas: HTMLCanvasElement) {
        var offset = Offset.Zero

        addTypedEvent<TouchEvent>("touchstart") { event ->
            canvas.getBoundingClientRect().apply {
                offset = Offset(x = left.toFloat(), y = top.toFloat())
            }

            onTouchEvent(event, offset)
        }

        addTypedEvent<TouchEvent>("touchmove") { event ->
            onTouchEvent(event, offset)
        }

        addTypedEvent<TouchEvent>("touchend") { event ->
            onTouchEvent(event, offset)
        }

        addTypedEvent<TouchEvent>("touchcancel") { event ->
            onTouchEvent(event, offset)
        }

        addTypedEvent<MouseEvent>("mousedown") { event ->
            onMouseEvent(event)
        }

        addTypedEvent<MouseEvent>("mouseup") { event ->
            onMouseEvent(event)
        }

        addTypedEvent<MouseEvent>("mousemove") { event ->
            onMouseEvent(event)
        }

        addTypedEvent<MouseEvent>("mouseenter") { event ->
            onMouseEvent(event)
        }

        addTypedEvent<MouseEvent>("mouseleave") { event ->
            onMouseEvent(event)
        }

        addTypedEvent<WheelEvent>("wheel") { event ->
            onWheelEvent(event)
        }

        canvas.addEventListener("contextmenu", { event ->
            event.preventDefault()
        })

        addTypedEvent<KeyboardEvent>("keydown") { event ->
            processKeyboardEvent(event)
        }

        addTypedEvent<KeyboardEvent>("keyup") { event ->
            processKeyboardEvent(event)
        }

        state.globalEvents.addDisposableEvent("focus") {
            archComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        state.globalEvents.addDisposableEvent("blur") {
            archComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        state.globalEvents.addDisposableEvent("visibilitychange") { event ->
            archComponentsOwner.lifecycle.handleLifecycleEvent(
                if (documentIsVisible()) Lifecycle.Event.ON_START
                else Lifecycle.Event.ON_STOP
            )
        }
    }

    init {
        initEvents(canvas)
        state.init()

        canvas.setAttribute("tabindex", "0")
        canvas.setAttribute("draggable", "true")

        scene.density = density
        archComponentsOwner.enableSavedStateHandles()

        val interopContainer = WebInteropContainer(InteropViewGroup(interopContainerElement))

        scene.setContent {
            CompositionLocalProvider(
                LocalSystemTheme provides systemThemeObserver.currentSystemTheme.value,
                LocalInteropContainer provides interopContainer,
                LocalActiveClipEventsTarget provides {
                    (platformContext.textInputService as WebTextInputService).getBackingInput() ?: canvas
                },
                content = {
                    interopContainer.TrackInteropPlacementContainer {
                        content()
                    }

                    rememberCoroutineScope().launch {
                        state.sizeFlow().collect { size ->
                            // Convert to proper type: IntSize was exposed to public API with meaning of DPs.
                            val boxSize = DpSize(size.width.dp, size.height.dp)
                            this@ComposeWindow.resize(boxSize)
                        }
                    }
                }
            )
        }

        archComponentsOwner.lifecycle.handleLifecycleEvent(
            if (document.hasFocus()) Lifecycle.Event.ON_RESUME
            else Lifecycle.Event.ON_START
        )
        archComponentsOwner.navigationEventDispatcherOwner
            .navigationEventDispatcher.addInput(navigationEventInput)
    }

    private fun resize(boxSize: DpSize) {
        val sizeInPx = boxSize.toSize(density).toIntSize()

        canvas.width = sizeInPx.width
        canvas.height = sizeInPx.height

        // Scale canvas to allow high DPI rendering as suggested in
        // https://www.khronos.org/webgl/wiki/HandlingHighDPI.
        canvas.style.width = "${boxSize.width.value}px"
        canvas.style.height = "${boxSize.height.value}px"

        _windowInfo.containerSize = sizeInPx
        _windowInfo.containerDpSize = boxSize

        // TODO: Align with Container/Mediator architecture
        skiaLayer.attachTo(canvas)
        scene.size = sizeInPx
        skiaLayer.needRedraw()
    }

    // TODO: need to call .dispose() on window close.
    fun dispose() {
        check(!isDisposed)
        archComponentsOwner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        archComponentsOwner.viewModelStore.clear()
        archComponentsOwner.navigationEventDispatcherOwner
            .navigationEventDispatcher.removeInput(navigationEventInput)

        scene.close()
        skiaLayer.detach()

        systemThemeObserver.dispose()
        state.dispose()
        // modern browsers supposed to garbage collect all events on the element disposed
        // but actually we never can be sure dom element was collected in first place
        canvasEvents.dispose()
        isDisposed = true
    }

    private fun onTouchEvent(
        event: TouchEvent,
        offset: Offset,
    ) {
        // iOS Safari doesn't request focus when the page is shown,
        // and the lifecycle doesn't trigger ON_RESUME.
        // so, we decided to handle every touch
        archComponentsOwner.lifecycle.currentState = Lifecycle.State.RESUMED

        val inputModeManager = platformContext.inputModeManager
        if (inputModeManager.inputMode != InputMode.Touch) {
            inputModeManager.requestInputMode(InputMode.Touch)
        }

        keyboardModeState = KeyboardModeState.Virtual
        val eventType = when (event.type) {
            "touchstart" -> PointerEventType.Press
            "touchmove" -> PointerEventType.Move
            "touchend", "touchcancel" -> PointerEventType.Release
            else -> PointerEventType.Unknown
        }

        /**
         * The set of touches needed for compose are:
         * - targetTouches: contains all pressed touches for the current target element
         * - changedTouches when the event is 'touchend' or 'touchcancel': contains released touches
         */
        val touches = event.targetTouches.asList().fastMap { it to true }.toMutableList()
        if (eventType == PointerEventType.Release) {
            touches.addAll(event.changedTouches.asList().fastMap { it to false } )
        }

        val pointers = touches.fastMap { (touch, pressed) ->
            ComposeScenePointer(
                id = PointerId(touch.identifier.toLong()),
                position = Offset(
                    x = touch.clientX - offset.x,
                    y = touch.clientY - offset.y
                ) * density.density,
                pressed = pressed,
                type = PointerType.Touch,
                pressure = touch.unsafeCast<ExtendedTouchEvent>().force.toFloat()
            )
        }

        activeTouchOffset = pointers.firstOrNull()?.position
        val result = scene.sendPointerEvent(
            eventType = eventType,
            pointers = pointers,
            buttons = PointerButtons(),
            keyboardModifiers = PointerKeyboardModifiers(),
            scrollDelta = Offset.Zero,
            nativeEvent = event,
            button = null
        )
        activeTouchOffset = null

        if (result.anyChangeConsumed) {
            event.preventDefault()
        }
    }

    private fun onMouseEvent(
        event: MouseEvent,
    ) {
        keyboardModeState = KeyboardModeState.Hardware
        val eventType = when (event.type) {
            "mousedown" -> PointerEventType.Press
            "mousemove" -> PointerEventType.Move
            "mouseup" -> PointerEventType.Release
            "mouseenter" -> PointerEventType.Enter
            "mouseleave" -> PointerEventType.Exit
            else -> PointerEventType.Unknown
        }
        scene.sendPointerEvent(
            eventType = eventType,
            position = event.offset,
            buttons = event.composeButtons,
            keyboardModifiers = PointerKeyboardModifiers(
                isCtrlPressed = event.ctrlKey,
                isMetaPressed = event.metaKey,
                isAltPressed = event.altKey,
                isShiftPressed = event.shiftKey,
            ),
            nativeEvent = event,
            button = event.composeButton,
        )
    }

    private fun onWheelEvent(
        event: WheelEvent,
    ) {
        keyboardModeState = KeyboardModeState.Hardware

        val horizontalScroll = when {
            event.deltaX.absoluteValue >= event.deltaY.absoluteValue -> event.deltaX
            event.shiftKey -> event.deltaY
            else -> 0f
        }

        val verticalScroll = if (horizontalScroll == 0f) event.deltaY else 0f

        val result = scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = event.offset,
            scrollDelta = Offset(
                x = horizontalScroll.toFloat(),
                y = verticalScroll.toFloat()
            ),
            buttons = event.composeButtons,
            keyboardModifiers = PointerKeyboardModifiers(
                isCtrlPressed = event.ctrlKey,
                isMetaPressed = event.metaKey,
                isAltPressed = event.altKey,
                isShiftPressed = event.shiftKey,
            ),
            nativeEvent = event,
            button = event.composeButton,
        )

        if (result.anyChangeConsumed) event.preventDefault()
    }

    private val MouseEvent.offset get() = Offset(
        x = offsetX.toFloat(),
        y = offsetY.toFloat()
    ) * density.density
}

//https://developer.mozilla.org/en-US/docs/Web/API/Document/visibilityState
private fun documentIsVisible(): Boolean = js("document.visibilityState === 'visible'")

private const val defaultCanvasElementId = "ComposeTarget"

/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Initializes the composition in HTML canvas identified by [canvasElementId].
 *
 * It can be resized by providing [requestResize].
 * By default, it will listen to the window resize events.
 *
 * By default, styles will be applied to use the entire inner window, disabling scrollbars.
 * This can be turned off by setting [applyDefaultStyles] to false.
 */
@ExperimentalComposeUiApi
@Deprecated(
message = "CanvasBasedWindow doesn't support HTML interop via WebElementView API and it doesn't support A11Y feature. Use ComposeViewport API instead",
replaceWith = ReplaceWith("ComposeViewport(content = content)"),
level = DeprecationLevel.ERROR)
fun CanvasBasedWindow(
    title: String? = null,
    canvasElementId: String = defaultCanvasElementId,
    requestResize: (suspend () -> IntSize)? = null,
    applyDefaultStyles: Boolean = true,
    content: @Composable () -> Unit = { }
) {
    if (title != null) {
        val htmlTitleElement = (
            document.head!!.getElementsByTagName("title").item(0)
                ?: document.createElement("title").also { document.head!!.appendChild(it) }
            ) as HTMLTitleElement
        htmlTitleElement.textContent = title
    }

    if (applyDefaultStyles) {
        document.head!!.appendChild(
            (document.createElement("style") as HTMLStyleElement).apply {
                type = "text/css"
                appendChild(
                    document.createTextNode(
                        "body { margin: 0; overflow: hidden; } #$canvasElementId { outline: none; }"
                    )
                )
            }
        )
    }

    val canvas = document.getElementById(canvasElementId)?.let { it as HTMLCanvasElement }
        ?: error("failed to find element with id '$canvasElementId'")

    val configuration = ComposeViewportConfiguration().apply {
        // No a11y support with deprecated CanvasBasedWindow API
        isA11YEnabled = false
    }

    ComposeWindow(
        canvas = canvas,
        rootNode = canvas.getRootNode(),
        // a detached container
        interopContainerElement = document.createElement("div") as HTMLDivElement,
        a11yContainerElement = document.createElement("div") as HTMLDivElement,
        content = content,
        configuration = configuration,
        state = if (requestResize == null) DefaultWindowState(document.documentElement!!) else ComposeWindowState.createFromLambda(requestResize)
    )
}

internal actual fun InternalComposeViewport(
    viewportContainerId: String?,
    configure: ComposeViewportConfiguration.() -> Unit,
    content: @Composable () -> Unit
) {
    onDomReady {
        val providedContainer = if (viewportContainerId != null) {
            document.getElementById(viewportContainerId) ?: error("failed to find element by viewportContainerId: '$viewportContainerId'")
        } else {
            document.body ?: error("failed to find <body> element")
        }

        ComposeViewport(providedContainer, configure, content)
    }
}

/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Creates the composition in HTML canvas created in parent container identified by [viewportContainer] Element.
 * This size of canvas is adjusted with the size of the container
 *
 * The current hierarchy:
 * <viewportContainer.shadowDom>
 *     <app root>
 *         <canvas/>
 *         <interop elements container/>
 *         <a11y elements root/>
 *     </app root>
 * </viewportContainer.shadowDom>
 */
@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainer: Element,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) = onSkikoReady {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.setAttribute("tabindex", "0")
    canvas.setAttribute("role", "generic")
    canvas.style.outline = "none" // Fixes https://youtrack.jetbrains.com/issue/CMP-9040

    // Create a common container (parent html element) for canvas and the interop container
    // to position at the same place - the interop container is position at 0,0 relative to <canvas>.
    // It simplifies the positioning of the interop views in the container.
    val layerRoot = document.createElement("div") as HTMLElement
    layerRoot.style.apply {
        position = "relative"
    }

    val shadowRoot = viewportContainer.attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
    val shadowRootStyle = document.createElement("style")
    shadowRootStyle.textContent = """
        :host {
            -webkit-touch-callout: none; 
            -webkit-user-select: none; 
            user-select: none;
            
            position: relative;
        }
    """.trimIndent()

    shadowRoot.append(shadowRootStyle, layerRoot)
    layerRoot.appendChild(canvas)

    val interopContainerElement = document.createElement("div") as HTMLDivElement
    layerRoot.appendChild(interopContainerElement)

    interopContainerElement.style.apply {
        position = "absolute"
        top = "0"
        left = "0"
    }

    val configuration = ComposeViewportConfiguration().apply(configure)

    val a11yContainerElement = if (configuration.isA11YEnabled) {
        (document.createElement("div") as HTMLDivElement).also { a11yContainer ->
            layerRoot.appendChild(a11yContainer)
            a11yContainer.style.apply {
                position = "absolute"
                top = "0"
                left = "0"
            }
        }
    } else {
        null
    }

    ComposeWindow(
        canvas = canvas,
        rootNode = shadowRoot,
        interopContainerElement = interopContainerElement,
        a11yContainerElement = a11yContainerElement,
        content = content,
        configuration = configuration,
        state = DefaultWindowState(viewportContainer)
    )
}