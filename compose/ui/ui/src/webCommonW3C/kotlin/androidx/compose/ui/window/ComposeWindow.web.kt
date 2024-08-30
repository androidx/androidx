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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.events.EventTargetListener
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
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
import androidx.compose.ui.native.ComposeLayer
import androidx.compose.ui.platform.DefaultInputModeManager
import androidx.compose.ui.platform.LocalInternalViewModelStoreOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WebTextInputService
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.scene.platformContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.coroutineContext
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
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
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.HTMLTitleElement
import org.w3c.dom.MediaQueryListEvent
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
    content: @Composable () -> Unit,
    private val state: ComposeWindowState
) : LifecycleOwner, ViewModelStoreOwner {
    private var isDisposed = false

    private val density: Density = Density(
        density = actualDensity.toFloat(),
        fontScale = 1f
    )

    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }

    private val canvasEvents = EventTargetListener(canvas)

    private var keyboardModeState: KeyboardModeState = KeyboardModeState.Hardware

    private val platformContext: PlatformContext = object : PlatformContext {
        override val windowInfo get() = _windowInfo

        override val inputModeManager: InputModeManager = DefaultInputModeManager()

        override val textInputService = object : WebTextInputService() {
            override fun isVirtualKeyboard() = keyboardModeState == KeyboardModeState.Virtual

            override fun getOffset(rect: Rect): Offset {
                val viewportRect = canvas.getBoundingClientRect()
                val offsetX = viewportRect.left.toFloat().coerceAtLeast(0f) + (rect.left / density.density)
                val offsetY = viewportRect.top.toFloat().coerceAtLeast(0f) + (rect.top / density.density)
                return Offset(offsetX, offsetY)
            }

            override fun processKeyboardEvent(keyboardEvent: KeyboardEvent) {
                this@ComposeWindow.processKeyboardEvent(keyboardEvent)
            }
        }

        override val viewConfiguration =
            object : ViewConfiguration by PlatformContext.Empty.viewConfiguration {
                override val touchSlop: Float get() = with(density) { 18.dp.toPx() }
            }

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            if (pointerIcon is BrowserCursor) {
                canvas.style.cursor = pointerIcon.id
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
        composeSceneContext = object : ComposeSceneContext {
            override val platformContext get() = this@ComposeWindow.platformContext
        },
        density = density,
        invalidate = skiaLayer::needRedraw,
    )

    private val layer = ComposeLayer(
        layer = skiaLayer,
        scene = scene
    )

    private val systemThemeObserver = getSystemThemeObserver()

    override val lifecycle = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()

    private fun <T : Event> addTypedEvent(
        type: String,
        handler: (event: T) -> Unit
    ) {
        canvasEvents.addDisposableEvent(type) { event -> handler(event as T) }
    }

    private fun processKeyboardEvent(keyboardEvent: KeyboardEvent) {
        val processed = scene.sendKeyEvent(keyboardEvent.toComposeEvent())
        if (processed) keyboardEvent.preventDefault()
    }

    private fun initEvents(canvas: HTMLCanvasElement) {
        var offset = Offset.Zero

        addTypedEvent<TouchEvent>("touchstart") { event ->
            event.preventDefault()

            canvas.getBoundingClientRect().apply {
                offset = Offset(x = left.toFloat(), y = top.toFloat())
            }

            onTouchEvent(event, offset)
        }

        addTypedEvent<TouchEvent>("touchmove") { event ->
            event.preventDefault()
            onTouchEvent(event, offset)
        }

        addTypedEvent<TouchEvent>("touchend") { event ->
            event.preventDefault()
            onTouchEvent(event, offset)
        }

        addTypedEvent<TouchEvent>("touchcancel") { event ->
            event.preventDefault()
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
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        state.globalEvents.addDisposableEvent("blur") {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
    }

    init {
        initEvents(canvas)
        state.init()

        canvas.setAttribute("tabindex", "0")

        scene.density = density

        layer.setContent {
            CompositionLocalProvider(
                LocalSystemTheme provides systemThemeObserver.currentSystemTheme.value,
                LocalLifecycleOwner provides this,
                LocalInternalViewModelStoreOwner provides this,
                content = {
                    content()
                    rememberCoroutineScope().launch {
                        state.sizeFlow().collect { size ->
                            this@ComposeWindow.resize(size)
                        }
                    }
                }
            )
        }

        lifecycle.handleLifecycleEvent(if (document.hasFocus()) Lifecycle.Event.ON_RESUME else Lifecycle.Event.ON_START)
    }

    fun resize(boxSize: IntSize) {
        val scale = density.density

        val width = (boxSize.width * scale).toInt()
        val height = (boxSize.height * scale).toInt()

        canvas.width = width
        canvas.height = height

        // Scale canvas to allow high DPI rendering as suggested in
        // https://www.khronos.org/webgl/wiki/HandlingHighDPI.
        canvas.style.width = "${boxSize.width}px"
        canvas.style.height = "${boxSize.height}px"

        _windowInfo.containerSize = IntSize(width, height)

        layer.layer.attachTo(canvas)
        layer.setSize(width, height)
        layer.layer.needRedraw()
    }

    // TODO: need to call .dispose() on window close.
    fun dispose() {
        check(!isDisposed)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()

        scene.close()
        layer.dispose()

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
        val inputModeManager = scene.platformContext.inputModeManager
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
        val pointers = event.changedTouches.asList().map { touch ->
            ComposeScenePointer(
                id = PointerId(touch.identifier.toLong()),
                position = Offset(
                    x = touch.clientX - offset.x,
                    y = touch.clientY - offset.y
                ) * density.density,
                pressed = when (eventType) {
                    PointerEventType.Press, PointerEventType.Move -> true
                    else -> false
                },
                type = PointerType.Touch,
                pressure = touch.unsafeCast<ExtendedTouchEvent>().force.toFloat()
            )
        }

        scene.sendPointerEvent(
            eventType = eventType,
            pointers = pointers,
            buttons = PointerButtons(),
            keyboardModifiers = PointerKeyboardModifiers(),
            scrollDelta = Offset.Zero,
            nativeEvent = event,
            button = null
        )

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
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = event.offset,
            scrollDelta = Offset(
                x = event.deltaX.toFloat(),
                y = event.deltaY.toFloat()
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
    }

    private val MouseEvent.offset get() = Offset(
        x = offsetX.toFloat(),
        y = offsetY.toFloat()
    ) * density.density
}

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

    val canvas = document.getElementById(canvasElementId) as HTMLCanvasElement

    ComposeWindow(
        canvas = canvas,
        content = content,
        state = if (requestResize == null) DefaultWindowState(document.documentElement!!) else ComposeWindowState.createFromLambda(requestResize)
    )
}

/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Creates the composition in HTML canvas created in parent container identified by [viewportContainerId] id.
 * This size of canvas is adjusted with the size of the container
 */
@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainerId: String,
    content: @Composable () -> Unit = { }
) {
    ComposeViewport(document.getElementById(viewportContainerId)!!, content)
}

/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Creates the composition in HTML canvas created in parent container identified by [viewportContainer] Element.
 * This size of canvas is adjusted with the size of the container
 */
@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainer: Element,
    content: @Composable () -> Unit = { }
) {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.setAttribute("tabindex", "0")

    viewportContainer.appendChild(canvas)

    ComposeWindow(
        canvas = canvas,
        content = content,
        state = DefaultWindowState(viewportContainer)
    )
}
