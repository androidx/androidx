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

@file:OptIn(ExperimentalWasmJsInterop::class)

package androidx.compose.ui.window

import androidx.annotation.VisibleForTesting
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.draganddrop.WebDragAndDropManager
import androidx.compose.ui.events.EventTargetListener
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.BrowserCursor
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.composeButton
import androidx.compose.ui.input.pointer.composeButtons
import androidx.compose.ui.internal.focusExt
import androidx.compose.ui.navigationevent.BackNavigationEventInput
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WebHapticFeedback
import androidx.compose.ui.platform.WebTextInputService
import androidx.compose.ui.platform.WebTextToolbar
import androidx.compose.ui.platform.WebWakeLockManager
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.platform.accessibility.ComposeWebSemanticsListener
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeSceneDragAndDropNode
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.scene.PointerEventResult
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
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.InteropViewGroup
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.TrackInteropPlacementContainer
import androidx.compose.ui.viewinterop.WebInteropContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.enableSavedStateHandles
import kotlin.math.absoluteValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.hostOs
import org.w3c.dom.AddEventListenerOptions
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.LOADING
import org.w3c.dom.MediaQueryListEvent
import org.w3c.dom.Node
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.pointerevents.PointerEvent

private val actualDensity
    get() = window.devicePixelRatio

internal interface ComposeWindowState {
    fun init() {}
    fun sizeFlow(): Flow<IntSize>

    val globalEvents: EventTargetListener

    fun dispose() {
        globalEvents.dispose()
    }
}

