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

import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.LayerType
import androidx.compose.ui.awt.AwtEventFilter
import androidx.compose.ui.awt.AwtEventListener
import androidx.compose.ui.awt.AwtEventListeners
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalInternalViewModelStoreOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import androidx.compose.ui.scene.skia.SwingSkiaLayerComponent
import androidx.compose.ui.scene.skia.WindowSkiaLayerComponent
import androidx.compose.ui.skiko.OverlayRenderDecorator
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import androidx.compose.ui.window.sizeInPx
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import java.awt.Component
import java.awt.Window
import java.awt.event.ComponentEvent
import java.awt.event.ComponentAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * Internal entry point to Compose.
 *
 * It binds Skia canvas and [ComposeScene] to [container].
 *
 * @property container A container for the [ComposeScene].
 * @property skiaLayerAnalytics The analytics for the Skia layer.
 * @property window The window ancestor of the [container].
 * @property windowContainer A container used for additional layers and as reference
 *  for window coordinate space.
 * @property useSwingGraphics Flag indicating if offscreen rendering to Swing graphics is used.
 * @property layerType The type of layer used for Popup/Dialog.
 * @property renderSettings The settings for rendering.
 */
internal class ComposeContainer(
    val container: JLayeredPane,
    private val skiaLayerAnalytics: SkiaLayerAnalytics,

    window: Window? = null,
    windowContainer: JLayeredPane = container,

    private val useSwingGraphics: Boolean = ComposeFeatureFlags.useSwingGraphics,
    private val layerType: LayerType = ComposeFeatureFlags.layerType,
    private val renderSettings: RenderSettings,
) : WindowFocusListener, WindowListener, LifecycleOwner, ViewModelStoreOwner {
    val windowContext = PlatformWindowContext()
    var window: Window? = null
        private set

    private var layoutDirection = layoutDirectionFor(window ?: container)

    /**
     * A list of additional layers. Layers are used to render [Popup]s and [Dialog]s.
     */
    private val layers = mutableListOf<DesktopComposeSceneLayer>()

    private var _windowContainer: JLayeredPane? = null
    var windowContainer: JLayeredPane
        get() = requireNotNull(_windowContainer)
        set(value) {
            if (_windowContainer == value) {
                return
            }
            if (layerType == LayerType.OnSameCanvas && value != container) {
                error("Customizing windowContainer cannot be used with LayerType.OnSameCanvas")
            }

            _windowContainer?.removeComponentListener(windowContainerComponentListener)
            value.addComponentListener(windowContainerComponentListener)

            _windowContainer = value

            windowContext.setWindowContainer(value)
            onWindowContainerSizeChanged()
            onWindowContainerPositionChanged()
        }

    private val coroutineExceptionHandler = DesktopCoroutineExceptionHandler()
    private val coroutineContext = MainUIDispatcher + coroutineExceptionHandler

    private val mediator = ComposeSceneMediator(
        container = container,
        windowContext = windowContext,
        exceptionHandler = {
            exceptionHandler?.onException(it) ?: throw it
        },
        eventListener = AwtEventListeners(
            DetectEventOutsideLayer(),
            FocusableLayerEventFilter()
        ),
        coroutineContext = coroutineContext,
        skiaLayerComponentFactory = ::createSkiaLayerComponent,
        composeSceneFactory = ::createComposeScene,
    )

    /**
     * The listener to [windowContainer] size and position changes.
     */
    private val windowContainerComponentListener = object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) = onWindowContainerSizeChanged()
        override fun componentMoved(e: ComponentEvent?) = onWindowContainerPositionChanged()
    }

    /**
     * The listener to [window] size changes.
     */
    private val windowSizeListener = object : ComponentAdapter() {
        // When the window is moved to a display with a different density, componentResized is
        // called on the window, but not on `windowContainer`, so we need to update the size
        // here too.
        override fun componentResized(e: ComponentEvent?) = onWindowContainerSizeChanged()
    }

    val contentComponent by mediator::contentComponent
    val focusManager by mediator::focusManager
    val accessible by mediator::accessible
    var rootForTestListener by mediator::rootForTestListener
    // TODO: Changing fullscreen probably will require recreate our layers
    //  It will require add this flag as remember parameters in rememberComposeSceneLayer
    var fullscreen by mediator::fullscreen
    var compositionLocalContext by mediator::compositionLocalContext
    var exceptionHandler: WindowExceptionHandler? = null
    val windowHandle by mediator::windowHandle
    val renderApi by mediator::renderApi
    val preferredSize by mediator::preferredSize

    override val lifecycle = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()

    private var isDisposed = false
    private var isDetached = true
    private var isMinimized = false
    private var isFocused = false

    init {
        setWindow(window)
        this.windowContainer = windowContainer

        if (layerType == LayerType.OnComponent && !useSwingGraphics) {
            error("LayerType.OnComponent can only be used with rendering via Swing graphics")
        }
    }

    fun dispose() {
        isDisposed = true
        updateLifecycleState()
        viewModelStore.clear()

        _windowContainer?.removeComponentListener(windowContainerComponentListener)
        mediator.dispose()
        layers.fastForEach(DesktopComposeSceneLayer::close)
    }

    override fun windowGainedFocus(event: WindowEvent) = onWindowFocusChanged()
    override fun windowLostFocus(event: WindowEvent) = onWindowFocusChanged()

    override fun windowOpened(e: WindowEvent) = Unit
    override fun windowClosing(e: WindowEvent) = Unit
    override fun windowClosed(e: WindowEvent) = Unit
    override fun windowIconified(e: WindowEvent) {
        isMinimized = true
        updateLifecycleState()
    }
    override fun windowDeiconified(e: WindowEvent) {
        isMinimized = false
        updateLifecycleState()
    }
    override fun windowActivated(e: WindowEvent) = Unit
    override fun windowDeactivated(e: WindowEvent) = Unit

    private fun onWindowFocusChanged() {
        isFocused = window?.isFocused ?: false
        windowContext.setWindowFocused(isFocused)
        mediator.onChangeWindowFocus()
        layers.fastForEach(DesktopComposeSceneLayer::onWindowFocusChanged)
        updateLifecycleState()
    }

    private fun onWindowContainerPositionChanged() {
        if (!container.isDisplayable) return

        mediator.onComponentPositionChanged()
        layers.fastForEach(DesktopComposeSceneLayer::onWindowContainerPositionChanged)
    }

    private fun onWindowContainerSizeChanged() {
        if (!container.isDisplayable) return

        windowContext.setContainerSize(windowContainer.sizeInPx)
        mediator.onComponentSizeChanged()
        layers.fastForEach(DesktopComposeSceneLayer::onWindowContainerSizeChanged)

        // Sometimes Swing displays interop views in incorrect order after resizing,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    /**
     * Callback to let layers draw overlay on main [mediator].
     */
    private fun onRenderOverlay(canvas: Canvas, width: Int, height: Int) {
        layers.fastForEach {
            it.onRenderOverlay(canvas, width, height, windowContext.isWindowTransparent)
        }
    }

    fun onWindowTransparencyChanged(value: Boolean) {
        windowContext.isWindowTransparent = value
        mediator.onWindowTransparencyChanged(value)
    }

    fun onLayoutDirectionChanged(component: Component) {
        // ComposeWindow and ComposeDialog relies on self orientation, not on container's one
        layoutDirection = layoutDirectionFor(component)
        mediator.onLayoutDirectionChanged(layoutDirection)
    }

    fun onRenderApiChanged(action: () -> Unit) {
        mediator.onRenderApiChanged(action)
    }

    fun addNotify() {
        mediator.onComponentAttached()
        setWindow(SwingUtilities.getWindowAncestor(container))

        // Re-checking the actual size if it wasn't available during init.
        onWindowContainerSizeChanged()
        onWindowContainerPositionChanged()

        isDetached = false
        updateLifecycleState()
    }

    fun removeNotify() {
        isDetached = true
        updateLifecycleState()

        setWindow(null)
    }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        mediator.contentComponent.setSize(width, height)

        // In case of preferred size there is no separate event for changing window size,
        // so re-checking the actual size on container resize too.
        onWindowContainerSizeChanged()
        onWindowContainerPositionChanged()
    }

    private fun setWindow(window: Window?) {
        if (this.window == window) {
            return
        }

        this.window?.let {
            it.removeWindowFocusListener(this)
            it.removeWindowListener(this)
            it.removeComponentListener(windowSizeListener)
        }
        window?.let {
            it.addWindowFocusListener(this)
            it.addWindowListener(this)
            it.addComponentListener(windowSizeListener)
        }
        this.window = window

        onWindowFocusChanged()
    }

    fun setKeyEventListeners(
        onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
        onKeyEvent: (KeyEvent) -> Boolean = { false },
    ) {
        mediator.setKeyEventListeners(onPreviewKeyEvent, onKeyEvent)
    }

    fun setContent(content: @Composable () -> Unit) {
        mediator.setContent {
            ProvideContainerCompositionLocals(this) {
                content()
            }
        }
    }

    private fun createSkiaLayerComponent(mediator: ComposeSceneMediator): SkiaLayerComponent {
        val renderDelegate = when (layerType) {
            // Use overlay decorator to allow window layers draw scrim on the main window
            LayerType.OnWindow -> OverlayRenderDecorator(mediator, ::onRenderOverlay)
            else -> mediator
        }
        return if (useSwingGraphics) {
            SwingSkiaLayerComponent(mediator, renderDelegate, skiaLayerAnalytics)
        } else {
            WindowSkiaLayerComponent(
                mediator,
                windowContext,
                renderDelegate,
                skiaLayerAnalytics,
                renderSettings
            )
        }
    }

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene {
        val density = container.density
        return when (layerType) {
            LayerType.OnSameCanvas ->
                CanvasLayersComposeScene(
                    density = density,
                    layoutDirection = layoutDirection,
                    coroutineContext = mediator.coroutineContext,
                    composeSceneContext = createComposeSceneContext(
                        platformContext = mediator.platformContext
                    ),
                    invalidate = mediator::onComposeInvalidation,
                )
            else -> PlatformLayersComposeScene(
                density = density,
                layoutDirection = layoutDirection,
                coroutineContext = mediator.coroutineContext,
                composeSceneContext = createComposeSceneContext(
                    platformContext = mediator.platformContext
                ),
                invalidate = mediator::onComposeInvalidation,
            )
        }
    }

    private fun createPlatformLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        compositionContext: CompositionContext
    ): ComposeSceneLayer {
        return when (layerType) {
            LayerType.OnWindow -> WindowComposeSceneLayer(
                composeContainer = this,
                skiaLayerAnalytics = skiaLayerAnalytics,
                transparent = true, // TODO: Consider allowing opaque window layers
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                compositionContext = compositionContext,
                renderSettings = renderSettings
            )
            LayerType.OnComponent -> SwingComposeSceneLayer(
                composeContainer = this,
                skiaLayerAnalytics = skiaLayerAnalytics,
                density = density,
                layoutDirection = layoutDirection,
                focusable = focusable,
                compositionContext = compositionContext
            )
            else -> error("Unexpected LayerType")
        }
    }

    /**
     * Generates a sequence of layers that are positioned above the given layer in the layers list.
     *
     * @param layer the layer to find layers above
     * @return a sequence of layers positioned above the given layer
     */
    fun layersAbove(layer: DesktopComposeSceneLayer) = sequence {
        var isAbove = false
        for (i in layers) {
            if (i == layer) {
                isAbove = true
            } else if (isAbove) {
                yield(i)
            }
        }
    }

    /**
     * Notify layers about change in layers list. Required for additional invalidation and
     * re-drawing if needed.
     *
     * @param layer the layer that triggered the change
     */
    private fun onLayersChange(layer: DesktopComposeSceneLayer) {
        layers.fastForEach {
            if (it != layer) {
                it.onLayersChange()
            }
        }
    }

    /**
     * Attaches a [DesktopComposeSceneLayer] to the list of layers.
     *
     * @param layer the layer to attach
     */
    fun attachLayer(layer: DesktopComposeSceneLayer) {
        layers.add(layer)
        onLayersChange(layer)
    }

    /**
     * Detaches a [DesktopComposeSceneLayer] from the list of layers.
     *
     * @param layer the layer to detach
     */
    fun detachLayer(layer: DesktopComposeSceneLayer) {
        layers.remove(layer)
        onLayersChange(layer)
    }

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext =
        ComposeSceneContextImpl(platformContext)

    private fun updateLifecycleState() {
        lifecycle.currentState = when {
            isDisposed -> State.DESTROYED
            isDetached || isMinimized -> State.CREATED
            !isDetached && !isMinimized && isFocused -> State.RESUMED
            else -> State.STARTED
        }
    }

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext {
        override fun createPlatformLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer = this@ComposeContainer.createPlatformLayer(
            density = density,
            layoutDirection = layoutDirection,
            focusable = focusable,
            compositionContext = compositionContext
        )
    }

    private inner class DesktopCoroutineExceptionHandler :
        AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            exceptionHandler?.onException(exception) ?: throw exception
        }
    }

    /**
     * Detect and trigger [DesktopComposeSceneLayer.onMouseEventOutside] if event happened below
     * focused layer.
     */
    private inner class DetectEventOutsideLayer : AwtEventListener {
        override fun onMouseEvent(event: AwtMouseEvent): Boolean {
            layers.fastForEachReversed {
                it.onMouseEventOutside(event)
                if (it.focusable) {
                    return false
                }
            }
            return false
        }
    }

    private inner class FocusableLayerEventFilter : AwtEventFilter() {
        private val noFocusableLayers get() = layers.all { !it.focusable }

        override fun shouldSendMouseEvent(event: AwtMouseEvent): Boolean = noFocusableLayers
        override fun shouldSendKeyEvent(event: AwtKeyEvent): Boolean = noFocusableLayers
    }
}

@Composable
private fun ProvideContainerCompositionLocals(
    composeContainer: ComposeContainer,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(
    LocalLifecycleOwner provides composeContainer,
    LocalInternalViewModelStoreOwner provides composeContainer,
    content = content
)