private sealed interface KeyboardModeState {
    object Virtual : KeyboardModeState
    object Hardware : KeyboardModeState
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
    private val layerRoot: HTMLElement,
    private val interopContainerElement: HTMLDivElement,
    private val a11yContainerElement: HTMLDivElement?,
    private val configuration: ComposeViewportConfiguration,
    content: @Composable () -> Unit,
    private val state: ComposeWindowState
) {
    private var isDisposed = false

    private var actualActivePointerButtons: PointerButtons? = null

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

    private val clipTarget = clipTargetElement(canvas)

    private val platformContext: PlatformContext =
        object : PlatformContext by PlatformContext.Empty() {
            override val windowInfo get() = _windowInfo
            override val architectureComponentsOwner get() = archComponentsOwner

            override val dragAndDropManager: PlatformDragAndDropManager = object :
                WebDragAndDropManager(rootNode, canvasEvents, state.globalEvents, density) {
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

            override val textToolbar: TextToolbar by lazy(LazyThreadSafetyMode.NONE) {
                WebTextToolbar()
            }

            override val hapticFeedback by lazy(LazyThreadSafetyMode.NONE) {
                WebHapticFeedback.webHapticFeedbackOrDefault()
            }

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

            override val textInputService: WebTextInputService by lazy(LazyThreadSafetyMode.NONE) {
                object : WebTextInputService() {

                    override val currentTouchOffset: Offset?
                        get() = activeTouchOffset

                    override val backingDomInputContainer: HTMLElement
                        get() = layerRoot

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
            }

            override val viewConfiguration =
                object : ViewConfiguration by PlatformContext.DefaultViewConfiguration {
                    override val touchSlop: Float get() = with(density) { 18.dp.toPx() }
                    override val maximumFlingVelocity: Float
                        //https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/core/java/android/view/ViewConfiguration.java;l=240;drc=733537294b158d22f2ae383f2ed77c93741798e9
                        get() = with(density) { 8000.dp.toPx() }
                }

            override var isKeepScreenOnEnabled: Boolean
                get() = WebWakeLockManager.isWakeLockActive()
                set(value) = WebWakeLockManager.sendWakeLockRequest(this@ComposeWindow, value)

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

            override val isClearFocusOnMouseDownEnabled: Boolean
                get() = configuration.isClearFocusOnMouseDownEnabled
        }

    private val skiaLayer: SkiaLayer = SkiaLayer().apply {
        renderDelegate = SkikoRenderDelegate { canvas, _, _, nanoTime ->
            scene.render(canvas.asComposeCanvas(), nanoTime)
        }
    }

    private val scene = CanvasLayersComposeScene(
        coroutineContext = Dispatchers.Main,
        platformContext = platformContext,
        density = density,
        invalidate = skiaLayer::needRender,
    )

    private val systemThemeObserver = getSystemThemeObserver()

    private fun <T : Event> addTypedEvent(
        type: String,
        handler: (event: T) -> Unit
    ) {
        canvasEvents.addDisposableEvent(type) { event -> handler(event as T) }
    }

    private fun <T : Event> addTypedEvent(
        type: String,
        passive: Boolean,
        handler: (event: T) -> Unit
    ) {
        canvasEvents.addDisposableEvent(type, passive) { event -> handler(event as T) }
    }

    private fun processKeyboardEvent(keyboardEvent: KeyboardEvent) {
        val keyEvent = keyboardEvent.toComposeEvent()
        val processed = scene.sendKeyEvent(keyEvent) ||
            navigationEventInput.onKeyEvent(keyEvent)

        if (processed) {
            keyboardEvent.preventDefault()
        } else if (keyEvent.type == KeyEventType.KeyDown) {
            processClipKeyDown(keyEvent)
        }
    }

    private val isMacOS = hostOs.isMacOS

    private var canvasFocused = false

    private fun processClipKeyDown(keyEvent: KeyEvent) {
        val mod = if (isMacOS) keyEvent.isMetaPressed else keyEvent.isCtrlPressed
        if (!mod) return
        if (keyEvent.key == Key.C || keyEvent.key == Key.V || keyEvent.key == Key.X) {
            // A browser is about to dispatch a Clipboard Event.
            // Some browsers do not dispatch Clipboard events to <canvas> despite it having focus,
            // so let it dispatch the event to clipTarget (text area).
            // By focusing on it, we let a browser dispatch the event to it.
            layerRoot.appendChild(clipTarget)
            focusExt(clipTarget, true)
        }
    }

    private fun initEvents(canvas: HTMLCanvasElement) {

        listOf(
            "pointerenter",
            "pointerdown",
            "pointermove",
            "pointerup",
            "pointerleave",
            "pointercancel"
        ).forEach { name ->
            addTypedEvent<PointerEvent>(name, passive = false) { onPointerEvent(it) }
        }

        state.globalEvents.addDisposableEvent("dragend") {
            // in Safari pointerup event is not firing when we drop or cancel drop
            // see https://youtrack.jetbrains.com/issue/CMP-10102
            actualActivePointerButtons = null
        }

        addTypedEvent<TouchEvent>("touchstart") { evt ->
            // in most cases we don't care about touches since in Compose we do not process them at all
            // there's one case however when we need to cancel them - it's when we are focussed in a DOM backing field
            // see https://youtrack.jetbrains.com/issue/CMP-10079

            val backingInput = (platformContext.textInputService as WebTextInputService).getBackingInput()
            if (backingInput?.isFocused() == true) {
                evt.preventDefault()
            }
        }

        addTypedEvent<WheelEvent>("wheel", passive = false) { event ->
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

        addTypedEvent<FocusEvent>("focus") { event ->
            canvasFocused = true
        }

        addTypedEvent<FocusEvent>("blur") { event ->
            canvasFocused = false
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

        val clipEventsTargetProvider: () -> HTMLElement = {
            (platformContext.textInputService as WebTextInputService).getBackingInput()
                ?: clipTarget
        }
        scene.setContent {
            CompositionLocalProvider(
                LocalSystemTheme provides systemThemeObserver.currentSystemTheme.value,
                LocalInteropContainer provides interopContainer,
                LocalActiveClipEventsTarget provides clipEventsTargetProvider,
                content = {
                    interopContainer.TrackInteropPlacementContainer {
                        content()
                    }

                    LaunchedEffect(Unit) {
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
        skiaLayer.needRender()
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

    private inner class TouchEventWithContainerOffset(
        val event: PointerEvent,
        val containerOffset: Offset
    ) {
        val composePointer = event.toScenePointerEvent(containerOffset, density)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as TouchEventWithContainerOffset

            if (event != other.event) return false
            if (containerOffset != other.containerOffset) return false

            return true
        }

        override fun hashCode(): Int {
            var result = event.hashCode()
            result = 31 * result + containerOffset.hashCode()
            return result
        }
    }

    private val activeTouchPointers = mutableIntObjectMapOf<TouchEventWithContainerOffset>()
    private val reusableTouchPointerList = mutableListOf<ComposeScenePointer>()
    private fun getActivePointers(): MutableList<ComposeScenePointer> {
        reusableTouchPointerList.clear()
        activeTouchPointers.forEachValue {
            reusableTouchPointerList.add(it.composePointer)
        }
        return reusableTouchPointerList
    }

    private fun onPointerEvent(event: PointerEvent) {
        val eventType = event.getPointerEventType()
        var result: PointerEventResult? = null

        if (isMouseEvent(event)) {
            keyboardModeState = KeyboardModeState.Hardware

            // validate event before sending it further - see
            // https://youtrack.jetbrains.com/issue/CMP-8430/Sequence-of-Move-PointerInputEvents-cancel-out-press-PointerInputEvent-under-certain-conditions

            var isValidEvent = true
            when (eventType) {
                PointerEventType.Press -> {
                    actualActivePointerButtons = event.composeButtons
                }
                PointerEventType.Release -> {
                    actualActivePointerButtons = null
                }
                PointerEventType.Move -> {
                    isValidEvent = actualActivePointerButtons == null || actualActivePointerButtons == event.composeButtons
                }
            }

            if (!isValidEvent) return

            scene.sendPointerEvent(
                eventType = eventType,
                position = event.offset,
                timeMillis = event.timeStamp.toInt().toLong(),
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
        } else {
            if (eventType == PointerEventType.Enter || eventType == PointerEventType.Exit) {
                //Enter and Exit events have no sense for touches (Firefox and Safari send them)
                return
            }

            // iOS Safari doesn't request focus when the page is shown,
            // and the lifecycle doesn't trigger ON_RESUME.
            // so, we decided to handle every touch
            archComponentsOwner.lifecycle.currentState = Lifecycle.State.RESUMED

            val inputModeManager = platformContext.inputModeManager
            if (inputModeManager.inputMode != InputMode.Touch) {
                inputModeManager.requestInputMode(InputMode.Touch)
            }
            keyboardModeState = KeyboardModeState.Virtual

            val current: TouchEventWithContainerOffset
            val active = activeTouchPointers[event.pointerId]
            if (active == null) {
                event.target?.let { setPointerCapture(it, event.pointerId) }
                val containerOffset = canvas.getBoundingClientRect().let {
                    Offset(it.left.toFloat(), it.top.toFloat())
                }
                current = TouchEventWithContainerOffset(event, containerOffset)
            } else {
                current = TouchEventWithContainerOffset(event, active.containerOffset)
            }
            activeTouchPointers[event.pointerId] = current

            activeTouchOffset = current.composePointer.position

            val pointers = getActivePointers()
            val buttons = PointerButtons()
            val keyboardModifiers = PointerKeyboardModifiers()

            var coalescedEvents: List<PointerEvent>? = null
            if (eventType == PointerEventType.Move) {
                coalescedEvents = getCoalescedEvents(event).toList()
            }

            if (coalescedEvents != null && coalescedEvents.size > 1) {
                var indexOfCurrentPointer = -1
                for (index in pointers.indices) {
                    if (pointers[index] == current.composePointer) {
                        indexOfCurrentPointer = index
                        break
                    }
                }

                coalescedEvents.fastForEach { coalescedEvent ->
                    val coalescedEventType = coalescedEvent.getPointerEventType()
                    val sceneEvent = coalescedEvent.toScenePointerEvent(current.containerOffset, density)
                    pointers[indexOfCurrentPointer] = sceneEvent
                    result = scene.sendPointerEvent(
                        eventType = coalescedEventType,
                        pointers = pointers,
                        buttons = buttons,
                        keyboardModifiers = keyboardModifiers,
                        scrollDelta = Offset.Zero,
                        timeMillis = coalescedEvent.timeStamp.toInt().toLong(),
                        nativeEvent = coalescedEvent,
                        button = null
                    )
                }
            } else {
                result = scene.sendPointerEvent(
                    eventType = eventType,
                    pointers = pointers,
                    buttons = buttons,
                    keyboardModifiers = keyboardModifiers,
                    scrollDelta = Offset.Zero,
                    timeMillis = event.timeStamp.toInt().toLong(),
                    nativeEvent = event,
                    button = null
                )
            }

            activeTouchOffset = null

            if (eventType == PointerEventType.Release) {
                activeTouchPointers.remove(event.pointerId)
            }

            if (result != null && result.anyChangeConsumed && event.cancelable) {
                event.preventDefault()
            }
        }
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

        // wheels event own buttons property is unreliable in Safari and Firefox
        // see CMP-9900 [web] Wheel event resolves buttons state incorrectly in Safari and Firefox
        val buttons = actualActivePointerButtons ?: event.composeButtons

        val result = scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = event.offset,
            scrollDelta = Offset(
                x = horizontalScroll.toFloat(),
                y = verticalScroll.toFloat()
            ),
            buttons = buttons,
            keyboardModifiers = PointerKeyboardModifiers(
                isCtrlPressed = event.ctrlKey,
                isMetaPressed = event.metaKey,
                isAltPressed = event.altKey,
                isShiftPressed = event.shiftKey,
            ),
            nativeEvent = event,
            button = event.composeButton,
        )

        if (result.anyChangeConsumed && event.cancelable) {
            event.preventDefault()
        }
    }

    private val MouseEvent.offset
        get() = Offset(
            x = offsetX.toFloat() * density.density,
            y = offsetY.toFloat() * density.density
        )
}

//https://developer.mozilla.org/en-US/docs/Web/API/Document/visibilityState
internal fun documentIsVisible(): Boolean = js("document.visibilityState === 'visible'")

// In K/JS target, an application can't start right away. We should wait until skiko.wasm is ready.
// We'll do it implicitly, rather than asking the app developers to call it.
internal fun onSkikoReady(block: () -> Unit) {
    @Suppress("INVISIBLE_REFERENCE")
    org.jetbrains.skiko.wasm.onWasmReady { block() }
}

internal fun onDomReady(block: () -> Unit) {
    // https://developer.mozilla.org/en-US/docs/Web/API/Document/DOMContentLoaded_event
    if (document.readyState == DocumentReadyState.LOADING) {
        document.addEventListener("DOMContentLoaded", {
            block()
        })
    } else {
        block()
    }
}

private fun setPointerCapture(target: EventTarget, pointerId: Int) {
    js("try { target.setPointerCapture(pointerId) } catch (e) {}")
}

private fun getCoalescedEvents(pointerEvent: PointerEvent): JsArray<PointerEvent> =
    js("pointerEvent.getCoalescedEvents ? pointerEvent.getCoalescedEvents() : []")

private fun PointerEvent.toScenePointerEvent(
    containerOffset: Offset,
    density: Density,
    pointerType: PointerType = PointerType.Touch
): ComposeScenePointer {
    val event = this
    val type = event.getPointerEventType()
    val position = Offset(
        x = (event.clientX - containerOffset.x) * density.density,
        y = (event.clientY - containerOffset.y) * density.density
    )
    return ComposeScenePointer(
        id = PointerId(event.pointerId.toLong()),
        position = position,
        pressed = type == PointerEventType.Press || type == PointerEventType.Move,
        type = pointerType,
        pressure = event.pressure
    )
}

/**
 * The purpose of the clipTarget element is to briefly steal the focus to let the browser dispatch
 * ClipboardEvent to it. Then it returns the focus to the canvas.
 */
private fun clipTargetElement(canvas: HTMLCanvasElement): HTMLTextAreaElement {
    val clipTarget = (document.createElement("textarea") as HTMLTextAreaElement).apply {
        tabIndex = -1
        setAttribute("aria-hidden", "true")
        style.position = "fixed"
        style.left = "-1000px"
        style.top = "0"
        style.opacity = "0"
        style.width = "1px"
        style.height = "1px"
    }

    val clipEventListener: (Event) -> Unit = { _ ->
        window.requestAnimationFrame {
            focusExt(canvas, true)
            clipTarget.remove()
        }
    }

    // Here just return the focus to canvas.
    // For the actual event handling see rememberClipboardEventsHandler implementations.
    clipTarget.addEventListener("copy", clipEventListener)
    clipTarget.addEventListener("cut", clipEventListener)
    clipTarget.addEventListener("paste", clipEventListener)

    return clipTarget
}

// strings checks are faster on a JS side
// language=js
private fun isMouseEvent(event: PointerEvent): Boolean = js("event.pointerType === 'mouse'")

// strings checks are faster on a JS side
// language=js
private fun getPointerEventCode(event: PointerEvent): Int = js(
    """{
        switch (event.type) {
          case 'pointerdown':
            return 1; // PointerEventType.Press
          case 'pointerup':
          case 'pointercancel':
            return 2; // PointerEventType.Release
          case 'pointermove':
            return 3; // PointerEventType.Move
          case 'pointerenter':
            return 4; //PointerEventType.Enter
          case 'pointerleave':
            return 5; //PointerEventType.Exit
          default:
            return 0; // PointerEventType.Unknown
        } 
    }"""
)

private fun PointerEvent.getPointerEventType(): PointerEventType =
    when (getPointerEventCode(this)) {
        PointerEventType.Press.value -> PointerEventType.Press
        PointerEventType.Release.value -> PointerEventType.Release
        PointerEventType.Move.value -> PointerEventType.Move
        PointerEventType.Enter.value -> PointerEventType.Enter
        PointerEventType.Exit.value -> PointerEventType.Exit
        else -> PointerEventType.Unknown
    }

private fun Element.isFocused(): Boolean {
    val activeElement = when {
        document.activeElement?.shadowRoot != null -> (document.activeElement?.shadowRoot as? ShadowRootExt)?.activeElement
        else -> document.activeElement
    }

    if (activeElement == null) {
        return false
    }

    return activeElement == this
}

private external interface ShadowRootExt {
    val activeElement: Element?
}

